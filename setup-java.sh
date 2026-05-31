#!/bin/bash

# Script to set up Java 17 for Spring Boot backend

echo "Setting up Java 17 for Spring Boot backend..."

# Check if Java 17 is already available
JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
if [ -n "$JAVA_17_HOME" ]; then
    # Verify it's actually Java 17
    JAVA_VERSION=$("$JAVA_17_HOME/bin/java" -version 2>&1 | head -1)
    if echo "$JAVA_VERSION" | grep -q "17\|1.17"; then
        echo "✓ Java 17 found at: $JAVA_17_HOME"
        export JAVA_HOME=$JAVA_17_HOME
        export PATH=$JAVA_HOME/bin:$PATH
        echo "✓ JAVA_HOME set to: $JAVA_HOME"
        "$JAVA_HOME/bin/java" -version
        exit 0
    fi
fi

echo "Java 17 not found. Please install it using one of these methods:"
echo ""
echo "Method 1: Using Homebrew (requires password):"
echo "  sudo chown -R \$(whoami) /usr/local/share/doc /usr/local/share/man /usr/local/share/man/man1"
echo "  chmod u+w /usr/local/share/doc /usr/local/share/man /usr/local/share/man/man1"
echo "  brew install openjdk@17"
echo ""
echo "Method 2: Using Homebrew Cask (requires password):"
echo "  brew install --cask temurin@17"
echo ""
echo "Method 3: Manual installation:"
echo "  1. Download from: https://adoptium.net/temurin/releases/?version=17"
echo "  2. Install the .pkg file"
echo ""
echo "After installation, run this script again to configure JAVA_HOME."

