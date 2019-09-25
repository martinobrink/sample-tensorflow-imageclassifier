#!/bin/bash

echo "Doing transfer learning..."

docker build -t kumuluzz/tensorflow-image-retraining .
docker run \
  --rm \
  -it \
  -v ${PWD}/output:/notebooks/output \
  -v ${PWD}/input:/notebooks/input \
  kumuluzz/tensorflow-image-retraining
