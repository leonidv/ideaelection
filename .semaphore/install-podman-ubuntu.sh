#!/usr/bin/env bash
source /etc/os-release
OS=xUbuntu_20.04
VERSION=1.21.2
PODMAN_REPO="https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_20.04/"
CONTAINERS_PATH=/mnt/containers/storage



echo "deb https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/$OS/ /" > /etc/apt/sources.list.d/devel:kubic:libcontainers:stable.list
echo "deb https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable:/cri-o:/$VERSION/$OS/ /" > /etc/apt/sources.list.d/devel:kubic:libcontainers:stable:cri-o:$VERSION.list

curl -L https://download.opensuse.org/repositories/devel:kubic:libcontainers:stable:cri-o:$VERSION/$OS/Release.key | apt-key add -
curl -L https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/$OS/Release.key | apt-key add -

apt-get update
apt-get install podman cri-o cri-o-runc -y

#apt-get install cri-o cri-o-runc podman podman-plugins -y
#sudo apt-get install podman podman-plugins containernetworking-plugins -y
#
#echo "change path of containers storage"
#mkdir -p "${CONTAINERS_PATH}"
#chown -Rv semaphore:semaphore "${CONTAINERS_PATH}"

#mkdir -p $HOME/.config/containers/
#echo "[storage]" > $HOME/.config/containers/storage.conf
#echo "driver = \"overlay\"" >> $HOME/.config/containers/storage.conf
#echo "rootless_storage_path=\"${CONTAINERS_PATH}\"" >> $HOME/.config/containers/storage.conf
#cat $HOME/.config/containers/storage.conf

#sudo sed -i -E "s/^graphroot.*?/graphroot = \"\/mnt\/containers\/storage\"/g" /etc/containers/storage.conf
#sudo sed -i -E "s/^mountopt.*?/graphroot = \"nodev,metacopy=off\"/g" /etc/containers/storage.conf
#grep "graphroot" /etc/containers/storage.conf
#grep "mountopt" /etc/containers/storage.conf


