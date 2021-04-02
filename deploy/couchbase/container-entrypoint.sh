#!/usr/bin/env bash
set -Eeo pipefail
set -m # return couchbase to foreground after initialization

# TODO add "-u"

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
  local var="$1"
  local fileVar="${var}_FILE"
  local def="${2:-}"

  if [ "${!var:-}" ] && [ "${!fileVar:-}" ]; then
    echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
    exit 1
  fi
  local val="$def"
  if [ "${!var:-}" ]; then
    val="${!var}"
  elif [ "${!fileVar:-}" ]; then
    val="$(<"${!fileVar}")"
  fi
  export "$var"="$val"
  unset "$fileVar"
}

# https://blog.couchbase.com/using-docker-develop-couchbase/
check_cb_is_stared() {
  curl -u "${CB_ADMIN}:${CB_PASSWORD}" --silent http://localhost:8091/pools >/dev/null
  echo $?
}

wait_cb_is_stared() {
  until [ $(check_cb_is_stared) = 0 ]; do
    echo >&2 "Waiting for Couchbase Server to be available"
    sleep 1
  done
}

#
# init variable CB_IS_INIT to "yes" if it was inited of "no" otherwise.
#
check_couchbase_is_init() {
  local node_uuid=$(curl -u "${CB_ADMIN}:${CB_PASSWORD}" http://localhost:8091/pools | grep -E '"uuid":"(\w|\d)')
  if [[ -z $node_uuid ]]; then
    export CB_IS_INIT="no"
  else
    export CB_IS_INIT="yes"
  fi
}

########################################################################################################################
echo "##############################################################################################################"
file_env "CB_ADMIN" "admin"
file_env "CB_PASSWORD" "password"

/entrypoint.sh couchbase-server &

wait_cb_is_stared

check_couchbase_is_init

case $CB_IS_INIT in
"yes") echo "couchbase already initialized" ;;
"no") source initialization/init.sh ;;
esac

fg 1
