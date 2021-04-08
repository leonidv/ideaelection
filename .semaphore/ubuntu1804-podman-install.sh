#!/usr/bin/env bash
source /etc/os-release

PODMAN_REPO="https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_18.04/"
echo "deb $PODMAN_REPO  / " | sudo tee -a /etc/apt/sources.list.d/podman.list > /dev/null
curl -L $PODMAN_REPO/Release.key | sudo apt-key add -
sudo apt-get -qq update
sudo apt-get -qq install podman -y
