#! /bin/bash

cd `dirname $0`  # This makes everything relative to mgmt/, no matter where you run your scripts from
. defs.sh

# This script just runs does an sbt compile & sets up the eclipse project

echo -e "\n\n ${Green}REFRESHING foundation${Color_Off}\n"
cd ../foundation
sbt compile
sbt eclipse

echo -e "\n\n ${Green}REFRESHING coreEngine${Color_Off}\n"
cd ../coreEngine
sbt compile
sbt eclipse

echo -e "\n\n ${Green}REFRESHING library${Color_Off}\n"
cd ../library
sbt compile
sbt eclipse
