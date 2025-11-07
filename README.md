# transactions-app

A banking transactions management application built with Quarkus that allows CSV import and provides a web interface for viewing transactions.

## Features

- **CSV Import**: Upload transactions in CSV format via REST API
- **Web Interface**: View all transactions in a formatted HTML table with highlighting for the largest income transaction
- **Data Validation**: Comprehensive validation for transaction fields (reference, timestamp, amount, currency)
- **Duplicate Detection**: Prevents importing transactions with duplicate references
- **PostgreSQL Storage**: Persistent storage

## Quick Start

### Running with Docker Compose

Start the application with all dependencies:

```bash
docker-compose up --build
```

The application will be available at **http://localhost/transactions**

### API Usage

**Upload transactions from CSV:**
```bash
curl -X POST -H 'Content-Type: text/csv' --data-binary @transactions.csv http://localhost/transactions
```

**View transactions in browser:**
```
http://localhost:5000/transactions
```

### CSV Format

The CSV file must include the following headers:
```csv
reference,timestamp,amount,currency,description
TX001,2024-01-15T10:30:00Z,100.50,EUR,Payment for services
TX002,2024-01-16T14:20:00Z,-50.25,USD,Refund
```

- **reference**: Unique transaction identifier (required)
- **timestamp**: ISO 8601 format (required)
- **amount**: Decimal number, negative for expenses (required)
- **currency**: 3-letter ISO currency code (required)
- **description**: Transaction description (optional)

## Architecture

The application uses a multi-container Docker setup:

- **PostgreSQL**: Database running on port 5432
- **Quarkus App**: Backend application with REST API and Qute templating
  - Main port: 8080 (internal)
  - Management port: 9000 (for health checks)
- **Nginx**: Reverse proxy exposing only `/transactions` endpoints on port 5000

Security is enforced through Nginx, which blocks access to development endpoints (Swagger UI, metrics, dev console).

## Development

### Run in development mode (Automatically starts dependencies like database in background - using docker)

```bash
./mvnw quarkus:dev
```

Access the application at http://localhost:8080/transactions  
Dev UI available at http://localhost:8080/q/dev/

### Run tests

```bash
./mvnw test
```

## Technology Stack

- **Framework**: Quarkus 3.29.0
- **Java**: 21
- **Database**: PostgreSQL 16
- **ORM**: Hibernate with Panache (Active Record pattern)
- **Templating**: Qute
- **CSV Parsing**: Apache Commons CSV
- **Testing**: JUnit 5, REST Assured

