#!/usr/bin/env bash

TAG=${1:-"latest"}

podman run -d --quiet \
  --name idel-backend \
  --replace=true \
  --network="host" \
  docker.io/leonidv/idel-backend:"${TAG}"
