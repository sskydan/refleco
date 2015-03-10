#!/bin/bash
if [ ! -d "elasticsearch" ]; then
	wget https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.4.zip -O elasticsearch.zip
	unzip elasticsearch.zip -d elasticsearch
fi

