[![CircleCI](https://circleci.com/gh/springframeworkguru/mssc-beer-order-service.svg?style=svg)](https://circleci.com/gh/springframeworkguru/mssc-beer-order-service)

## 📦 mssc-beer-order-service

# 🧾 Beer Order Service

Handles the **beer ordering workflow** and manages the lifecycle of beer orders.

## Responsibilities
- Create and manage beer orders
- Coordinate with Beer and Inventory services
- Enforce business rules for ordering

## Architecture Role
- Communicates with Beer Service and Inventory Service
- Accessible externally via API Gateway

## Tech Stack
- Spring Boot
- Spring Cloud OpenFeign / REST
- Spring Cloud Eureka Client

## 1️⃣ Architecture & Flow Diagrams

### 🌐 High-Level Microservices Architecture

```mermaid
flowchart LR
    Client --> Gateway
    Gateway --> Eureka

    Gateway --> BeerService
    Gateway --> OrderService

    OrderService --> InventoryService
    InventoryService --> FailoverService

    BeerService --> ConfigServer
    OrderService --> ConfigServer
    InventoryService --> ConfigServer

    ConfigServer --> ConfigSource[(Git Config Repo)]
```

---

### 🔍 Service Discovery Flow

```mermaid
sequenceDiagram
    participant Service
    participant Eureka

    Service->>Eureka: Register instance
    Eureka-->>Service: Registration confirmed

    Service->>Eureka: Heartbeat
    Eureka-->>Service: OK
```

---

### ⚙️ Configuration Loading Flow

```mermaid
sequenceDiagram
    participant Service
    participant ConfigServer
    participant GitRepo

    Service->>ConfigServer: Request configuration
    ConfigServer->>GitRepo: Fetch config files
    GitRepo-->>ConfigServer: application.yml
    ConfigServer-->>Service: Environment properties
```

---

## 2️⃣ Security & Gateway Flow

### 🔐 Security Responsibility Model

| Layer             | Responsibility                             |
| ----------------- | ------------------------------------------ |
| API Gateway       | Authentication, request filtering, routing |
| Internal Services | Authorization, service-to-service trust    |

---

### 🔁 Request Flow with Security

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Eureka
    participant Service

    Client->>Gateway: HTTP Request
    Gateway->>Eureka: Resolve service name
    Eureka-->>Gateway: Service instance
    Gateway->>Service: Forward request
    Service-->>Gateway: Response
    Gateway-->>Client: Final response
```

### Notes

* Gateway is the **single external entry point**
* Services are **not exposed directly**
* Security policies can be centralized at the gateway level

---

## 3️⃣ How to Run the Whole System Locally

### ✅ Prerequisites

* Java 17+
* Maven 3.x
* Git

---

### ▶️ Startup Order (Important)

Start services in the following order:

1. **Config Source (Git repo)**

   * No runtime needed (configuration only)

2. **Config Server** (`mssc-brewery-config`)

   ```bash
   mvn spring-boot:run
   ```

3. **Eureka Server** (`mssc-brewery-eureka`)

   ```bash
   mvn spring-boot:run
   ```

4. **Core Services** (any order after Eureka)

   * `mssc-beer-service`
   * `mssc-beer-inventory-service`
   * `mssc-beer-order-service`
   * `mssc-beer-inventory-failover-service`

5. **API Gateway** (`mssc-brewery-gateway`)

---

### 🌍 Local Endpoints

| Component        | URL                                                      |
| ---------------- | -------------------------------------------------------- |
| Eureka Dashboard | [http://localhost:8761](http://localhost:8761)           |
| API Gateway      | [http://localhost:9090](http://localhost:9090) (example) |
| Config Server    | [http://localhost:8888](http://localhost:8888)           |

---
