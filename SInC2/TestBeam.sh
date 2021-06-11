#!/bin/bash

DATASETS=(D DBf DBl Fs Fm S)
# DATASETS=(E)
BEAM_WIDTHS=(1 2 3 4 5 6 7 8 9 10)
EVAL_METRICS=(δ τ h)

export SWI_HOME_DIR=/home/trams/lib/swipl
export LD_LIBRARY_PATH=/home/trams/lib/swipl/lib/x86_64-linux
export LD_PRELOAD=/home/trams/lib/swipl/lib/x86_64-linux/libswipl.so

for dataset in "${DATASETS[@]}"
do
    for beam in "${BEAM_WIDTHS[@]}"
    do
        for metric in "${EVAL_METRICS[@]}"
        do
            echo "$dataset" "$beam" "$metric"
            java -jar TestBeam.jar "$beam" "$metric" "$dataset"
        done
    done
done