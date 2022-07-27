#!/usr/bin/env bash

CONTAINER_NAME=saedi-smtp-fake
CONTAINER_ID=$(podman ps -a -q --filter name=$CONTAINER_NAME)

if [[ -z "${CONTAINER_ID}" ]]; then
  podman run \
   --quiet \
   --detach \
   --name $CONTAINER_NAME \
   -p 5080:5080 \
   -p 5025:5025 \
   -e fakesmtp.authentication.username=fakesmtp \
   -e fakesmtp.authentication.password=719e7e8cbe7c \
   docker.io/gessnerfl/fake-smtp-server \
else
  podman start $CONTAINER_NAME
fi
