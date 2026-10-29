# Educational Resource Platform

A Spring Boot REST API for an educational resource platform. The
application uses Spring Security with JWT-based authentication, MySQL
for persistence, and Flyway for database migrations. Docker Compose runs
the application and database together.

## Tech stack

-   Java 21
-   Spring Boot 4.1.2
-   Maven
-   Spring Web MVC
-   Spring Security and JWT
-   Spring Data JPA / Hibernate
-   MySQL 8.4
-   Flyway database migrations
-   Bean Validation
-   Docker and Docker Compose

## Prerequisites

Install the following before running the project:

-   [Docker Desktop](https://www.docker.com/products/docker-desktop/)
    with Docker Compose
-   Git (optional, for cloning the repository)

You do not need to install Java, Maven, or MySQL locally when running
the application through Docker Compose.

## 1. Clone the repository

``` bash
git clone https://github.com/r-a-m-a-2-5-6/edu-classes.git
cd <YOUR_REPOSITORY_DIRECTORY>
```

Replace the placeholders with your GitHub repository URL and directory
name.

## 2. Configure environment variables

Create a local `.env` file by copying the example file.

**Windows PowerShell:**

``` powershell
Copy-Item .env.example .env
```

**macOS / Linux:**

``` bash
cp .env.example .env
```

Open `.env` and set values for the local environment. Example:

``` dotenv
DB_USERNAME=appuser
DB_PASSWORD=replace_with_a_strong_local_password
DB_ROOT_PASSWORD=replace_with_a_strong_root_password
JWT_SECRET=PASTE_BASE64URL_SECRET_HERE
```

These are example values only. Use your own strong secrets.

### Generate a Base64URL-encoded JWT secret

The application expects `JWT_SECRET` to be **Base64URL encoded**.
Generate a random 32-byte (256-bit) secret and encode it as Base64URL
without `=` padding.

**Windows PowerShell (recommended):**

``` powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$secret = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$secret
```

Copy the output into the `JWT_SECRET` value in `.env`. Do not add quotes
or spaces around the value.

The output should use only Base64URL characters (`A-Z`, `a-z`, `0-9`,
`-`, `_`) and should not contain `=` padding. Generate a different
secret for each environment.

> **Important:** The application must decode the Base64URL value before
> using it as the cryptographic key if that is how `JwtService` is
> implemented. Keep the configuration and key-decoding logic consistent;
> do not Base64URL-encode a secret a second time if the application
> already expects raw key bytes.

## 3. Start the application

Make sure Docker Desktop is running, then execute:

``` bash
docker compose up -d --build
```

The first build may take several minutes while Docker downloads
dependencies and images.

Check the container status:

``` bash
docker compose ps
```

View application logs:

``` bash
docker compose logs -f app
```

View database logs:

``` bash
docker compose logs -f db
```

Wait for the application to finish starting and for Flyway migrations to
complete successfully.

## 4. Access the application

-   API base URL: `http://localhost:8080`
-   MySQL host port: `127.0.0.1:3307` (the database container listens on
    port `3306` internally)

The database is intended to be accessed by the application through the
Compose service name `db`. Inside Docker, the JDBC URL should use
`db:3306`, not `localhost:3306`.

If API documentation is enabled in the application, use the
documentation URL configured by the project. Otherwise, test the
endpoints using Postman.

## 5. Database migrations

Flyway manages database schema changes. Hibernate is configured with
`ddl-auto=validate`, so Hibernate validates the schema rather than
creating or updating tables automatically.

Migration scripts are stored under:

``` text
src/main/resources/db/migration/
```

**Best practice:** once a migration has been applied to a shared or
persistent database, do not edit that migration file. Add a new
versioned migration instead. Editing an applied migration can cause a
Flyway checksum mismatch.

## 6. Run tests

To run the project tests locally, use:

``` bash
mvn clean verify
```

This command requires the test configuration and any required test
database/services to be available.

The Docker image build may skip test execution if the Dockerfile uses
`-DskipTests`. Run the test suite separately and check its result before
submitting changes.

## 7. Stop the application

Stop containers without deleting the database volume:

``` bash
docker compose down
```

Start them again:

``` bash
docker compose up -d
```

To remove the containers **and permanently delete the local database
volume and its data**, run:

``` bash
docker compose down -v
```

Only use `-v` when you are sure the local database data can be deleted.

## Environment variables

  -----------------------------------------------------------------------
  Variable                            Purpose
  ----------------------------------- -----------------------------------
  `DB_USERNAME`                       MySQL application user

  `DB_PASSWORD`                       Password for the MySQL application
                                      user

  `DB_ROOT_PASSWORD`                  MySQL root password used for local
                                      database initialization

  `JWT_SECRET`                        Random Base64URL-encoded JWT
                                      signing secret
  -----------------------------------------------------------------------

The Compose configuration also sets the Spring datasource URL, Flyway
settings, JWT issuer, and JWT expiry for the application.

## Security notes

-   Never commit `.env` to Git.
-   Commit `.env.example` with placeholders only.
-   Keep real database passwords and JWT signing secrets out of source
    code, logs, screenshots, and GitHub.
-   Use separate secrets for local development, testing, and production.
-   Rotate any secret that has accidentally been committed or shared.
-   Use HTTPS and environment-specific secret management for deployed
    environments.
-   Do not expose the MySQL port publicly in production.

## Troubleshooting

### Application cannot connect to MySQL

1.  Check that the `db` service is running and healthy:

    ``` bash
    docker compose ps
    ```

2.  Inspect the logs:

    ``` bash
    docker compose logs --tail=100 db
    docker compose logs --tail=100 app
    ```

3.  Confirm the username and password in `.env` match the Compose
    configuration. MySQL initialization variables do not automatically
    change credentials in an already-initialized data volume.

### Flyway checksum mismatch

An already-applied migration may have been edited. Restore the original
migration if the edit was accidental. If the schema or migration history
needs repair, inspect the database and migration changes first; do not
run repair blindly.

### Rebuild the application

``` bash
docker compose up -d --build
```

If a build cache issue is suspected:

``` bash
docker compose build --no-cache app
docker compose up -d
```

## Project structure

``` text
.
├── src/
│   ├── main/
│   │   ├── java/                 # Application source code
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/     # Flyway migration scripts
│   └── test/                     # Automated tests
├── Dockerfile
├── compose.yaml
├── .env.example
├── .gitignore
└── pom.xml
```
