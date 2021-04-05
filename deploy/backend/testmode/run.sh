#!/usr/bin/env bash

cd /app
java -Dtestmode=on -Dlogging.config=logback.xml -jar idel-backend.jar