# 智慧 IT 服務工單系統

企業內部 IT 支援平台。員工開單描述問題，系統自動分類、分派；工程師處理工單直到結案；主管透過儀表板檢視 SLA 達成率。

## 架構

| 服務 | 說明 | 連接埠 |
|---|---|---|
| ticket-service | 工單生命週期、狀態機、SLA 計時 | 8081 |
| kb-service | 知識庫文章管理 | 8082 |
| MySQL 8.0 | 關聯式資料庫（ticket_db、kb_db） | 3306 |

## 快速啟動

```bash
docker compose up --build
```

首次啟動需要建置映像檔，約需 3–5 分鐘。啟動完成後：

- ticket-service 健康檢查：http://localhost:8081/actuator/health
- kb-service 健康檢查：http://localhost:8082/actuator/health

## 停止與清除

```bash
# 停止服務（保留資料）
docker compose down

# 停止服務並清除資料
docker compose down -v
```

## 專案結構

```
├── pom.xml                  # Maven 父 POM
├── docker-compose.yml       # 一鍵啟動
├── init-db/                 # MySQL 初始化腳本
├── ticket-service/          # 工單服務
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── kb-service/              # 知識庫服務
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
└── architecture.md          # 架構決策文件
```

## 技術棧

- Java 17 + Spring Boot 3.3.5
- Spring Data JPA + MySQL 8.0
- Spring Boot Actuator（健康檢查）
- Docker + Docker Compose（multi-stage build）
- Lombok
