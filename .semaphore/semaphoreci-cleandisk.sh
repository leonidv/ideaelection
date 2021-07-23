#!/usr/bin/env bash

sudo df -h
sudo service docker stop
sudo umount -f /var/lib/docker
sudo df -h
