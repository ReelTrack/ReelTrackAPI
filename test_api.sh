#!/bin/bash

# ReelTrack API Test Script
# This script tests all API endpoints

BASE_URL="http://localhost:8080"

echo "=== ReelTrack API Testing ==="
echo ""

# Test 1: Health Check
echo "1. Testing Health Check..."
curl -s "$BASE_URL/health" | jq
echo ""

# Test 2: Get all users (should be empty initially)
echo "2. Getting all users..."
curl -s "$BASE_URL/api/users" | jq
echo ""

# Test 3: Create a new user
echo "3. Creating a new user..."
USER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "email": "test@example.com",
    "passwordHash": "$2a$10$abcdefghijklmnopqrstuv",
    "role": "USER"
  }')
echo "$USER_RESPONSE" | jq
USER_ID=$(echo "$USER_RESPONSE" | jq -r '.id')
echo "Created user with ID: $USER_ID"
echo ""

# Test 4: Get user by ID
echo "4. Getting user by ID..."
curl -s "$BASE_URL/api/users/$USER_ID" | jq
echo ""

# Test 5: Update user
echo "5. Updating user..."
curl -s -X PUT "$BASE_URL/api/users/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user_updated",
    "email": "test_updated@example.com",
    "passwordHash": "$2a$10$newhashedpassword",
    "role": "MODERATOR"
  }' | jq
echo ""

# Test 6: Get updated user
echo "6. Getting updated user..."
curl -s "$BASE_URL/api/users/$USER_ID" | jq
echo ""

# Test 7: Ban user
echo "7. Banning user..."
curl -s -X POST "$BASE_URL/api/users/$USER_ID/ban" | jq
echo ""

# Test 8: Check banned status
echo "8. Checking banned status..."
curl -s "$BASE_URL/api/users/$USER_ID" | jq '.isBanned'
echo ""

# Test 9: Unban user
echo "9. Unbanning user..."
curl -s -X POST "$BASE_URL/api/users/$USER_ID/unban" | jq
echo ""

# Test 10: Get all users
echo "10. Getting all users..."
curl -s "$BASE_URL/api/users" | jq
echo ""

# Test 11: Delete user
echo "11. Deleting user..."
curl -s -X DELETE "$BASE_URL/api/users/$USER_ID" | jq
echo ""

# Test 12: Verify deletion
echo "12. Verifying deletion (should return 404)..."
curl -s "$BASE_URL/api/users/$USER_ID" | jq
echo ""

echo "=== Testing Complete ==="
