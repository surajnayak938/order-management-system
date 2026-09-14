# Order Management System

A Spring Boot microservices application built as a Maven multi-module project.
The services use Eureka for discovery, an API gateway for external HTTP traffic,
Kafka for order notifications, and Zipkin/Micrometer for distributed tracing.

## Modules

| Module | Application name | Port | Purpose |
| --- | --- | --- | --- |
| discovery-server | `discovery-server` | 8761 | Eureka service registry |
| api-gateway | `api-gateway` | 8080 | Gateway for service APIs |
| order-service | `order-service` | 8081 | Creates and persists orders |
| product-service | `product-service` | Random (`server.port=0`) | Product catalog |
| inventory-service | `inventory-service` | Random (`server.port=0`) | Stock availability |
| notification-service | `notification-service` | Random (`server.port=0`) | Consumes order notification events |

Use the gateway for client requests:

```text
http://localhost:8080/api/product
http://localhost:8080/api/order
http://localhost:8080/api/inventory?skuCode=iphone_13&skuCode=iPhone_13_red
```

## Kafka notifications

After an order is saved, `order-service` publishes an `OrderPlacedEvent` to the
`notificationTopic` topic. `notification-service` consumes the event and can be
extended to send email or other notifications.

Start Kafka from the repository root:

```sh
docker compose up -d
docker compose logs -f broker
```

Applications running from IntelliJ use `localhost:9092`; containers use
`broker:29092`.

## Tracing

Run Zipkin on `localhost:9411`. Services export spans to
`http://localhost:9411/api/v2/spans`. View them at
[http://localhost:9411/zipkin](http://localhost:9411/zipkin).

## Build and test

Use JDK 17 or later. The Maven wrapper downloads Maven as needed.

```sh
./mvnw clean verify
```

On Windows, use `mvnw.cmd` instead of `./mvnw`. Product integration tests
require Docker for a temporary MongoDB container. Order and inventory tests use
temporary H2 databases.

## Run locally

Start discovery first, then Kafka, and run each service in a separate terminal:

```sh
./mvnw -pl discovery-server spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl product-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

MongoDB must be available at `localhost:27017`, MySQL at `localhost:3306`,
Kafka at `localhost:9092`, and Zipkin at `localhost:9411`. Create the
`order-service` and `inventory-service` MySQL databases first. The default MySQL
username is `root` with an empty password; override it with
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.

## IntelliJ IDEA

Open the root `pom.xml` as a Maven project. IntelliJ imports all modules,
including `notification-service`, automatically.
