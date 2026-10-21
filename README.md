# Biznes ERP CRM

A desktop ERP and CRM application developed for a company's daily business operations, bringing customer management, inventory, production, sales and finance into one workspace.

The application interface is in **Azerbaijani**, with financial amounts displayed in **Azerbaijani manat (AZN)**.

## Demo

A walkthrough using fictional customer, inventory, payment and invoice data.

https://github.com/user-attachments/assets/0059221a-313a-45a5-a83d-a5fae95dfdab



## Overview

The system supports the operational flow of a business that manages raw materials, manufactures products and delivers them to customers. It connects product recipes and stock movements with purchases, deliveries, payments and monthly reports.

## Features

- **Business partners:** customer, manager and supplier records, customer assignments and outstanding balances.
- **Inventory:** raw materials, finished products, quantities, costs and stock movements.
- **Production:** product recipes, material consumption, production records and waste tracking.
- **Purchases and deliveries:** supplier purchases, customer deliveries, document records and payment balances.
- **Finance:** incoming and outgoing cash movements, payments and debt tracking.
- **Employees and payroll:** employee records, salaries, advances and bonuses.
- **Credit tracking:** credit agreements, installment payments and remaining balances.
- **Reporting:** financial dashboard, monthly reports and an activity log.
- **Invoicing:** invoice records, line items and PDF invoice generation.

## Technology

| Component | Technology |
| --- | --- |
| Language | Java 21 |
| Desktop interface | JavaFX 21 and CSS |
| Build and dependency management | Maven |
| Database access | JDBC |
| Default database | SQLite |
| Optional database | PostgreSQL |
| PDF generation | OpenPDF |
| Build automation | GitHub Actions on Windows with Java 21 |

## Getting started

### Requirements

- JDK 21
- Apache Maven 3.9 or later
- Internet access for the initial dependency download

### Build and run

From the directory containing `pom.xml`:

```sh
mvn clean verify
mvn javafx:run
```

Set `JAVA_HOME` to your JDK 21 installation. Maven downloads the required JavaFX, JDBC and PDF dependencies.

The generated JAR is a standard Maven artifact; it does not bundle its runtime dependencies. Use `mvn javafx:run` to launch the application during development.

### IntelliJ IDEA

1. Open `pom.xml` as a Maven project and allow dependencies to load.
2. Set the Project SDK and Maven Runner JRE to JDK 21.
3. Run the Maven `javafx:run` goal, or run `az.company.erp.MainLauncher`.
4. Set the working directory to the folder containing `pom.xml` so the application uses the correct configuration and data paths.

## Database configuration

### SQLite

SQLite is used by default and requires no separate database server. On first launch, the application creates its schema and the local database at:

```text
data/biznes_idareetme.db
```

### PostgreSQL

To use PostgreSQL:

1. Create an empty database and a database user with permission to create and access its tables.
2. Copy `config/database.properties.example` to `config/database.properties`.
3. Set `db.url`, `db.user` and `db.password` in the local configuration file.
4. Start the application from the project root. The schema is created automatically.

If the PostgreSQL connection fails, the current implementation falls back to SQLite. Check the startup console message to see which database is active. Data is not synchronized between the two databases.

Local connection settings, database files and generated build output are excluded through `.gitignore`. Company records and credentials are not included in the source package.

## Source structure

```text
src/main/
├── java/az/company/erp/
│   ├── MainLauncher.java         # Application entry point
│   ├── ErpCrmApplication.java    # JavaFX screens and application flow
│   ├── Database.java             # JDBC operations and schema initialization
│   ├── InvoiceEntry.java         # Invoice and invoice item models
│   └── InvoicePdfGenerator.java  # PDF invoice generation
└── resources/styles/app.css     # Application styling
```

## Verification and current limitations

The project has been built with JDK 21. Local checks covered SQLite schema creation, startup data loading, customer and invoice persistence, PDF generation and application startup in IntelliJ IDEA.

GitHub Actions runs `mvn clean verify` on pushes and pull requests. The repository does not yet contain automated functional tests, and PostgreSQL workflows have not been verified.

- The application currently has no login or role-based access control.
- PDF generation uses Arial from the Windows fonts directory when available, with a Helvetica fallback. Azerbaijani character rendering on other operating systems needs verification.

## License

No license has been specified for the project's source code. Third-party dependencies retain their respective licenses.
