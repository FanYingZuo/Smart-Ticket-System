# 智慧 IT 服務工單系統 — 工作規範

## 規格來源與優先順序
1. `docs/architecture.md`（最高）
2. `docs/checklist.md`
3. `docs/progress.md`（規劃排程與待辦，低於前兩者）
- 規格衝突時停下來問，不自行選擇。
- `docs/log.md` 是討論紀錄，不是規格來源。

## 任務範圍
- 一次只做 `docs/progress.md` 中「目前階段」的一個任務，範圍外不做。
- 不得新增 `docs/architecture.md` 未提到的服務、資料表或外部依賴。
- 規格不明確時，停下來問，不得自行假設。

## 文件規則
- 不得修改 `docs/` 下的文件，除非使用者明確要求。
- 每完成一個任務：
  1. 更新 `docs/progress.md` 的勾選狀態。
  2. 在 `docs/log.md` 最上方新增一則紀錄（日期、本次完成、未決事項）。

## 安全
- 密碼、API Key、資料庫帳密一律用環境變數（如 `${MYSQL_ROOT_PASSWORD}`），不得寫進程式碼、設定檔或 commit。
- `.env` 已在 `.gitignore`；範例檔用 `.env.example`，只放變數名稱與假值。
- 不得在回應中輸出真實的密碼或金鑰內容。

## 驗證
完成任何任務前，必須通過：
```
mvn -q test
```
健康檢查指令：TODO（待骨架建立後補上 `curl` 指令與預期結果）。

## 禁止事項
- `git push`
- `docker compose down -v`
- 刪除 Docker Volume
- `rm`、`Remove-Item`、`del`（刪檔前須先說明並取得同意）

## Git
- 每個任務完成後，以一個獨立 commit 收尾，訊息簡述任務內容。
- 不得 force push、改寫已推送的歷史，除非使用者明確同意。
- commit 前先看 `git status` 與 `git diff`，確認沒有混入無關檔案或機密。

## 審查
- 任務完成後，交給 `reviewer` subagent 做獨立審查，檢查是否違反本文件與 `docs/` 規格。
- 審查發現的問題須修正或在 `docs/log.md` 說明為何保留。

## 語言
- 與使用者溝通與文件撰寫以繁體中文為主；程式碼識別字維持英文。
