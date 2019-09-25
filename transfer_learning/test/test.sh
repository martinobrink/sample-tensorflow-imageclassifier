#!/bin/sh

python label_image.py \
--graph=../output/output_graph.pb \
--labels=../output/output_labels.txt \
--input_layer=Placeholder \
--output_layer=final_result \
--input_height=224 \
--input_width=224 \
--image=./fox/image0.jpg

python label_image.py \
--graph=../output/output_graph.pb \
--labels=../output/output_labels.txt \
--input_layer=Placeholder \
--output_layer=final_result \
--input_height=224 \
--input_width=224 \
--image=./nothing/image0.jpg

python label_image.py \
--graph=../output/output_graph.pb \
--labels=../output/output_labels.txt \
--input_layer=Placeholder \
--output_layer=final_result \
--input_height=224 \
--input_width=224 \
--image=./raccoon_dog/image0.jpg