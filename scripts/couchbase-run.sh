#!/usr/bin/env bash


APP_PORT=${1:-""}
TAG=${2:-"latest"}

if [[ -x "$(command -v podman)" ]]; then
  CONTAINER_MANAGER="podman"
else
  echo "Docker is used. Please consider using podman instead of docker (https://podman.io)."
  CONTAINER_MANAGER="docker"
fi

CONTAINER_ID=$(${CONTAINER_MANAGER} ps -a -q --filter name=idel-couchbase)

if [[ -z "${CONTAINER_ID}" ]]; then
  echo "Additional published ports: $APP_PORT"
  ${CONTAINER_MANAGER} run -d --quiet \
    --name idel-couchbase \
    -p 8091-8094:8091-8094 -p 11210:11210 $APP_PORT \
    docker.io/leonidv/idel-couchbase:$TAG

else
  ${CONTAINER_MANAGER} start idel-couchbase
fi
