FROM tensorflow/tensorflow:1.14.0-py3

WORKDIR notebooks

RUN pip install tensorflow_hub

COPY src .

CMD ./retrain.sh
