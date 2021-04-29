#!/usr/bin/env bash
podman pod create --name saedi-testmode -p 8091-8094:8091-8094 -p 11210:11210 -p 8080:8080
podman run -d --name saedi-couchbase --pod saedi-testmode docker.io/leonidv/idel-couchbase
podman run -d --name saedi-backend --pod saedi-testmode docker.io/leonidv/idel-backend-testmode