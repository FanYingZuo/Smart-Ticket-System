# 討論紀錄 — 智慧 IT 服務工單系統

> 每次聊天的摘要，用於追蹤決策過程與邏輯演變。新的一則加在最上面。

---

## 2026-11-11（三）討論記錄 #4

**本次完成（commit `4918b61`）：**
- `README.md`：快速啟動補上 `cp .env.example .env` 與填寫 `MYSQL_ROOT_PASSWORD`、`DB_APP_PASSWORD` 的步驟；修正專案結構段落中 `architecture.md` 的位置（實際在 `docs/`）
- 新增 `.dockerignore`（排除 `.git`、`.env`、`target/`、`docs/`、`.claude/`、文件與 PDF）
- `init-db/01-create-databases.sh` 在 git 中改為可執行（100755）
- 驗證：`mvn -q test` 通過；`docker compose up -d --build` 成功；`curl http://localhost:8081/actuator/health`（ticket-service）與 `curl http://localhost:8082/actuator/health`（kb-service）皆回 `{"status":"UP"}`
- 持久化實測：插入標記資料（`tickets.id = 9999`）後執行 `docker compose restart`，重啟後標記仍在、筆數不變，確認資料由 `mysql-data` Volume 保存（非 `data.sql` 重灌）；測完已刪除標記，兩個服務重啟後 health 皆 `UP`
- 過程處理：本機 3306 被非本專案的容器 `verify-clone-mysql-1` 佔用，經使用者同意後已停止（未刪除）；先前失敗的 `up` 留下網路不完整的舊容器（服務連不到 `mysql`），以 `docker compose up -d --force-recreate` 重建，未動 Volume
- `reviewer` 審查：無阻擋性問題；README 專案結構錯誤已修正

**例外說明：** 本次未以 `/dispatch` 啟動，由主 session 直接修改，`reviewer` 為事後補派。往後任務一律走 `/dispatch`。

**未決事項：**
- ER（`architecture.md` 第 8 節）尚缺 SLA 時間戳，且 `paused_minutes` 的來源未寫明；建議規則：每段暫停的 `exited_at` 寫入時，累加 `exited_at − entered_at`（分鐘），待使用者確認
- seed 工單 #2 資料不一致：狀態 PENDING 且 `paused_minutes = 30`，但對應 `pause_histories` 未結束（`exited_at = NULL`）；待上一項決定後修正
- 尚無 repository 測試驗證工單與轉移歷程的關聯（屬骨架階段，待使用者決定是否現在做）
- 根目錄重複文件已於 #3 移除，無需再處理
- `progress.md` 未勾選：「Docker Compose 骨架」寫 MySQL、RabbitMQ、Chroma、Keycloak，但骨架階段只要求服務與 DB，兩者範圍不一致，待使用者決定
- `.claude/settings.json` 的修改（#3 遺留）仍未 commit

---

## 2026-11-11（三）討論記錄 #3

**本次完成：**
- 建立 AI 派工模式：新增 `/dispatch` 指令（`.claude/commands/dispatch.md`）與 `implementer` agent，搭配既有 `tester`、`reviewer`
- 移除根目錄重複的 `architecture.md`、`checklist.md`、`progress.md`、`log.md`（與 `docs/` 內容相同），以 `docs/` 為唯一版本
- `progress.md`：勾選「建立 GitHub repo」，將 API Key 註冊與 Jev 測試標為【人工】
- `CLAUDE.md`：補上健康檢查指令，並註明 `/dispatch` 收尾更新 progress／log 視為已授權
- 新增 `派工模式使用說明.md`

**未決事項：**
- `.claude/settings.json` 的權限調整被自動模式安全檢查擋下，需使用者手動套用
- 本次為工具設定，未經 reviewer subagent 審查（agent 需重開 session 才會載入）

---

## 2026-11-03（三）討論記錄 #2

**定位調整：** 這份專案不再以「課程作業、需要老師審核」的角度處理，改以「可交付的實際專案 / 作品集」角度規劃。架構與邏輯要經得起之後跟別人介紹與被追問，不依賴外部審查節點。Jev 是確定要用的技術選型，不是待確認的加分項目。

**本次完成：**
- 重寫架構決策文件，移除「需跟老師確認」的措辭，改為直接決策並記錄理由
- 新增「邏輯盲點與容易出錯的地方」章節，涵蓋：
  - SLA 計時：停表歷程表設計、工作日曆邊界情況、重開工單的 SLA 規則（目前仍未定案）
  - 狀態機：併發轉移需要樂觀鎖、非法轉移需在 Service 層驗證
  - AI 整合：Jev confidence 不等於正確、回傳結果需型別檢查、Structured Output 與 Jev 的分工若沒切開會重工、AI 逾時不能卡住主流程
  - CQRS / 異步：事件亂序到達、消費失敗訊息的去向（死信佇列）
- 補上 Jev API 的具體 request/response JSON 結構草稿，供之後實作參考
- 更新檢查清單與進度表，拿掉教師審核相關項目

**目前未定案、需要在實作前決定的事：**
- 重開工單（Resolved → Assigned，7 天內）是否重算 SLA
- RabbitMQ 消費失敗的具體處理策略（死信佇列 vs 重試上限）
- 工作日曆計算要手刻邏輯還是用現成套件

**下一步：** 架構文件可以交給實作階段（例如 Claude Code）直接讀取開始建置骨架。

---

## 2026-11-03（三）討論記錄 #1

**討論內容：**
- 確認選題：智慧 IT 服務工單系統
- 部署方式決策：Docker Compose
- 分派方式決策：系統自動指派
- 說明主管儀表板的用途：SLA 達成率、工程師負載、逾期清單，採 CQRS 查詢端設計
- 研究 Jev（TypeSafe AI 的 System One 決策模型）：
  - 三種題型：是非題（noul）、單選題（choice，最多 255 選項）、打分題（score，最多 11 級）
  - API：`POST https://api.typesafe.ai/v1/systemone`，Bearer token 認證，不帶 token 可匿名存取
  - 有 Spring AI Community 整合專案（`typesafe-spring-ai`），也有純 Java HTTP Client 的實作文章
  - 計畫用途：工單分派決策，與 Spring AI 的 Structured Output（TicketTriage 分類）分工
- 建立四份文件：架構決策文件、驗收檢查清單、進度表、本討論紀錄檔

---
