#!/bin/bash

echo "Copying model files to app code..."

cp output/model_quantized.tflite ../app/assets/model_quantized.tflite
cp output/output_labels.txt ../app/assets/output_labels.txt
