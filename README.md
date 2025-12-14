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
