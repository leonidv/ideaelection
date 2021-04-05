#!/usr/bin/env bash

START_DIR=$(pwd)

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "GIT_ROOT=${HOME}/${SEMAPHORE_GIT_DIR}"

cd ${DIR}/../backend
./gradlew clean bootJar && java -Dtestmode=on -jar build/libs/idel-backend.jar

cd ${START_DIR}