Bank Bots API
A RESTful API for managing bank bots that integrate with bank APIs to retrieve account and transaction data for financial tracking applications.

Overview
The Bank Bots API provides a standardized interface for registering, configuring, and interacting with bank bots. These bots connect to various bank APIs and provide a unified way to access financial data such as account balances and transactions.

This API is designed to be used in mobile financial tracking applications built with Kotlin Multiplatform (KMP), supporting Android and iOS platforms.

Features
Bot Management: Register, update, and delete bank bots
Multiple Authentication Methods: Support for OAuth2, username/password, and API key authentication
Account Data: Retrieve accounts, balances, and transactions
Webhooks: Get real-time notifications for new transactions and balance changes
Security: JWT authentication, session management, and rate limiting
Getting Started
Prerequisites
JDK 11 or higher
PostgreSQL
Docker and Docker Compose (optional, for containerized deployment)
Installation
Clone this repository:
bash
git clone https://github.com/yourcompany/bank-bots-api.git
cd bank-bots-api
Build the project:
bash
./gradlew build
Run the application:
bash
./gradlew run
Alternatively, use Docker Compose:

bash
docker-compose up -d
Configuration
The application can be configured through environment variables:

PORT: Server port (default: 8080)
DATABASE_URL: PostgreSQL connection URL
DATABASE_USER: Database username
DATABASE_PASSWORD: Database password
JWT_SECRET: Secret key for JWT token signing
LOAD_SAMPLE_DATA: Set to "true" to load sample data (for testing)
API Documentation
Bot Management
Register a Bot
POST /api/v1/bots
Request Body:

json
{
  "name": "Sberbank Bot",
  "handle": "@sberbank",
  "bankCode": "SBER",
  "description": "Bot for accessing Sberbank accounts and transactions",
  "authType": "oauth2",
  "credentials": {
    "clientId": "abc123",
    "clientSecret": "xyz789",
    "authorizationUrl": "https://sberbank.ru/oauth/authorize",
    "tokenUrl": "https://sberbank.ru/oauth/token"
  },
  "logoUrl": "https://sberbank.ru/logo.png",
  "supportedFeatures": ["accounts", "transactions"]
}
Get List of Bots
GET /api/v1/bots?limit=20&offset=0&sort=name
Get Bot Details
GET /api/v1/bots/{bot_id}
Update Bot
PUT /api/v1/bots/{bot_id}
Delete Bot
DELETE /api/v1/bots/{bot_id}
Bot Authentication
Authorize with Bot (Login/Password)
POST /api/v1/bots/{bot_id}/authorize
Request Body:

json
{
  "username": "user123",
  "password": "secret"
}
Authorize with Bot (API Key)
POST /api/v1/bots/{bot_id}/authorize
Request Body:

json
{
  "apiKey": "api-key-value"
}
Authorize with Bot (OAuth2)
POST /api/v1/bots/{bot_id}/authorize
Response:

json
{
  "authorizationUrl": "https://bank.com/oauth/authorize?client_id=abc&..."
}
Account Data
Get Accounts
GET /api/v1/bots/{bot_id}/accounts
Headers:

X-Session-Token: <session_token>
Get Account Balance
GET /api/v1/bots/{bot_id}/accounts/{account_id}/balance
Headers:

X-Session-Token: <session_token>
Get Transactions
GET /api/v1/bots/{bot_id}/accounts/{account_id}/transactions?from=2023-01-01&to=2023-01-31
Headers:

X-Session-Token: <session_token>
Webhooks
Register Webhook
POST /api/v1/webhooks
Request Body:

json
{
  "botId": "bot-id",
  "event": "transaction.created",
  "url": "https://yourapp.com/webhook-handler"
}
Security
All API endpoints are secured with JWT authentication
Session tokens are used for bot authentication
Rate limiting is implemented to prevent abuse
Webhook signatures are verified using HMAC-SHA256
Deployment
Use the provided deploy.sh script for simplified deployment:

bash
# Build the application
./deploy.sh build

# Run locally
./deploy.sh run

# Deploy with Docker
./deploy.sh docker-build
./deploy.sh docker-run

# Deploy to a remote server
./deploy.sh deploy
Contributing
Fork the repository
Create a feature branch
Commit your changes
Push to the branch
Create a Pull Request
License
This project is licensed under the MIT License - see the LICENSE file for details.

