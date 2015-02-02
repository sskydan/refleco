#!/bin/bash

now=$(date +"%Y%m%d")
cd ../library
zip -r esIndex_$now data/
mv esIndex* ../mgmt

echo "ES Index packaged"
