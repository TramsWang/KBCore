#!/bin/bash

FAMILIES=(2 4 6 8 10 12 14 16 18 20)
MODELS=(basic Cr)

export SWI_HOME_DIR=/home/trams/lib/swipl
export LD_LIBRARY_PATH=/home/trams/lib/swipl/lib/x86_64-linux
export LD_PRELOAD=/home/trams/lib/swipl/lib/x86_64-linux/libswipl.so

for families in "${FAMILIES[@]}"
do
    for model in "${MODELS[@]}"
    do
        echo "$families" "$model"
        java -Xmx12g -jar TestSpeedUp.jar "$model" "$families"
    done
done