#!/bin/bash
# MySQL 容器首次啟動時執行。密碼由環境變數提供（見 .env），不寫進檔案。
set -euo pipefail

# set -u 擋不住「已設定但為空」的變數；明確要求非空，避免建立出空密碼帳號。
: "${MYSQL_ROOT_PASSWORD:?必須在 .env 設定 MYSQL_ROOT_PASSWORD（不可為空）}"
: "${DB_APP_PASSWORD:?必須在 .env 設定 DB_APP_PASSWORD（不可為空）}"

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE DATABASE IF NOT EXISTS ticket_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS kb_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'app'@'%' IDENTIFIED BY '${DB_APP_PASSWORD}';
GRANT ALL PRIVILEGES ON ticket_db.* TO 'app'@'%';
GRANT ALL PRIVILEGES ON kb_db.* TO 'app'@'%';
FLUSH PRIVILEGES;
EOSQL
