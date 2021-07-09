#!/bin/bash

DATASETS=(E D DBf Fs Fm S)
# DATASETS=(S)
EVAL_METRICS=(δ τ h)
# EVAL_METRICS=(δ τ)

for dataset in "${DATASETS[@]}"
do
    for metric in "${EVAL_METRICS[@]}"
    do
        echo "$dataset" "$beam" "$metric"
        java -Xmx12g -jar TestOrigin.jar 5 "$metric" "$dataset"
    done
done