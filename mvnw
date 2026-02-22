#!/bin/sh
# Maven Wrapper - downloads Maven if not installed
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
PROPERTIES="$DIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_VERSION="3.9.6"
MAVEN_HOME="$HOME/.m2/wrapper/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

# Use system Maven if available
if command -v mvn > /dev/null 2>&1; then
    exec mvn "$@"
fi

# Download Maven if not cached
if [ ! -f "$MAVEN_BIN" ]; then
    echo "Downloading Apache Maven $MAVEN_VERSION..."
    mkdir -p "$HOME/.m2/wrapper"
    URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$URL" | tar -xz -C "$HOME/.m2/wrapper/"
    else
        wget -qO- "$URL" | tar -xz -C "$HOME/.m2/wrapper/"
    fi
    mv "$HOME/.m2/wrapper/apache-maven-$MAVEN_VERSION" "$MAVEN_HOME" 2>/dev/null || true
fi

exec "$MAVEN_BIN" "$@"
