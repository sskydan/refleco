#!/bin/bash

# This script will download & extract elasticsearch  with appropriate plugins (if needed)
# Then it will launch a 5 node local cluster, with cluster name 'refleco-dev'
# You will get a nice monitoring site at localhost:9200/_plugin/kopf
# You can monitor the development cluster on your local machine, and do es queries on it
# By default, we our cluster name is $COMPANY-dev
# This is for development environment only

cd `dirname $0`  # This makes everything relative to mgmt/, no matter where you run your scripts from
. defs.sh

if [ ! -d "elasticsearch" ]; then
	echo -e "\n\n ${Green}FETCHING elasticsearch${Color_Off}\n"
	wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.4.zip -O elasticsearch.zip
	unzip elasticsearch.zip
	rm elasticsearch.zip
	mv elasticsearch-1.4.4 elasticsearch
	./elasticsearch/bin/plugin --install lmenezes/elasticsearch-kopf/master
fi

# We need edit our es config file
# We append because we don't want to remove any potentially important configs
echo cluster.name: orbit >> elasticsearch/config/elasticsearch.yml
echo index.number_of_shards: 5 >> elasticsearch/config/elasticsearch.yml

echo -e "\n\n ${Green}DISPATCHING elasticsearch${Color_Off}\n"
cd elasticsearch
./bin/elasticsearch
