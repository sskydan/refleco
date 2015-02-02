#!/bin/bash

### script to be used on deploy server (aws) to update the django app
sudo rm -rf /var/www/reflecho.com/djangoServer
sudo cp -r ../djangoServer /var/www/reflecho.com/

sudo chown -R apache:apache /var/www/reflecho.com/djangoServer
sudo chmod -R 755 /var/www/reflecho.com

echo "Django app updated"
