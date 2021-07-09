#!/bin/bash

DATASETS=(WKc WKt WKw WKi)
BEAM_WIDTHS=(3)
# EVAL_METRICS=(δ τ h)
EVAL_METRICS=(h δ)

for metric in "${EVAL_METRICS[@]}"
do
    for dataset in "${DATASETS[@]}"
    do
        for beam in "${BEAM_WIDTHS[@]}"
        do
            echo "$dataset" "$beam" "$metric"
            java -Xmx12g -jar TestOrigin.jar "$beam" "$metric" "$dataset"
        done
    done
done