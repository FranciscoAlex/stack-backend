# JWT Authentication API Documentation

## Overview
This Spring Boot application provides JWT-based authentication via REST API endpoints. The authentication system uses access tokens (short-lived) and refresh tokens (long-lived) for secure user sessions.

## Base URL
```
http://localhost:8080/api
```

## Endpoints

### 1. Health Check
**GET** `/api/public/health`

Check if the API is running.

**Response:**
```json
{
  "status": "UP",
  "message": "API is running"
}
```

### 2. Register User
**POST** `/api/auth/register`

Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "username": "johndoe"
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Email already exists"
}
```

### 3. Login
**POST** `/api/auth/login`

Authenticate and receive JWT tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "username": "johndoe"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "message": "Invalid email or password"
}
```

### 4. Refresh Token
**POST** `/api/auth/refresh`

Get a new access token using a refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "username": "johndoe"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "message": "Invalid refresh token"
}
```

### 5. Logout
**POST** `/api/auth/logout`

Invalidate a refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

## Using Protected Endpoints

To access protected endpoints, include the access token in the Authorization header:

```
Authorization: Bearer <accessToken>
```

**Example:**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
     http://localhost:8080/api/protected-endpoint
```

## Token Expiration

- **Access Token**: 24 hours (86400000 milliseconds)
- **Refresh Token**: 7 days (604800000 milliseconds)

## Configuration

### Environment Variables

Set these environment variables before running the application:

- `DB_USERNAME`: Database username (default: `postgres`)
- `DB_PASSWORD`: Database password (required)
- `JWT_SECRET`: Secret key for JWT signing (must be at least 256 bits)

### Database Connection

The application is configured to connect to Supabase PostgreSQL database. Update the connection details in `application.properties` if needed.

## Security Features

- Password encryption using BCrypt
- JWT token-based authentication
- Refresh token rotation
- Stateless session management
- CORS enabled for cross-origin requests

## Example Usage with cURL

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

