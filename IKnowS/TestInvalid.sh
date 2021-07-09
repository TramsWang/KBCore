#!/bin/bash

# DATASETS=(E D DBf Fs Fm)
DATASETS=(S)
# BEAM_WIDTHS=(1 2 3 4 5 6 7 8 9 10)
BEAM_WIDTHS=(1 2 3 4 5)
EVAL_METRICS=(δ)

for dataset in "${DATASETS[@]}"
do
    for beam in "${BEAM_WIDTHS[@]}"
    do
        for metric in "${EVAL_METRICS[@]}"
        do
            echo "$dataset" "$beam" "$metric"
            java -Xmx12g -jar TestOrigin.jar "$beam" "$metric" "$dataset"
        done
    done
done