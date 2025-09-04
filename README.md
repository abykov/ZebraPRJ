# ZebraPRJ
Simple REST service for managing list of users.
## Table of improvements:
- [√] Created GET /users endpoint
- [√] Put it into a docker container
- [√] Integrated with Jenkins pipeline
- [√] Integrated with postgreSQL
- [-] Added H2 DB for tests
- [√] Add Testcontainers instead of H2
- [√] Added docker compose to run Jenkins and Postgre
- [√] Add POST /addUser
- [√] Add DELETE /user
- [√] Add tests for POST /addUser and DELETE /user
- [√] Add tests for /crazy endpoint
- [√] GRPc service
- [√] Add POST /userproperty backed by MongoDB

## MongoDB integration
This project now uses **MongoDB** to store user property data. Ensure a MongoDB instance
is available when running the application. The `POST /userproperty` endpoint persists
received records into MongoDB.

### POST /userproperty
Adds one or more user property objects and returns the persisted records. Example request:

```http
POST /userproperty
Content-Type: application/json

{
  "userId": "123",
  "address": "1 Main St",
  "organisation": "ACME",
  "favouriteColour": "blue"
}
```