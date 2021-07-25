#!/usr/bin/env bash

TAG=${1:-"latest"}

podman run -d --quiet \
  --name idel-backend \
  --network=container:idel-couchbase \
  --replace=true \
  docker.io/leonidv/idel-backend:"${TAG}"
