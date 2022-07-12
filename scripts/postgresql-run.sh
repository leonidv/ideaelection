#!/usr/bin/env bash

CONTAINER_ID=$(podman ps -a -q --filter name=saedi-postgresql)

if [[ -z "${CONTAINER_ID}" ]]; then
  podman run \
   --quiet \
   --detach \
   --name saedi-postgresql \
   -p 5432:5432 \
   -e POSTGRES_USER=admin \
   -e POSTGRES_PASSWORD=password \
   -e POSTGRES_DB=saedi \
   docker.io/postgres:14.1 \
   -h '*'
else
  podman start saedi-postgresql
fi

