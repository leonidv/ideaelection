#!/usr/bin/env bash

CB_ADMIN="${CB_ADMIN:=admin}"
CB_PASSWORD="${CB_PASSWORD:=password}"
CB_HOST="${CB_HOST:=localhost}"
CB_STORAGE_ENGINE="${CB_STORAGE_ENGINE:=plasma}" #plasma for enterprise, forestdb for community

CURL_COMMON="-s --fail -u $CB_ADMIN:$CB_PASSWORD"

CALL_DIR="$(pwd)"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

checkCurlExec() {
    local curlCode=$1
    local msg=$2
    if [[ curlCode -eq 0 ]]
     then
        echo "${msg}"
     else
        echo -e "\033[31mFailed on [${msg}], exit code = ${curlCode}\e[0m"
        exit 1
    fi
}


## Set controller name
curl -s --fail http://${CB_HOST}:8091/node/controller/rename \
        --data hostname=127.0.0.1

checkCurlExec $? "Name is 127.0.0.1"

curl -s --fail http://${CB_HOST}:8091/node/controller/setupServices \
        --data "services=kv,n1ql,index,fts"

checkCurlExec $? "Services: kv, n1ql, index, fts"

curl -s --fail http://${CB_HOST}:8091/pools/default  \
        --data memoryQuota=256 \
        --data indexMemoryQuota=256 \
        --data ftsMemoryQuota=256

checkCurlExec $? "Quotas: 256mb to all"

#Create administrator user with default name and password
curl -s --fail http://${CB_HOST}:8091/settings/web  \
        --data username=$CB_ADMIN \
        --data password=$CB_PASSWORD \
        --data port=8091

checkCurlExec $? "Default admin user is $CB_ADMIN"


curl ${CURL_COMMON} http://${CB_HOST}:8091/settings/indexes   \
        --data storageMode=$CB_STORAGE_ENGINE

checkCurlExec $? "Index $CB_STORAGE_ENGINE enabled"

curl ${CURL_COMMON} http://${CB_HOST}:8091/pools/default/buckets  \
        --data replicaNumber=0 \
        --data name=ideaelection \
        --data ramQuotaMB=128

checkCurlExec $? "Default bucket ideaelection is created"

sleep 3

curl -G -XPOST ${CURL_COMMON} http://${CB_HOST}:8093/query/service \
        --data-urlencode statement='CREATE INDEX `ideas` ON `ideaelection`(`_type`)'
checkCurlExec $? "Index on type created"

curl -G -XPOST ${CURL_COMMON} http://${CB_HOST}:8093/query/service \
        --data-urlencode statement='CREATE INDEX `index_type_ctime` ON `ideaelection`(`_type`,`ctime`)'
checkCurlExec $? 'Index on type and ctime created'

curl -G -XPOST ${CURL_COMMON} http://${CB_HOST}:8093/query/service \
        --data-urlencode statement='CREATE INDEX index_id_type ON `ideaelection`(`id`,`_type`)'
checkCurlExec $? 'Index on id and type created'

curl ${CURL_COMMON} -XPUT http://${CB_HOST}:8094/api/index/idea_fts \
        -H 'cache-control: no-cache' \
        -H 'content-type: application/json' \
        -d @${SCRIPT_DIR}/idea-fts-mapping.json
checkCurlExec $? 'FTS is created'

