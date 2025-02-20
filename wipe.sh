#!/bin/bash

# Check if solution name is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <solution_name>"
    exit 1
fi

SOLUTION_NAME=$1
BINARY_PATH="solutions/${SOLUTION_NAME}"
SOURCE_PATH="source/${SOLUTION_NAME}.cpp"
RESULTS_PATH="results/${SOLUTION_NAME}.csv"

# Remove binary if it exists
if [ -f "$BINARY_PATH" ]; then
    echo "Removing binary: $BINARY_PATH"
    rm "$BINARY_PATH"
fi

# Remove source if it exists
if [ -f "$SOURCE_PATH" ]; then
    echo "Removing source: $SOURCE_PATH"
    rm "$SOURCE_PATH"
fi

# Remove results if they exist
if [ -f "$RESULTS_PATH" ]; then
    echo "Removing results: $RESULTS_PATH"
    rm "$RESULTS_PATH"
fi

echo "Cleanup complete for $SOLUTION_NAME" 