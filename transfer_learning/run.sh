#!/bin/bash

python retrain.py \
  --image_dir ./input \
  --how_many_training_steps 1000 \
  --output_graph ./output/output_graph.pb \
  --output_labels ./output/output_labels.txt \
  --saved_model_dir=./output/saved_models/$(date +%s) \
  --tfhub_module https://tfhub.dev/google/imagenet/mobilenet_v1_100_224/quantops/feature_vector/3
