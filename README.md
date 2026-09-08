# Order Management System

A Java and Spring Boot microservices project under development.

## Services

- `product-service/`: Product creation and persistence using MongoDB.
- Order and inventory services are planned.

Each service is maintained in its own directory and can be built independently.

## Run the product service locally

Requirements: JDK 17 or later and MongoDB running locally on port 27017.
The included Maven wrapper downloads Maven as needed.

```sh
cd product-service
./mvnw clean install
./mvnw spring-boot:run
```

The service uses `mongodb://localhost:27017/product-service` and the default HTTP port `8080`.
Override the database connection using the `SPRING_MONGODB_URI` environment variable.

On macOS, if MongoDB Community 8.0 is installed through Homebrew:

```sh
brew services start mongodb-community@8.0
```

MongoDB Compass can connect to `mongodb://localhost:27017` to browse collections and documents.
