#!/usr/bin/env bash
set -x

if [[ -x "$(command -v podman)" ]]; then
 CONTAINER_MANAGER="podman"
else
 echo "Docker is used. Please consider using podman instead of docker (https://podman.io)."
 CONTAINER_MANAGER="docker"
fi



CONTAINER_ID=$(${CONTAINER_MANAGER} ps -a -q --filter name=idel-couchbase)


if [[ -z "${CONTAINER_ID}" ]]
then
    ${CONTAINER_MANAGER} run -p 8091-8094:8091-8094 -p 11210:11210 --name idel-couchbase  idel-couchbase
else
    ${CONTAINER_MANAGER} start idel-couchbase
fi
