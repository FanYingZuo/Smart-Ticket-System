INSERT IGNORE INTO tickets (id, title, description, category, priority, status, suggested_team, assigned_team, assignee_id, response_due_at, resolution_due_at, responded_at, resolved_at, paused_minutes, response_breached, resolution_breached, created_at, updated_at, version)
VALUES
(1, '全公司無法登入 VPN', '今早 9:00 起全公司員工無法連線 VPN，影響遠端辦公。已確認非網路設備問題，疑似 VPN Server 憑證過期。', 'network', 'P1', 'ASSIGNED', 'network', 'network', 1, '2026-11-10 09:15:00', '2026-11-10 13:00:00', '2026-11-10 09:05:00', NULL, 0, false, false, '2026-11-10 09:00:00', '2026-11-10 09:05:00', 0),
(2, '會計系統報表匯出失敗', '會計部門反映月報匯出功能出現 500 錯誤，無法產出十月份財務報表。', 'software', 'P2', 'PENDING', 'software', 'software', 2, '2026-11-10 11:00:00', '2026-11-11 18:00:00', '2026-11-10 10:05:00', NULL, 0, false, false, '2026-11-10 10:00:00', '2026-11-10 14:00:00', 0),
(3, '新進員工筆電安裝申請', '新進員工需安裝 Office 365、開發工具與 VPN 客戶端。', 'hardware', 'P3', 'OPEN', 'hardware', NULL, NULL, '2026-11-11 18:00:00', '2026-11-17 18:00:00', NULL, NULL, 0, false, false, '2026-11-10 14:00:00', '2026-11-10 14:00:00', 0),
(4, '印表機驅動程式異常', 'B 棟 3F 多功能印表機無法列印，所有使用者皆受影響。', 'hardware', 'P3', 'RESOLVED', 'hardware', 'hardware', 3, '2026-11-09 18:00:00', '2026-11-13 18:00:00', '2026-11-08 10:05:00', '2026-11-09 11:00:00', 0, false, false, '2026-11-08 10:00:00', '2026-11-09 11:00:00', 0),
(5, 'Email 無法收信', '行銷部全體約 20 人無法收到外部郵件，寄信正常。', 'network', 'P2', 'CLOSED', 'network', 'network', 1, '2026-11-08 11:00:00', '2026-11-09 18:00:00', '2026-11-08 10:03:00', '2026-11-09 14:00:00', 0, false, false, '2026-11-08 10:00:00', '2026-11-09 15:00:00', 0);

INSERT IGNORE INTO status_transitions (id, ticket_id, from_status, to_status, changed_by, changed_at, reason)
VALUES
(1, 1, 'OPEN', 'ASSIGNED', 'SYSTEM', '2026-11-10 09:05:00', '系統自動指派至網路組'),
(2, 2, 'OPEN', 'ASSIGNED', 'SYSTEM', '2026-11-10 10:05:00', '系統自動指派至軟體組'),
(3, 2, 'ASSIGNED', 'PENDING', 'engineer_wang', '2026-11-10 14:00:00', '等待客戶提供錯誤截圖與操作步驟'),
(4, 4, 'OPEN', 'ASSIGNED', 'SYSTEM', '2026-11-08 10:05:00', '系統自動指派至硬體組'),
(5, 4, 'ASSIGNED', 'RESOLVED', 'engineer_lee', '2026-11-09 11:00:00', '已重新安裝驅動程式並測試列印正常'),
(6, 5, 'OPEN', 'ASSIGNED', 'SYSTEM', '2026-11-08 10:03:00', '系統自動指派至網路組'),
(7, 5, 'ASSIGNED', 'RESOLVED', 'engineer_chen', '2026-11-09 14:00:00', '已修復 MX Record 設定，郵件恢復收信'),
(8, 5, 'RESOLVED', 'CLOSED', 'user_marketing', '2026-11-09 15:00:00', '確認已恢復正常');

INSERT IGNORE INTO pause_histories (id, ticket_id, entered_at, exited_at, reason)
VALUES
(1, 2, '2026-11-10 14:00:00', NULL, '等待客戶提供錯誤截圖');

ALTER TABLE tickets AUTO_INCREMENT = 100;
ALTER TABLE status_transitions AUTO_INCREMENT = 100;
ALTER TABLE pause_histories AUTO_INCREMENT = 100;
