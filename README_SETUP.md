# Backend Setup Instructions

## Prerequisites

The Spring Boot backend requires **Java 17 or higher**. Currently, your system has Java 8 installed.

## Quick Start

### Step 1: Install Java 17

Open a terminal and run one of these commands (you'll be prompted for your password):

```bash
# Recommended: Install using Homebrew Cask
brew install --cask temurin@17
```

Or if you prefer the formula version:

```bash
# Fix permissions first
sudo chown -R $(whoami) /usr/local/share/doc /usr/local/share/man /usr/local/share/man/man1
chmod u+w /usr/local/share/doc /usr/local/share/man /usr/local/share/man/man1

# Then install
brew install openjdk@17
```

### Step 2: Verify Java 17 Installation

After installation, verify it's working:

```bash
/usr/libexec/java_home -v 17
java -version  # Should show version 17.x
```

### Step 3: Set Database Password

Set your Supabase database password as an environment variable:

```bash
export DB_PASSWORD=your-supabase-database-password
```

You can also set a JWT secret (optional, defaults are provided):

```bash
export JWT_SECRET=your-256-bit-secret-key-at-least-32-characters-long
```

### Step 4: Start the Backend

Use the provided script:

```bash
cd stack_backend
./start-backend.sh
```

Or manually:

```bash
cd stack_backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw spring-boot:run
```

## Services Status

Once both services are running:

- **Frontend**: http://localhost:5173 (React/Vite)
- **Backend API**: http://localhost:8080 (Spring Boot)
- **Health Check**: http://localhost:8080/api/public/health

## API Endpoints

See `API_DOCUMENTATION.md` for complete API documentation.

### Quick Test

```bash
# Health check
curl http://localhost:8080/api/public/health

# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser"
  }'
```

## Troubleshooting

### Java Version Issues

If you see "No compiler is provided" or "Unsupported class file major version", make sure Java 17 is installed and JAVA_HOME is set correctly.

### Database Connection Issues

Make sure:
1. Your Supabase database password is set in `DB_PASSWORD` environment variable
2. The database URL in `application.properties` is correct
3. Your Supabase project is active

### Port Already in Use

If port 8080 is already in use, you can change it in `application.properties`:
```properties
server.port=8081
```

