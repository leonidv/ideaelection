#!/usr/bin/env bash

if [[ -x "$(command -v podman)" ]]; then
 CONTAINER_MANAGER="podman"
else
 echo "Docker is used. Please consider using podman instead of docker (https://podman.io)."
 CONTAINER_MANAGER="docker"
fi



CONTAINER_ID=$(${CONTAINER_MANAGER} ps -a | grep "couchbase:6.6.1" | head -c 12)

if [[ -z "${CONTAINER_ID}" ]]
then
    ${CONTAINER_MANAGER} run -d --name db -p 8091-8094:8091-8094 -p 11210:11210 couchbase:6.6.1
else
    ${CONTAINER_MANAGER} start ${CONTAINER_ID}
fi

