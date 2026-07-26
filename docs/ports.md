| Service           |                         Port |
| ----------------- | ---------------------------: |
| GateBridge        |       **8080** (entry point) |
| Auth Service      |                         8085 |
| Knowledge Service |                         8086 |
| AI Service        |                         8087 |
| Worker Service    |  8088 (no HTTP, if possible) |


| Docker Compose    |                         Port |
| ----------------- | ---------------------------: |
| PostgreSQL        |                         5432 |
| Redis             |                         6379 |
| RabbitMQ          |                         5672 |
| Kafka             |                         9092 |
| Neo4j HTTP        |                         7474 |
| Neo4j Bolt        |                         7687 |
