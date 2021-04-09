#!/usr/bin/env bash

APP_JAR="../backend/build/libs/idel-backend.jar"
MODE=${1:-dev}
echo "mode is ${MODE}"


if [[ ! -f "$APP_JAR" ]]
then
  echo "can't read application jar"
  echo "APP_JAR=$APP_JAR"
  exit 1
fi

podman build --quiet -t docker.io/leonidv/idel-backend-${MODE} -f backend/Containerfile \
  --build-arg PROJECT_DIR=backend \
  --build-arg SCRIPTS_DIR=deploy/backend/${MODE}/ \
  ../
