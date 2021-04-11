#!/usr/bin/env bash
source /etc/os-release

PODMAN_REPO="https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_18.04/"
CONTAINERS_PATH=/mnt/containers/storage

echo "deb $PODMAN_REPO  / " | sudo tee -a /etc/apt/sources.list.d/podman.list > /dev/null
curl -L $PODMAN_REPO/Release.key | sudo apt-key add -
sudo apt-get -qq update
sudo apt-get -qq install podman -y

echo "change path of containers storage"
sudo mkdir -p "${CONTAINERS_PATH}"
sudo chown -Rv semaphore:semaphore "${CONTAINERS_PATH}"

#mkdir -p $HOME/.config/containers/
#echo "[storage]" > $HOME/.config/containers/storage.conf
#echo "driver = \"overlay\"" >> $HOME/.config/containers/storage.conf
#echo "rootless_storage_path=\"${CONTAINERS_PATH}\"" >> $HOME/.config/containers/storage.conf
#cat $HOME/.config/containers/storage.conf

sudo sed -i -E "s/^graphroot.*?/graphroot = \"\/mnt\/containers\/storage\"/g" /etc/containers/storage.conf
grep "graphroot" /etc/containers/storage.conf

