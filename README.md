# Order Management System

Three Spring Boot services built together from the repository root as a Maven
multi-module project: `com.suraj:order-management-system:1.0-SNAPSHOT`.

## Modules

| Module | HTTP port | Database |
| --- | --- | --- |
| product-service | 8080 | MongoDB: product-service |
| order-service | 8081 | MySQL: order-service |
| inventory-service | 8082 | MySQL: inventory-service |

## Build and test

Use JDK 17 or later. The parent POM sets the Java release to 17 for every module.
The Maven wrapper downloads Maven as needed. Docker must be running for the product
integration tests, which start a temporary MongoDB container. Order and inventory
tests use temporary H2 databases.

```sh
./mvnw clean verify
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Run a service

From the repository root, run one of these commands in a separate terminal:

```sh
./mvnw -pl product-service spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
```

For application startup, MongoDB must be available at `localhost:27017` and MySQL
at `localhost:3306`. Create the `order-service` and `inventory-service` MySQL
databases first. The local MySQL defaults are username `root` and an empty password;
override them with `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
Override the product database URI with `SPRING_MONGODB_URI`.

## IntelliJ IDEA

Open the root `pom.xml` as a project, or link it through the Maven tool
window. It imports all three modules automatically. Unlink any old standalone
service POMs if you continue using the existing IDE window.
