#!/bin/bash

# to be used on prod environment
eval `ssh-agent -s`
ssh-add /home/ec2-user/.ssh/robot

