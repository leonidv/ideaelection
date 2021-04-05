#!/usr/bin/env bash

MODE=${1:-testmode}


#case $MODE in
#"testmode") PODMAN_ADVANCED_SETTINGS="--env LOG_DIR=/idel/logs" ;;
#"dev") source initialization/init.sh ;;
#esac

podman run -d \
  --name idel-backend-${MODE} \
  --network=container:idel-couchbase \
  --replace=true \
  leonidv/idel-backend-${MODE}
