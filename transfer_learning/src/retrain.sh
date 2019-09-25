#!/bin/sh
 
python retrain.py \
  --image_dir ./input \
  --how_many_training_steps 1000 \
  --output_graph ./output/output_graph.pb \
  --output_labels ./output/output_labels.txt \
  --saved_model_dir=./output/saved_models/$(date +%s) \
  --tfhub_module https://tfhub.dev/google/imagenet/mobilenet_v1_100_224/quantops/feature_vector/3

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
