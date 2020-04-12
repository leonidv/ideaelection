#!/usr/bin/env bash

# https://stackoverflow.com/a/246128/224222
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

export INSOMNIA_DATA_PATH=${DIR}"/insomnia"
insomnia