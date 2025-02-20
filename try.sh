#!/bin/bash

# Check if solution name is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <solution_name>"
    exit 1
fi

SOLUTION_NAME=$1
SOURCE_PATH="source/${SOLUTION_NAME}.cpp"

# Copy main.cpp to the solutions directory
echo "Copying main.cpp to ${SOURCE_PATH}"
cp main.cpp "${SOURCE_PATH}"

# Run the solution using run.sh
./run.sh "${SOLUTION_NAME}" 