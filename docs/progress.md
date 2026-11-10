# 進度表 — 智慧 IT 服務工單系統

> 最後更新：2026-11-11
> 標記 `【人工】` 的項目需要使用者本人操作（帳號、金鑰、外部服務），`/dispatch` 會跳過。
> 目標日期（自訂，不依賴任何外部審查節點）：骨架完成 11/15、主功能與 AI 整合完成 12/10、最終交付 12/20

## 總覽

| 階段 | 時間 | 重點 |
|---|---|---|
| 準備 | 11/3–11/8 | repo 建立、Docker Compose 骨架、Jev 實驗 |
| 骨架 | 11/9–11/15 | 2 個服務健康檢查、ER 與核心 Entity、架構文件定稿 |
| 主流程（第 1 週） | 11/16–11/22 | 工單主流程、五狀態機、兩個計時器、停表歷程 |
| 主流程（第 2 週） | 11/23–11/29 | Keycloak、第三方登入、RabbitMQ 通知、自動分派邏輯 |
| AI 整合 | 11/30–12/6 | Spring AI 四項、Chroma RAG、Jev 分派決策、Function Calling |
| 收尾（第一階段） | 12/7–12/10 | dashboard-service CQRS、Swagger、測試覆蓋、Demo 演練 |
| 最終交付 | 12/11–12/20 | 測試補強、文件定稿、RAG 對照報告、README、Demo 演練 |

## 待辦清單（目前階段：準備）

- [ ] 【人工】註冊 TypeSafe AI 取得 API Key（或先用匿名存取測試）
- [ ] 【人工】用 5–10 筆模擬工單測試 Jev 的分類準確率與延遲，記錄結果
- [x] 建立 GitHub repo（monorepo 結構）
- [ ] Docker Compose 骨架：MySQL、RabbitMQ、Chroma、Keycloak 能一鍵啟動

## 骨架階段（11/9–11/15）待辦

- [ ] 架構決策文件定稿（草案見 `docs/architecture.md`）
- [ ] ticket-service、kb-service 可啟動並回應健康檢查端點
- [ ] docker-compose.yml 能一鍵起服務與 DB，資料以 Volume 持久化
- [ ] 核心 Entity 與 Repository，含至少一組關聯映射，有可用測試資料

## 主流程階段（11/16–12/10）待辦

### 第 1 週（11/16–11/22）：工單主流程
- [ ] 五狀態機（Open → Assigned → Pending → Resolved → Closed）
- [ ] 非法轉移擋下，回傳明確錯誤碼
- [ ] 兩個計時器（回應 / 解決）分開計算
- [ ] Pending 停表與進出歷程
- [ ] 狀態轉移加樂觀鎖（`@Version`）

### 第 2 週（11/23–11/29）：安全與異步
- [ ] Keycloak 整合：登入發 Token、API 驗證
- [ ] 至少兩種角色權限區隔
- [ ] 第三方登入（GitHub 或 Google）
- [ ] RabbitMQ 異步通知流程，含消費失敗處理策略（死信佇列）
- [ ] 系統自動分派邏輯（規則式備援先行）

### AI 整合（11/30–12/6）
- [ ] Spring AI：Chat、PromptTemplate、Structured Output（TicketTriage）、Function Calling
- [ ] Chroma RAG：文件入庫、檢索問答、來源引用
- [ ] Jev 分派決策整合，含降級路徑與型別檢查

### 收尾（12/7–12/10）
- [ ] dashboard-service CQRS 查詢端（SLA 達成率、工程師負載）
- [ ] Swagger UI 可瀏覽主要端點
- [ ] 測試覆蓋率達標
- [ ] 主流程 Demo 演練（含一次失敗情境）

## 最終交付（12/11–12/20）待辦

- [ ] 測試覆蓋率補強
- [ ] 架構決策文件最終版（心得與踩坑記錄）
- [ ] RAG 效果比較報告：至少 5 組問答有/無 RAG 對照
- [ ] README：架構圖、啟動步驟、環境變數說明、API 清單
- [ ] Docker Compose 一鍵啟動驗收
- [ ] Demo 與口頭介紹演練

## 風險提醒

- 主流程 + AI 整合是工作量最重的階段，要在 12/10 前完成
- 最後 10 天只留給補文件與修 bug，不要把功能拖到這裡做
- Jev 的 API 可用性與準確率需要儘早驗證，避免卡在 AI 整合階段才發現問題
