# Account Management System - CashHive

A project utilising Spring Boot and Java to create an account management system. 
This system stores user and account information and provides transaction 
management functionality.

## Technology Stack
- Spring Boot 3.4.3
- Java 17
- Spring Data JPA
- H2 Database (Memory Mode)
- Redis (Concurrency Control)
- JUnit 5 (Unit Testing)
- Lombok
- Gradle
- Swagger (Spring Doc)

## Key Features
### Account-Related Features
1. **Account Creation**
   - Create accounts by inputting user ID and initial balance
   - Account numbers are generated as 10-digit random numbers (with duplicate checking)
   - Maximum of 10 accounts per user
2. **Account Closure**
   - Verify account ownership before processing closure
   - Accounts with remaining balance cannot be closed
3. **Account List Query**
   - View all account information (account number, balance) for a user

### Transaction-Related Features
1. **Balance Utilisation**
   - Deduct specified amount from account
   - Perform validation such as insufficient balance, closed accounts, etc.
2. **Balance Utilisation Cancellation**
   - Process cancellation based on specific transaction ID
   - Only allows cancellation of the exact original amount
3. **Transaction Query**
   - View detailed transaction information via transaction ID

## System Structure
``` text
com.example.myaccountsystem
├── config                  # System configuration classes
│   ├── DataInitializer.java    # Initial data setup
│   ├── RedisConfig.java        # Redis configuration
│   └── SwaggerConfig.java      # Swagger API documentation setup
├── controller              # API controllers
│   ├── AccountController.java  # Account-related APIs
│   └── TransactionController.java # Transaction-related APIs
├── dto                     # Data transfer objects
├── entity                  # DB entity classes
│   ├── Account.java        # Account information
│   ├── Transaction.java    # Transaction information
│   └── User.java           # User information
├── exception               # Exception handling
├── repository              # Data access layer
├── service                 # Business logic
│   ├── AccountService.java     # Account-related services
│   ├── RedisLockService.java   # Redis lock service
│   └── TransactionService.java # Transaction-related services
└── type                    # Enumeration type definitions
```

## API Specification
### Account API
1. **Account Creation**
   - `POST /api/account`
   - Request: `CreateAccountRequest` (user ID, initial balance)
   - Response: `CreateAccountResponse` (user iD, account number, creation timestamp)
2. **Account Closure**
    - `DELETE /api/account`
    - Request: `UnregisterAccountRequest` (user ID, account number)
    - Response: `UnregisterAccountResponse` (user ID, account number, closure timestamp)
3. **Account Query**
   - `GET /api/account/{userId}`
   - Response: `GetAccountsResponse` (user ID, account list)

### Transaction API
1. **Balance Utilisation**
   - `POST /api/transaction/use`
   - Request: `UseBalanceRequest` (user ID, account number, amount)
   - Response: `UseBalanceResponse` (account number, transaction result, transaction ID, amount, transaction timestamp)
2. **Balance Utilisation Cancellation**
    - `POST /api/transaction/cancel`
    - Request: `CancelBalanceRequest` (transaction ID, account number, account)
    - Response: `CancelBalanceResponse` (account number, transaction result, transaction ID, amount, transaction timestamp)
3. **Transaction Query**
   - `GET /api/transaction/{transactionId}`
   - Response: `GetTransactionResponse` (account number, transaction type, transaction result, transaction ID, amount, transaction timestamp)

## Concurrency Problem Resolution
To resolve concurrency issues (lost updates) that may occur during account balance management,
the following strategies are employed:

1. **Redis Distributed Lock**
   - Apply distributed locks using Redis for each account
   - Implement lock acquisition, release, and extension functionality via `RedisLockService`
2. **DB Lock**
   - Use JPA's Pessimistic Lock for concurrency control at the DB level
   - Apply `@Lock(LockModeType.PESSIMISTIC_WRITE`

## Error Handling
All APIs share the following error response structure:

``` json
{
  "errorCode": "ERROR_CODE",
  "errorMessage": "Error message"
}
```

Key error codes:
- `USER_NOT_FOUND`: User does not exist
- `MAX_ACCOUNT_PER_USER_10`: User has reached the maximum account limit (10)
- `ACCOUNT_NOT_FOUND`: Account does not exist
- `ACCOUNT_OWNER_MISMATCH`: Account owner and requester are different
- `ACCOUNT_ALREADY_UNREGISTERED`: Account already closed
- `ACCOUNT_HAS_BALANCE`: Attempt to close an account with remaining balance
- `ACCOUNT_TRANSACTION_LOCK`: Account is locked by another transaction
- `AMOUNT_EXCEED_BALANCE`: Insufficient balance
- `TRANSACTION_NOT_FOUND`: Transaction information not found
- `TRNASACTION_ACCOUNT_MISMATCH`: Transaction and account mismatch
- `CANCEL_MUST_FULLY`: Partial cancellation attempt (only full cancellation allowed)
- `TOO_SMALL_AMOUNT`: Minimum transaction amount limit
- `TOO_LARGE_AMOUNT`: Maximum transaction amount limit

## Getting Started
### Requirements
- Java 17
- Gradle
- Redis

### Installation and Execution
``` bash
# Clone the project
git clone https://github.com/yourusername/account-management-system.git
cd account-management-system

# Build
./gradlew build

# Run
./gradlew bootRun
```

### API Documentation
- Swagger UI: http://localhost:8080/swagger-ui.html

### Testing
The system includes unit tests which can be run with the following command:
``` bash
./gradlew test
```

The following tests are included:
- `AccountServiceTest`: Account-related service tests
- `TransactionServiceTest`: Transaction-related service tests
- `RedisLockServiceTest`: Redis lock-related tests
- `AccountControllerTest`: Account API tests
- `TransactionControllerTest`: Transaction API tests

## Licence
MIT Licence