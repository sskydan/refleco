#!/bin/bash

# delete old wars
rm ../library/target/scala-2.11/*.war
rm ../coreEngine/target/scala-2.11/*.war

# prepare library
cd ../library
sbt package
cd target/scala-2.11/
mv *.war "../../../mgmt/"

cd ../../../mgmt/

# prepare coreEngine
cd ../coreEngine
sbt package
cd target/scala-2.11/
mv *.war "../../../mgmt/"

cd ../../../mgmt/

echo "WARs built and moved to mgmt"
