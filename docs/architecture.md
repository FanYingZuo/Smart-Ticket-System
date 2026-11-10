# 架構決策文件 — 智慧 IT 服務工單系統

> 狀態：草案（規劃階段，尚未開始實作）
> 最後更新：2026-11-03
> 定位：作為可交付的作品集專案，架構與邏輯需經得起別人檢視與提問

## 1. 系統情境

企業內部 IT 支援平台。員工開單描述問題，系統自動判斷分類、優先級與應派送的小組；工程師處理工單直到結案；主管透過儀表板檢視 SLA 達成率與工程師負載。AI 參與兩個環節：工單的結構化分類與解法建議（Spring AI），以及分派決策（Jev）。

## 2. 服務拆分

| 服務 | 職責 | 資料邊界 |
|---|---|---|
| ticket-service | 工單生命週期、狀態機、SLA 計時、停表歷程 | 工單主表、狀態轉移歷程表、停表歷程表 |
| kb-service | 知識庫文章與向量索引 | KB 文章表、Chroma 向量庫 |
| ai-assist-service | 工單自動分類（Structured Output）、RAG 解法建議、Function Calling、Jev 分派決策 | 無獨立資料表，呼叫 ticket-service / kb-service API |
| dashboard-service | CQRS 查詢端：SLA 達成率、工程師負載 | 自己的讀取模型（由事件同步） |
| notification-service | 異步通知 | 無獨立資料表（或通知歷程表） |

**拆分理由：** 每個服務對應一個獨立的業務能力與資料邊界，服務間不共用 schema、不直接讀對方資料庫。ai-assist-service 獨立於 ticket-service，因為外部 AI 呼叫（延遲、失敗、重試）是不同的關注點，不該耦合進工單的交易邏輯——工單的狀態轉移必須能在 AI 服務整個掛掉的情況下依然正常運作。

## 3. 核心業務規則

### 3.1 優先級與 SLA 時限

| 優先級 | 回應時限 | 解決時限 | 典型情境 |
|---|---|---|---|
| P1 緊急 | 15 分鐘 | 4 小時 | 全公司無法登入 |
| P2 高 | 1 小時 | 1 個工作天 | 單一部門系統異常 |
| P3 一般 | 1 個工作天 | 5 個工作天 | 軟體安裝申請 |

### 3.2 狀態機（5 個狀態）

| 狀態 | SLA 計時 | 誰能轉出 | 允許的下一步 |
|---|---|---|---|
| Open | 回應與解決計時都在跑 | 系統（自動指派） | Assigned |
| Assigned | 解決計時跑，回應計時停止 | 負責工程師 | Pending、Resolved |
| Pending | 停表，不算在 SLA 內 | 負責工程師 | Assigned、Resolved |
| Resolved | 停止計時，達成與否定案 | 負責工程師 | Closed、Assigned（7 天內重開） |
| Closed | 不再變更 | 使用者或系統自動（7 天） | 無（只能開新單） |

非法轉移（如 Closed → Assigned、Open → Resolved）必須擋下，丟出自訂 Business Exception 並回傳明確錯誤碼；每次轉移寫入歷程表。

### 3.3 SLA 計時規則

- 兩個獨立計時器：回應時限（Open → Assigned）與解決時限（Open → Resolved），各自在建單時寫入 due time。
- 進出 Pending 時停表，累計 `pausedMinutes`；每次進出寫一筆歷程（進入時間、離開時間、原因）。
- 是否 breached：以「實際耗時 − 停表時間」判定。
- 解決時限以工作時間計算（週一至五 09:00–18:00），至少要跳過週末。

### 3.4 分派方式：系統自動指派

工單建立後，不經人工指派，由系統自動完成分派：

1. AI（Spring AI Structured Output）判斷工單的 `category`、`priority`、`suggestedTeam`。
2. Jev 針對分派這一層做決策：在 `suggestedTeam` 之下，判斷是否有特殊情況需要改派（如疑似重複工單、需要主管立即介入）。
3. 系統在最終決定的組別內，依工程師目前未結案工單數挑人指派。
4. 轉派須留歷程，且不得重算 SLA。

這個設計把「分類」與「分派決策」拆成兩層，分類交給 Spring AI（課程要求的 Structured Output），分派決策交給 Jev（選擇題型任務），兩者不重疊。

## 4. 微服務模式與理由

| 模式 | 用在哪裡 | 為何選用 |
|---|---|---|
| CQRS 讀寫分離 | dashboard-service | SLA 統計查詢不應直接 join 工單表；寫入端（開單、轉派）與查詢端（統計）效能需求不同，查詢端可接受最終一致 |
| RabbitMQ 異步 | notification-service、dashboard-service 的事件同步 | 通知與統計更新不應阻塞工單狀態轉移的主交易 |
| 部署 | Docker Compose | multi-stage Dockerfile，`docker compose up` 一鍵啟動完整環境 |

## 5. AI 整合設計

### 5.1 Spring AI

- **Structured Output**：`TicketTriage` 物件（category、priority、suggestedTeam、estimatedHours），用 `BeanOutputConverter` + `@Description`。
- **PromptTemplate**：工單描述轉換為分類 prompt。
- **RAG**：相似歷史工單 + KB 文章混合檢索，回答附來源文章 ID，向量持久化於 Chroma。
- **Function Calling**：對話式操作，例如「把 #1234 轉給網路組」→ 呼叫 `assignTicket`，需參數驗證與可呼叫範圍白名單。

### 5.2 Jev — 分派決策層

- **定位**：工單系統的決策節點，負責「選哪一組」「是否需要主管介入」「是否疑似重複」這類有限選項的判斷，不生成文字。
- **呼叫方式**：`POST https://api.typesafe.ai/v1/systemone`，Header `Authorization: Bearer <API_KEY>`、`Content-Type: application/json`。
- **Request 結構（概念）：**
  ```json
  {
    "model": "jev-latest",
    "state": { "ticket": "工單描述文字", "customerTier": "...", "accountAgeDays": 400 },
    "questions": {
      "team": {
        "type": "choice",
        "instructions": "這張工單該分派給哪一組",
        "criteria": { "network": "網路相關問題", "account": "帳號權限問題", "hardware": "硬體設備問題" }
      },
      "needs_escalation": {
        "type": "noul",
        "instructions": "是否需要主管立即介入"
      }
    }
  }
  ```
- **Response 結構（概念）：**
  ```json
  {
    "answers": {
      "team": { "type": "choice", "choice": "network", "confidence": 0.87 },
      "needs_escalation": { "type": "noul", "noul": 0.12 }
    },
    "usage": { "input_tokens": 180 }
  }
  ```
- **降級路徑**：Jev 逾時或回傳錯誤時，退回規則式分派（依 Spring AI 判斷的 category 直接對應固定組別），並記錄一筆「降級事件」供後續檢討使用率與準確度。
- **成本考量**：輸入計費（約 $0.042 / 百萬 token），輸出免費；單張工單的呼叫成本可忽略不計，適合高頻呼叫。

## 6. 邏輯盲點與容易出錯的地方

這一節記錄設計時已經意識到、但容易在實作中漏掉或做錯的地方，供日後實作與代碼審查時對照。

### 6.1 SLA 計時

- **停表期間的 due time 不會跟著平移**：若只在工單上疊加 `pausedMinutes`，沒有獨立的停表歷程表，會無法回答「這張單到底停了幾次、各停多久」，稽核時說不清楚。必須有獨立的 Pending 歷程表，每次進出各一筆記錄。
- **工作日曆的邊界情況**：週五下班前（例如 17:59）開的 P2 工單，解決時限是「1 個工作天」，必須從下一個工作時段起算，不能簡單地加 24 小時，否則會把週末算進去。
- **重開工單（Resolved → Assigned，7 天內）是否重算 SLA**：目前規則只明確說「轉派不重算 SLA」，但沒有明確說「重開」算不算一種特殊的轉派。這是容易被老師或面試官追問的灰色地帶，需要在實作前先決定好規則並寫進文件，而不是寫到一半才發現沒想清楚。

### 6.2 狀態機

- **併發轉移**：兩個人（或系統事件）同時嘗試轉移同一張工單的狀態，若沒有樂觀鎖或版本控制，可能產生不一致的歷程紀錄。Entity 應該加 `@Version` 欄位。
- **非法轉移的判斷要在 Service 層做，不能只靠前端擋**：API 本身必須驗證，否則直接呼叫 API 就能繞過。

### 6.3 AI 與 Jev 整合

- **Jev 不驗證語意正確性，只給機率**：`confidence` 高不代表判斷一定對，低信心的情況需要有明確的 fallback 門檻（例如 confidence < 0.6 時走人工複核佇列），不能無條件相信回傳結果。
- **Jev 回傳的 choice 若不在原本定義的選項集合裡（理論上不會發生，但程式仍要防禦）**：解析 response 時要做型別與範圍檢查，不能直接 cast 使用。
- **Structured Output 與 Jev 的分工若沒有清楚切開，容易重工**：如果 Spring AI 已經判斷了 `suggestedTeam`，Jev 又重新判斷一次「該分給哪組」，邏輯上是重複的。正確做法是 Jev 只處理「是否要override」這類例外判斷，不是重做分類。
- **AI 服務（Spring AI 或 Jev）逾時或掛掉時，工單建立流程不能被卡住**：分類與分派邏輯應該用非同步或加上明確的 timeout + fallback，不能讓 AI 呼叫成為整個開單流程的單點故障。

### 6.4 CQRS 與異步

- **事件順序問題**：如果 dashboard-service 用訊息佇列同步資料，必須處理「訊息晚到」或「亂序到達」的情況，否則統計數字可能短暫不準。這點在 Demo 或面試時很適合主動提出來討論。
- **消費失敗的訊息要去哪裡**：沒有死信佇列（DLQ）或重試機制的話，失敗訊息會憑空消失，這是最常被抽問「資料不一致時會發生什麼」的地方。

## 7. 待確認 / 待驗證事項

- [ ] Jev 的 API 金鑰取得與實際可用性，需要實測確認
- [ ] 在工單資料上的分派準確率與延遲，需自行量測記錄
- [ ] RabbitMQ 消費失敗處理策略（死信佇列 / 重試上限）尚未決定
- [ ] 工作日曆計算（跳過週末）的實作方式尚未決定（手刻邏輯 or 套件）
- [ ] 重開工單是否重算 SLA 的規則尚未定案（見 6.1）
- [ ] 停表分鐘（`paused_minutes`）以日曆分鐘或工作時間分鐘換算尚未決定；解決時限採工作時間（見 3.3），兩者基準需一致

## 8. ER 關聯圖

### 8.1 ticket-service（ticket_db）

```
tickets
├── id              BIGINT PK AUTO_INCREMENT
├── title           VARCHAR(255) NOT NULL
├── description     TEXT
├── category        VARCHAR(255)
├── priority        ENUM('P1','P2','P3') NOT NULL
├── status          ENUM('OPEN','ASSIGNED','PENDING','RESOLVED','CLOSED') NOT NULL
├── suggested_team  VARCHAR(255)          -- AI 建議的組別
├── assigned_team   VARCHAR(255)          -- 實際指派的組別
├── assignee_id     BIGINT NULL           -- 負責工程師 ID
├── response_due_at DATETIME              -- 回應時限到期時間
├── resolution_due_at DATETIME            -- 解決時限到期時間
├── responded_at    DATETIME NULL         -- 實際回應時間（首次進入 Assigned）；NULL 表示尚未回應
├── resolved_at     DATETIME NULL         -- 實際解決時間（進入 Resolved）；NULL 表示尚未解決
├── paused_minutes  BIGINT NOT NULL DEFAULT 0 -- 累計停表分鐘數（來源見「SLA 欄位說明」）
├── response_breached  BOOLEAN            -- 回應是否逾期
├── resolution_breached BOOLEAN           -- 解決是否逾期
├── created_at      DATETIME NOT NULL
├── updated_at      DATETIME
└── version         BIGINT                -- @Version 樂觀鎖

status_transitions                        -- 狀態轉移歷程
├── id              BIGINT PK AUTO_INCREMENT
├── ticket_id       BIGINT FK → tickets.id
├── from_status     ENUM NOT NULL
├── to_status       ENUM NOT NULL
├── changed_by      VARCHAR(255) NOT NULL  -- 操作者（工程師帳號或 SYSTEM）
├── changed_at      DATETIME NOT NULL
└── reason          VARCHAR(255)

pause_histories                           -- 停表歷程
├── id              BIGINT PK AUTO_INCREMENT
├── ticket_id       BIGINT FK → tickets.id
├── entered_at      DATETIME NOT NULL     -- 進入 Pending 的時間
├── exited_at       DATETIME NULL         -- NULL 表示目前仍在 Pending
└── reason          VARCHAR(255)
```

**關聯：**
- `tickets` 1 ←→ N `status_transitions`（一張工單有多筆狀態轉移紀錄）
- `tickets` 1 ←→ N `pause_histories`（一張工單可多次進出 Pending）

**SLA 欄位說明：**
- `paused_minutes` 是由 `pause_histories` 衍生的累計值，不是獨立輸入。每當一筆暫停紀錄寫入 `exited_at`（離開 Pending）時，在同一個交易中累加該段的 `exited_at − entered_at`（分鐘）。仍在 Pending（`exited_at` 為 NULL）的那一段尚未計入，因此 Pending 期間 `paused_minutes` 不變。
- `pause_histories` 是事實來源；`paused_minutes` 與已結束的暫停紀錄總和應一致，兩者不一致視為資料錯誤。
- `responded_at`：工單首次由 Open 進入 Assigned 時寫入，之後不再更新（轉派不改變、不重算）。
- `resolved_at`：進入 Resolved 時寫入。重開（Resolved → Assigned）時此欄位如何處理，待 6.1 重開規則定案後決定。
- 逾期判定（對應 3.3）：`response_breached` 為 `responded_at` 晚於 `response_due_at`；`resolution_breached` 為 `resolved_at` 扣除 `paused_minutes` 後晚於 `resolution_due_at`。

### 8.2 kb-service（kb_db）

```
articles
├── id              BIGINT PK AUTO_INCREMENT
├── title           VARCHAR(255) NOT NULL
├── content         TEXT NOT NULL
├── category        VARCHAR(255)
├── created_at      DATETIME NOT NULL
└── updated_at      DATETIME
```

## 9. API 端點草案

### 9.1 ticket-service（port 8081）

| Method | Path | 說明 |
|---|---|---|
| GET | `/actuator/health` | 健康檢查 |
| POST | `/api/tickets` | 建立工單（觸發 AI 分類 + 自動分派） |
| GET | `/api/tickets` | 查詢工單清單（分頁、排序、篩選） |
| GET | `/api/tickets/{id}` | 查詢單一工單詳情 |
| PUT | `/api/tickets/{id}/transition` | 狀態轉移（body 帶 toStatus、reason） |
| GET | `/api/tickets/{id}/transitions` | 查詢該工單的狀態轉移歷程 |
| GET | `/api/tickets/{id}/pauses` | 查詢該工單的停表歷程 |

### 9.2 kb-service（port 8082）

| Method | Path | 說明 |
|---|---|---|
| GET | `/actuator/health` | 健康檢查 |
| POST | `/api/articles` | 新增知識庫文章 |
| GET | `/api/articles` | 查詢文章清單（分頁、分類篩選） |
| GET | `/api/articles/{id}` | 查詢單一文章 |
| PUT | `/api/articles/{id}` | 更新文章 |
| DELETE | `/api/articles/{id}` | 刪除文章 |

## 10. 修訂紀錄

| 日期 | 內容 |
|---|---|
| 2026-11-03 | 建立草案：服務拆分、狀態機、SLA 規則、分派方式（系統自動指派 + Jev）、部署選 Docker Compose、新增邏輯盲點章節 |
| 2026-11-11 | v1 定稿：新增 ER 關聯圖（ticket_db 三表、kb_db 一表）、API 端點草案、Monorepo 骨架建置完成 |
| 2026-11-11 | ER 補充：`tickets` 新增 `responded_at`、`resolved_at`；`paused_minutes` 改為 NOT NULL 並寫明來源（由 `pause_histories` 在離開 Pending 時累加）；新增「SLA 欄位說明」；列出停表分鐘計算基準為未決事項 |
