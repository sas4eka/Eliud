#!/bin/bash

# Check if solution name is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <solution_name>"
    exit 1
fi

SOLUTION_NAME=$1
BINARY_PATH="solutions/${SOLUTION_NAME}"
SOURCE_PATH="source/${SOLUTION_NAME}.cpp"

# Check if binary exists
if [ ! -f "${BINARY_PATH}" ]; then
    # Binary not found, check for source file
    if [ -f "$SOURCE_PATH" ]; then
        echo "Binary not found. Compiling from source: $SOURCE_PATH"
        g++-14 -O2 -std=c++17 "$SOURCE_PATH" -o "${BINARY_PATH}"
        if [ $? -ne 0 ]; then
            echo "Compilation failed!"
            exit 1
        fi
        echo "Compilation successful!"
    else
        echo "Error: Neither binary nor source file found for $SOLUTION_NAME"
        exit 1
    fi
fi

# Run the solution
java -cp out/production/Eliud Runner "${SOLUTION_NAME}"
