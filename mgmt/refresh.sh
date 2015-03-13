#! /bin/bash

# This script just runs does an sbt compile & sets up the eclipse project

green='\033[0;32m'
NC='\033[0m'

echo -e "\n\n ${green}REFRESHING foundation${NC}\n"
cd ../foundation
sbt compile
sbt eclipse

echo -e "\n\n ${green}REFRESHING coreEngine${NC}\n"
cd ../coreEngine
sbt compile
sbt eclipse

echo -e "\n\n ${green}REFRESHING library${NC}\n"
cd ../library
sbt compile
sbt eclipse
