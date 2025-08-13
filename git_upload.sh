#!/bin/bash

echo "🏴‍☠️ Treasure Hunt Adventure - Git Upload Script"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $2 -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
    else
        echo -e "${RED}❌ $1${NC}"
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo -e "${RED}❌ Git is not installed. Please install Git first.${NC}"
    exit 1
fi

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo -e "${RED}❌ Error: gradlew not found. Please run this script from the project root directory.${NC}"
    exit 1
fi

echo "🔍 Preparing project for Git upload..."
echo ""

# 1. Initialize git repository if not already initialized
if [ ! -d ".git" ]; then
    print_info "Initializing Git repository..."
    git init
    print_status "Git repository initialized" $?
else
    print_info "Git repository already exists"
fi

# 2. Check if .gitignore exists and is properly configured
if [ -f ".gitignore" ]; then
    print_status ".gitignore file exists" 0
else
    print_warning ".gitignore file missing - creating basic one"
    cat > .gitignore << 'EOF'
# Built application files
*.apk
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
release/

# Gradle files
.gradle/
build/

# Local configuration file (sdk path, etc)
local.properties

# Android Studio
.idea/
*.iml

# OS-specific files
.DS_Store
Thumbs.db

# Keystore files
*.jks
*.keystore

# External native build folder
.externalNativeBuild
.cxx/
EOF
fi

# 3. Add all files to git
print_info "Adding files to Git..."
git add .
print_status "Files added to Git staging area" $?

# 4. Check git status
echo ""
print_info "Git status:"
git status --short

# 5. Create initial commit
echo ""
print_info "Creating initial commit..."
git commit -m "🏴‍☠️ Initial commit: AI-Powered Treasure Hunt Adventure

✨ Features:
- AI-powered personalized challenge system
- Real-time behavior analytics and ML-based personalization
- Adaptive difficulty adjustment based on player performance
- Modern Material Design 3 UI with smooth animations
- Comprehensive Firebase integration
- TensorFlow Lite ML models for challenge generation
- Real-time progress tracking with milestone detection
- Social features and multiplayer challenges

🤖 AI Components:
- PlayerBehaviorAnalyzer: 8 categories of behavior tracking
- AIChallengeGenerator: ML-powered challenge creation
- ChallengeProgressManager: Real-time progress tracking
- PersonalizedChallenge: Rich challenge model with AI metadata

📱 UI Components:
- PersonalizedChallengeActivity: Main challenge interface
- ChallengeDetailsActivity: Detailed challenge view
- Advanced RecyclerView adapters with animations
- Material Design 3 components and theming

🧪 Testing:
- Comprehensive unit tests for all components
- Integration tests for Firebase and ML integration
- UI tests for user interactions

📚 Documentation:
- Complete Firebase setup guide with security rules
- Deployment guide with build configuration
- Project verification and upload scripts

🚀 Ready for production deployment!"

commit_result=$?
print_status "Initial commit created" $commit_result

# 6. Show commit information
if [ $commit_result -eq 0 ]; then
    echo ""
    print_info "Commit details:"
    git log --oneline -1
    
    echo ""
    print_info "Files in repository:"
    git ls-files | head -20
    if [ $(git ls-files | wc -l) -gt 20 ]; then
        echo "... and $(( $(git ls-files | wc -l) - 20 )) more files"
    fi
fi

# 7. Instructions for GitHub upload
echo ""
echo "🚀 Ready to upload to GitHub!"
echo "============================="
echo ""
echo "To upload this project to GitHub, follow these steps:"
echo ""
echo "1. Create a new repository on GitHub:"
echo "   - Go to https://github.com/new"
echo "   - Repository name: treasure-hunt-adventure"
echo "   - Description: AI-Powered Treasure Hunt Adventure with personalized challenges"
echo "   - Make it public or private as desired"
echo "   - DO NOT initialize with README (we already have one)"
echo ""
echo "2. Add GitHub remote and push:"
echo "   git remote add origin https://github.com/YOUR_USERNAME/treasure-hunt-adventure.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "3. Alternative: Use GitHub CLI (if installed):"
echo "   gh repo create treasure-hunt-adventure --public --source=. --remote=origin --push"
echo ""
echo "4. After uploading, don't forget to:"
echo "   - Add your google-services.json file (locally, not to Git)"
echo "   - Configure Firebase project with the provided rules"
echo "   - Set up API keys in local.properties"
echo "   - Create releases and tags for version management"
echo ""

# 8. Project statistics
echo "📊 Project Upload Summary:"
echo "=========================="
total_files=$(git ls-files | wc -l)
java_files=$(git ls-files | grep -c '\.java$')
xml_files=$(git ls-files | grep -c '\.xml$')
md_files=$(git ls-files | grep -c '\.md$')

print_info "Total files: $total_files"
print_info "Java files: $java_files"
print_info "XML files: $xml_files"
print_info "Documentation files: $md_files"

# Calculate repository size
repo_size=$(du -sh .git 2>/dev/null | cut -f1)
if [ ! -z "$repo_size" ]; then
    print_info "Repository size: $repo_size"
fi

echo ""
echo "🎯 What's included in this upload:"
echo "=================================="
echo "✅ Complete AI-powered challenge system"
echo "✅ Modern Material Design 3 UI"
echo "✅ Comprehensive test suite"
echo "✅ Firebase integration with security rules"
echo "✅ TensorFlow Lite ML model support"
echo "✅ Real-time behavior analytics"
echo "✅ Adaptive difficulty system"
echo "✅ Social and multiplayer features"
echo "✅ Complete documentation and guides"
echo "✅ Build and deployment scripts"

echo ""
echo -e "${GREEN}🏴‍☠️ Project is ready for GitHub! Set sail for the digital seas! ⚓${NC}"
echo ""
echo "Remember to:"
echo "- ⭐ Star the repository if you like it"
echo "- 📝 Update the README with your specific setup"
echo "- 🔒 Keep your API keys and google-services.json secure"
echo "- 🚀 Deploy and share your treasure hunting adventure!"
