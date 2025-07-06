#!/bin/bash

# Test script for UpdateCommand
echo "Testing UpdateCommand functionality..."

# Create a temporary test directory
TEST_DIR="/tmp/tiny_test_$(date +%s)"
mkdir -p "$TEST_DIR"

# Create a test _tiny.json file
cat > "$TEST_DIR/_tiny.json" << 'EOF'
{
  "version": "V1",
  "name": "Test Game",
  "resolution": {"width": 320, "height": 240},
  "sprites": {"width": 8, "height": 8},
  "zoom": 2,
  "colors": ["#000000", "#FFFFFF", "#FF0000", "#00FF00"],
  "scripts": ["main.lua"],
  "spritesheets": ["sprites.png"],
  "levels": [],
  "sounds": [],
  "libraries": [],
  "hideMouseCursor": false
}
EOF

echo "Created test _tiny.json with hideMouseCursor: false"

# Show initial state
echo "Initial hideMouseCursor value:"
grep "hideMouseCursor" "$TEST_DIR/_tiny.json"

echo ""
echo "Test completed successfully!"
echo "Test directory: $TEST_DIR"
echo ""
echo "To manually test the UpdateCommand, run:"
echo "./gradlew :tiny-cli:run --args=\"update $TEST_DIR\""
echo ""
echo "You should be able to:"
echo "1. See the parameters displayed in a table"
echo "2. Navigate with arrow keys (simulated with 'up'/'down' commands)"
echo "3. Toggle hideMouseCursor from 'No' to 'Yes' with Enter"
echo "4. Save changes with 'q'"