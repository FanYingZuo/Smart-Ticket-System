#!/bin/bash
# MySQL 容器首次啟動時執行。密碼由環境變數提供（見 .env），不寫進檔案。
set -euo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE DATABASE IF NOT EXISTS ticket_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS kb_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'app'@'%' IDENTIFIED BY '${DB_APP_PASSWORD}';
GRANT ALL PRIVILEGES ON ticket_db.* TO 'app'@'%';
GRANT ALL PRIVILEGES ON kb_db.* TO 'app'@'%';
FLUSH PRIVILEGES;
EOSQL
