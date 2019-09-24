#!/bin/bash

tflite_convert \
  --output_file=./output/model_quantized.tflite \
  --graph_def_file=./output/output_graph.pb \
  --output_format=TFLITE \
  --inference_type=QUANTIZED_UINT8 \
  --input_shapes=1,224,224,3 \
  --input_arrays=Placeholder \
  --output_arrays=final_result \
  --mean_values=128 \
  --std_dev_values=127