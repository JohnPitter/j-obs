# J-Obs Microservices Sample

This sample demonstrates distributed tracing and service map visualization across multiple services.

## Architecture

```
                    ┌─────────────────┐
                    │     Client      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │  :8084
                    │    /j-obs       │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
     ┌─────────────────┐           ┌─────────────────┐
     │  Order Service  │           │Inventory Service│
     │    /j-obs       │  ──────▶  │    /j-obs       │
     │     :8085       │           │     :8086       │
     └─────────────────┘           └─────────────────┘
```

## Features Demonstrated

- **Distributed Tracing**: Trace context propagation across services
- **Service Map**: Visual representation of service dependencies
- **Cross-Service Spans**: See the complete request journey
- **Dependency Health**: Monitor downstream service health

## Running Locally

### Start all services (requires 3 terminals):

Terminal 1 - Inventory Service:
```bash
cd samples/j-obs-microservices/inventory-service
mvn spring-boot:run
```

Terminal 2 - Order Service:
```bash
cd samples/j-obs-microservices/order-service
mvn spring-boot:run
```

Terminal 3 - API Gateway:
```bash
cd samples/j-obs-microservices/api-gateway
mvn spring-boot:run
```

### Running with Docker Compose

```bash
cd samples/j-obs-microservices

# Build all services
mvn clean package -DskipTests

# Start with Docker Compose
docker-compose up -d
```

## Dashboards

Each service has its own J-Obs dashboard:

- **API Gateway**: http://localhost:8084/j-obs
- **Order Service**: http://localhost:8085/j-obs
- **Inventory Service**: http://localhost:8086/j-obs

## Testing the Trace Propagation

### Create an order (spans across all 3 services):
```bash
curl -X POST http://localhost:8084/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": "PROD-001", "quantity": 2, "customerName": "John Doe"}'
```

### Get all orders:
```bash
curl http://localhost:8084/api/orders
```

### Check inventory:
```bash
curl http://localhost:8084/api/inventory
```

### Check service health:
```bash
curl http://localhost:8084/api/health/services
```

## Trace Flow Example

When you create an order, the trace shows:

```
▼ POST /api/orders                              [Gateway]
  ├─ HTTP POST → order-service/api/orders       [Gateway → Order]
  │  ├─ Create order logic                      [Order]
  │  └─ HTTP GET → inventory-service/api/...    [Order → Inventory]
  │     └─ Check stock availability             [Inventory]
  └─ Return response                            [Gateway]
```

## Service Map

The Service Map page in J-Obs shows:

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│ API Gateway  │ ───▶ │Order Service │ ───▶ │  Inventory   │
│   ✅ 45ms    │      │   ✅ 120ms   │      │   ✅ 25ms    │
└──────────────┘      └──────────────┘      └──────────────┘
```

## Ports

| Service | Port | Dashboard |
|---------|------|-----------|
| API Gateway | 8084 | http://localhost:8084/j-obs |
| Order Service | 8085 | http://localhost:8085/j-obs |
| Inventory Service | 8086 | http://localhost:8086/j-obs |
