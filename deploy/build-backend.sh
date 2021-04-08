#!/usr/bin/env bash

MODE=${1:-dev}
echo "mode is ${MODE}"

podman build -t docker.io/leonidv/idel-backend-${MODE} -f backend/Containerfile \
  --build-arg PROJECT_DIR=backend \
  --build-arg SCRIPTS_DIR=deploy/backend/${MODE}/ \
  ../
