#!/bin/bash


cum=2000
for n in $@
do
    echo screen -dmS node$n bash -c 'cd out/production/ass2 ;java Lsr '$n' '$cum' '../../../config$n.txt'; sleep 10'
    `screen -dmS node$n bash -c 'cd out/production/ass2 ;java Lsr '$n' '$cum' '../../../config$n.txt'; sleep 10'`
    echo starting "id=$n" on port $cum...
    cum=$(($cum+1))
done
