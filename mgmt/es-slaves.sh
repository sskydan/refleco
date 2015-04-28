#!/bin/bash

# kick off a bunch of slaves
# number of slaves is the first param

cd `dirname $0`  # This makes everything relative to mgmt/, no matter where you run your scripts from
. defs.sh


cd elasticsearch

i=0
while [ $i -lt $1 ]; do
	./bin/elasticsearch &
	let i=i+1
done

