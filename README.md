# OpenPoker

Open Source Planning Poker application for agile teams.

## Tech Stack

| Layer     | Technology                                |
|-----------|-------------------------------------------|
| Backend   | Java 21, Spring Boot 4.0, Maven           |
| Frontend  | React 18, Vite, TypeScript                |
| Database  | PostgreSQL (via Docker)                   |
| Infra     | Docker, Docker Compose                    |

## Project Structure

```
OpenPoker/
├── backend/          # Spring Boot REST API
│   └── src/
│       └── main/java/com/openpoker/
│           ├── controller/   # REST controllers
│           ├── service/      # Business logic
│           ├── repository/   # Data access (Spring Data JPA)
│           └── entity/       # JPA entities
└── frontend/         # React + Vite + TypeScript SPA
    └── src/
        ├── components/   # Reusable UI components
        ├── hooks/        # Custom React hooks
        └── services/     # API client services
```

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21
- Node.js 20+

### Run with Docker Compose

```bash
docker-compose up --build
```

### Backend (standalone)

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend (standalone)

```bash
cd frontend
npm install
npm run dev
```

## License

MIT
