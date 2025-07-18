#!/bin/bash

echo "🔧 Location Context Bug Fix - Testing Movement Commands"
echo "======================================================="
echo

# Check if server is running
echo "📡 Checking if backend server is running..."
if ! curl -s http://localhost:8080/api/metrics > /dev/null; then
    echo "❌ Backend server is not running!"
    echo "   Please start it with: ./start-backend.sh"
    exit 1
fi

echo "✅ Backend server is running"
echo

# Create a test session
echo "🎮 Creating test session..."
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8080/api/session/create \
  -H "Content-Type: application/json" \
  -d '{"player_id":"test-player","player_name":"Debug Hero"}')

# Extract session ID (fix: use snake_case field name)
SESSION_ID=$(echo "$SESSION_RESPONSE" | grep -o '"session_id":"[^"]*"' | cut -d'"' -f4)

if [ -z "$SESSION_ID" ]; then
    echo "❌ Failed to create session"
    echo "Response: $SESSION_RESPONSE"
    exit 1
fi

echo "✅ Session created: $SESSION_ID"
echo

# Test initial status
echo "📊 Checking initial player status..."
curl -s "http://localhost:8080/api/game/status?session_id=$SESSION_ID" | \
    grep -o '"current_location":"[^"]*"' | head -1
echo

# Test movement to cave entrance
echo "🚶 Testing movement: village -> cave_entrance"
MOVE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/game/action \
  -H "Content-Type: application/json" \
  -d "{\"session_id\":\"$SESSION_ID\",\"command\":\"/go cave_entrance\"}")

echo "Response preview:"
echo "$MOVE_RESPONSE" | head -c 200
echo "..."
echo

# Check updated status
echo "📊 Checking updated player status..."
STATUS_RESPONSE=$(curl -s "http://localhost:8080/api/game/status?session_id=$SESSION_ID")
echo "$STATUS_RESPONSE" | grep -o '"current_location":"[^"]*"' | head -1
echo "$STATUS_RESPONSE" | grep -o '"location_name":"[^"]*"' | head -1
echo

# Test AI prompt to see if context is correct
echo "🤖 Checking AI context for correct location..."
AI_PROMPT=$(curl -s "http://localhost:8080/api/ai/prompt?session_id=$SESSION_ID")
echo "Current location in AI context:"
echo "$AI_PROMPT" | grep -A 3 -B 3 "Player Location" | head -10
echo

echo "🧪 Test complete!"
echo
echo "📋 What to check:"
echo "   1. Location should change from 'village' to 'cave_entrance'"
echo "   2. AI context should show the new location"
echo "   3. Check server logs for movement and context refresh messages"
echo
echo "📊 To see full status:"
echo "   curl \"http://localhost:8080/api/game/status?session_id=$SESSION_ID\""
echo
echo "🤖 To see full AI context:"
echo "   curl \"http://localhost:8080/api/ai/prompt?session_id=$SESSION_ID\""
