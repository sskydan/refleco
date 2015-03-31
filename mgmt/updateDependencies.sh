#!/bin/bash

shopt -s globstar

### Build foundation project & update its dependencies

# delete old jar dependencies
rm ../library/lib/foundation*
rm ../coreEngine/lib/foundation*
rm ../ukrm/lib/foundation*

# backup current jar
cd "../foundation/target/scala-2.11/"
mkdir "bkp"
mv *.jar bkp/

# build new version & copy it upstream
cd "../../" # /reflecho/foundation
sbt package
cd -
cp *.jar "../../../library/lib/"
cp *.jar "../../../coreEngine/lib/"
cp *.jar "../../../ukrm/lib/"

# update eclipse project structure
cd "../../../" # /reflecho
cd library
sbt eclipse
cd ../coreEngine
sbt eclipse
cd ../ukrm
sbt eclipse


echo "Project foundation built and deployed"

