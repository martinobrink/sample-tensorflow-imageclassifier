#!/bin/bash

echo "Copying model files to app code..."

cp output/model.tflite ../app/assets/model.tflite
cp output/output_labels.txt ../app/assets/output_labels.txt
