#!/bin/bash
# Push script for Gatha Jain (N038)
# Repository: https://github.com/gathajain04-ui/GeminiApiComposeStarter.git
# Feature Branch: N038_GathaJain-Gemini-Chat-App-Assignment1

set -e

BRANCH="N038_GathaJain-Gemini-Chat-App-Assignment1"
REMOTE_URL="https://github.com/gathajain04-ui/GeminiApiComposeStarter.git"

echo "=========================================================="
echo " Pushing Project to GitHub"
echo " Author: Gatha Jain (Roll No: N038)"
echo " Branch: $BRANCH"
echo " Remote: $REMOTE_URL"
echo "=========================================================="

if [ -n "$1" ]; then
    # Token passed as command line argument
    TOKEN="$1"
    AUTH_URL="https://gathajain04-ui:${TOKEN}@github.com/gathajain04-ui/GeminiApiComposeStarter.git"
    git push "$AUTH_URL" "$BRANCH"
elif [ -n "$GITHUB_TOKEN" ]; then
    AUTH_URL="https://gathajain04-ui:${GITHUB_TOKEN}@github.com/gathajain04-ui/GeminiApiComposeStarter.git"
    git push "$AUTH_URL" "$BRANCH"
else
    echo "Tip: You can pass your Personal Access Token (PAT) directly:"
    echo "  ./push_to_github.sh <your_token>"
    echo ""
    git push -u origin "$BRANCH"
fi

echo ""
echo "Successfully pushed! Next step: Open Pull Request on GitHub targeting ifahimkhan/GeminiApiComposeStarter:master"
