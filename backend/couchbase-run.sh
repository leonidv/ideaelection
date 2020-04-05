#!/usr/bin/env bash

CONTAINER_ID=$(docker ps -a | grep couchbase | head -c 12)

if [[ -z "$CONTAINER_ID" ]]
then
    docker run -t --name db -p 8091-8094:8091-8094 -p 11210:11210 couchbase/server-sandbox:6.5.0
else
    docker start $CONTAINER_ID
fi

