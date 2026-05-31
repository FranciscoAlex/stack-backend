#!/bin/bash

# Script to start Spring Boot backend with Java 17

cd "$(dirname "$0")"

# Try to find and set Java 17
JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
if [ -n "$JAVA_17_HOME" ]; then
    # Verify it's actually Java 17
    JAVA_VERSION=$("$JAVA_17_HOME/bin/java" -version 2>&1 | head -1)
    if echo "$JAVA_VERSION" | grep -q "17\|1.17"; then
        export JAVA_HOME=$JAVA_17_HOME
        export PATH=$JAVA_HOME/bin:$PATH
        echo "Using Java 17 from: $JAVA_HOME"
        "$JAVA_HOME/bin/java" -version
    else
        echo "ERROR: Java 17 is required but not found."
        echo ""
        echo "Please install Java 17 first:"
        echo "  brew install --cask temurin@17"
        echo ""
        echo "Or run: ./setup-java.sh"
        exit 1
    fi
else
    echo "ERROR: Java 17 is required but not found."
    echo ""
    echo "Please install Java 17 first:"
    echo "  brew install --cask temurin@17"
    echo ""
    echo "Or run: ./setup-java.sh"
    exit 1
fi

# Check if database password is set
if [ -z "$DB_PASSWORD" ]; then
    echo "WARNING: DB_PASSWORD environment variable is not set."
    echo "Please set it before starting the backend:"
    echo "  export DB_PASSWORD=your-supabase-password"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Start Spring Boot application
echo "Starting Spring Boot backend..."
./mvnw spring-boot:run

