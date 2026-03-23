#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080/api"

echo "=== Creating tags ==="

echo "-> Creating 'clojure' tag..."
curl -s -X POST "$BASE_URL/tag" \
  -H "Content-Type: application/json" \
  -d '{"slug":"clojure","name":"Clojure","color":"#5881D8"}' | jq .

echo "-> Creating 'nodejs' tag..."
curl -s -X POST "$BASE_URL/tag" \
  -H "Content-Type: application/json" \
  -d '{"slug":"nodejs","name":"Node.js","color":"#68A063"}' | jq .

echo ""
echo "=== Creating posts ==="

echo "-> Creating post with 'clojure' tag..."
curl -s -X POST "$BASE_URL/post" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started with Clojure",
    "content": "Clojure is a dynamic, functional Lisp dialect running on the JVM.",
    "tags": ["clojure"]
  }' | jq .

echo "-> Creating post with 'nodejs' tag..."
curl -s -X POST "$BASE_URL/post" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Building REST APIs with Node.js",
    "content": "Node.js makes it easy to build scalable network applications.",
    "tags": ["nodejs"]
  }' | jq .

echo ""
echo "=== Listing all posts ==="
curl -s "$BASE_URL/post" | jq .
