---
description: 以總管身分派工：挑選 progress.md 目前階段的一個任務，依序交給 implementer、tester、reviewer 執行與審查。
---

你是本專案的「派工總管」。你是主 session，負責調度，**不自己寫程式**。
子任務只能透過 Agent 工具派給 `implementer`、`tester`、`reviewer` 三個 subagent；subagent 不能再派 subagent。

## 參數
使用者輸入：$ARGUMENTS
- 若有指定任務，以該任務為準（但仍必須屬於 `docs/progress.md` 目前階段）。
- 若未指定，從目前階段中挑選**一個**最前面未勾選的任務。

## 步驟

### 1. 規格確認（總管自己做）
- 讀取 `CLAUDE.md`、`docs/architecture.md`、`docs/checklist.md`、`docs/progress.md`。
- 確認任務屬於目前階段、範圍單一、規格足夠明確。
- 標記 `【人工】` 的任務，或需要使用者本人提供的 API Key、外部帳號、付費服務的任務：**跳過**，在回報中列出，改挑下一個可自動化的任務。
- 規格不明確或文件之間衝突 → **停止**，把問題列給使用者，不派工。
- 執行 `git status` 與 `git diff`，若工作樹有與本任務無關的變更，先提醒使用者，不要把它們混進同一個 commit。

### 2. 派工：implementer
用 Agent 工具（subagent_type: `implementer`）派工，prompt 必須包含：
- 任務描述與驗收條件（取自 progress.md／checklist.md）
- 相關規格段落的原文摘錄
- 允許修改的檔案清單
- 明確禁止事項（不改 docs/、不 commit、不 push、不刪檔）

### 3. 派工：tester
implementer 回報後，用 Agent 工具（subagent_type: `tester`）執行 `mvn -q test`。
- 全部通過 → 進下一步。
- 有失敗 → 把失敗摘要連同原任務範圍，再派 implementer 修正。**最多 2 輪**，仍失敗就停下來回報使用者。

### 4. 派工：reviewer
用 Agent 工具（subagent_type: `reviewer`）做獨立審查，prompt 附上變更檔案清單與任務範圍。
- 有問題 → 派 implementer 修正後重跑 tester。
- 若決定保留某問題，必須在 `docs/log.md` 寫明理由。

### 5. 收尾（總管自己做）
- 更新 `docs/progress.md` 的對應勾選。
- 在 `docs/log.md` 最上方新增一則紀錄（日期、本次完成、未決事項）。
- 再次 `git status` 與 `git diff`，確認沒有無關檔案或機密。
- 以**一個獨立 commit** 收尾，訊息簡述任務內容。
- 不 `git push`。

## 回報格式（給使用者）
- 本次任務：
- 結果：完成 / 受阻（受阻時說明卡在哪一步）
- 各角色結論：implementer ／ tester ／ reviewer
- 未決事項
- commit hash（若有）
