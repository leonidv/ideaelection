#!/usr/bin/env bash

sudo df -h
sudo service docker stop
sudo umount -f /var/lib/docker
sudo rm -rf ~/.phpbrew ~/.kerl ~/.sbt ~/.nvm ~/.npm ~/.kiex /usr/local/golang/ /opt/*
sudo df -h
