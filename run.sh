#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | xargs)
else
    echo "Warning: .env file not found!"
    echo "Please create .env file from .env.example"
    exit 1
fi

# Check if required environment variables are set
if [ -z "$DB_POSTGRES_URL" ] || [ -z "$DB_POSTGRES_USER" ] || [ -z "$DB_POSTGRES_PASSWORD" ]; then
    echo "Error: Required environment variables are not set!"
    echo "Please check your .env file"
    exit 1
fi

echo "Starting ReelTrack API..."
./gradlew run
