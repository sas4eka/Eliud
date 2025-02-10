if [ -z "$1" ]; then
  echo "Usage: ./run.sh <solutionFilename>"
  exit 1
fi

java -cp out/production/Eliud Runner $1
