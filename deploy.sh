#!/bin/bash

# AI-RPG Platform Heroku Deployment Script

echo "🚀 Starting AI-RPG Platform Heroku Deployment..."

# Step 1: Clean and build locally first
echo "🔧 Cleaning and building locally..."
./gradlew clean build

if [ $? -ne 0 ]; then
    echo "❌ Local build failed. Please fix build errors first."
    exit 1
fi

# Step 2: Check critical files exist
echo "🔍 Checking critical files..."
required_files=(
    "src/main/java/com/eventsourcing/api/ApiModels.java"
    "src/main/java/com/eventsourcing/ai/AIResponse.java"
    "src/main/java/com/eventsourcing/ai/AIConfig.java"
    "src/main/java/com/eventsourcing/Main.java"
    "system.properties"
    "Procfile"
)

for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        echo "❌ Missing file: $file"
        exit 1
    fi
done

echo "✅ All critical files present"

# Step 3: Add all files to git
echo "📝 Adding files to git..."
git add .

# Step 4: Check git status
echo "📊 Git status:"
git status --porcelain

# Step 5: Commit changes
if [ -n "$(git status --porcelain)" ]; then
    echo "💾 Committing changes..."
    git commit -m "fix: ensure all record classes and Heroku config are committed for deployment"
else
    echo "✅ No changes to commit"
fi

# Step 6: Clear Heroku build cache
echo "🧹 Clearing Heroku build cache..."
heroku builds:cache:purge -a ai-rpg 2>/dev/null || echo "ℹ️ Could not clear cache (app might not exist yet)"

# Step 7: Deploy to Heroku
echo "🚀 Deploying to Heroku..."
git push heroku main

echo "🎉 Deployment complete! Monitor with: heroku logs --tail -a ai-rpg"
