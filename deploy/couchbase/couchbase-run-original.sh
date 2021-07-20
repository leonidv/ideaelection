#!/usr/bin/env bash
set +x

PODMAN_ARGS=${1:-""}

CONTAINER="$(head -n 1 Containerfile | cut -d' ' -f2)"

CONTAINER_ID=$(podman ps -a -q --filter name=couchbase-original)

if [[ -n "${CONTAINER_ID}" ]]; then
  echo "CLEAR CONTAINER $CONTAINER_ID"
  podman stop couchbase-original
  podman rm -fv couchbase-original
fi

echo ${CONTAINER}
podman run -d --quiet \
    --name couchbase-original \
    -p 8091-8094:8091-8094 -p 11210:11210 \
    ${CONTAINER}

#podman logs couchbase-original