#!/bin/bash

# Setup Heroku environment variables for AI-RPG Platform

echo "🔧 Setting up Heroku environment variables..."

# Set Java runtime
heroku config:set JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -Xmx512m -XX:MaxMetaspaceSize=128m"

# Claude AI Configuration (you'll need to set your actual API key)
echo "⚠️  Please set your Claude API key:"
echo "heroku config:set CLAUDE_API_KEY=your_actual_api_key_here"

heroku config:set CLAUDE_MODEL=claude-sonnet-4-20250514
heroku config:set CLAUDE_MAX_TOKENS=1000
heroku config:set CLAUDE_TEMPERATURE=0.7

# AI Platform Configuration
heroku config:set AI_REQUESTS_PER_MINUTE=60
heroku config:set AI_CACHE_TTL_MINUTES=30
heroku config:set AI_LANGUAGE=en

# Game Configuration
heroku config:set GAME_SYSTEM=dnd
heroku config:set GAME_ADVENTURE=tsr_basic

# Logging Configuration
heroku config:set LOG_LEVEL=INFO

echo "✅ Environment variables configured!"
echo "📋 Current config:"
heroku config
