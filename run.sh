#!/bin/bash

# Check if solution name is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <solution_name>"
    exit 1
fi

SOLUTION_NAME=$1
BINARY_PATH="solutions/${SOLUTION_NAME}"
SOURCE_PATH="source/${SOLUTION_NAME}.cpp"

# First check for source file
if [ -f "$SOURCE_PATH" ]; then
    echo "Source file found. Compiling: $SOURCE_PATH"
    g++ -O2 -std=c++17 "$SOURCE_PATH" -o "${BINARY_PATH}"
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
    echo "Compilation successful!"
else
    # No source file, check if binary exists
    if [ ! -f "${BINARY_PATH}" ]; then
        echo "Error: Neither source file nor binary found for $SOLUTION_NAME"
        exit 1
    fi
    echo "No source file found. Using existing binary."
fi

# Run the solution
java -cp out/production/Eliud Runner "${SOLUTION_NAME}"
