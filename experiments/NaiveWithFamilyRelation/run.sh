#!/bin/bash
export SWI_HOME_DIR=/home/trams/lib/swipl
export LD_LIBRARY_PATH=/home/trams/lib/swipl/lib/x86_64-linux
export CLASSPATH=/home/trams/lib/swipl/lib
export LD_PRELOAD=/home/trams/lib/swipl/lib/x86_64-linux/libswipl.so

for DUPLICATE in $(seq 1 1 1)
do
    echo DUPLICATE: $DUPLICATE
    for ERROR_RATE in $(seq 0.05 0.05 0.05)
    do
        for ((FAMILY_CNT=100; FAMILY_CNT<=100; FAMILY_CNT*=10 ))
        do
            # java -jar ExpNaive.jar $ERROR_RATE $FAMILY_CNT DUP_$DUPLICATE Simple
            # java -jar ExpNaive.jar $ERROR_RATE $FAMILY_CNT DUP_$DUPLICATE Medium
            java -jar ExpNaive-Amie.jar $ERROR_RATE $FAMILY_CNT DUP_$DUPLICATE Simple
            java -jar ExpNaive-Amie.jar $ERROR_RATE $FAMILY_CNT DUP_$DUPLICATE Medium
        done
    done
done