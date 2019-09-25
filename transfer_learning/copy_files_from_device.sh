#!/bin/bash

echo "Copying files..."

adb shell 'ls /sdcard/training_data/*.jpg' | tr -d '\r' | sed -e 's/^\///' | xargs -n1 adb pull