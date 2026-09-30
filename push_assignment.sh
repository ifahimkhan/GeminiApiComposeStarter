#!/bin/bash
# ============================================================
# PUSH SCRIPT for N161_Aarya - Assignment 1 Submission
# Run this AFTER you have forked the professor's repo
# ============================================================

echo "🚀 Pushing N161_Aarya branch to your GitHub fork..."
echo ""

# Push the submission branch to your fork
git push -u origin N161_Aarya

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Push successful!"
    echo ""
    echo "📌 Now open this URL to create your Pull Request:"
    echo "   https://github.com/Aarya200518/GeminiApiComposeStarter/pull/new/N161_Aarya"
    echo ""
    echo "👉 In the PR, set the base repository to:"
    echo "   ifahimkhan/GeminiApiComposeStarter  ←  base: master"
    echo "   Aarya200518/GeminiApiComposeStarter  ←  compare: N161_Aarya"
else
    echo ""
    echo "❌ Push failed. Make sure you have:"
    echo "   1. Forked https://github.com/ifahimkhan/GeminiApiComposeStarter to your account"
    echo "   2. Correct GitHub credentials / Personal Access Token"
fi
