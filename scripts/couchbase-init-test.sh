#!/usr/bin/env bash
CB_HOST=localhost
## Set controller name
curl -u Administrator:password http://${CB_HOST}:8091/node/controller/rename \
        --data hostname=127.0.0.1 \
&& echo 'Name is 127.0.0.1'

curl -u Administrator:password http://${CB_HOST}:8091/node/controller/setupServices \
        --data 'services=kv,n1ql,index,fts' \
&& echo 'Services: kv, n1ql, index, fts'

curl -u Administrator:password http://${CB_HOST}:8091/pools/default  \
        --data memoryQuota=256 \
        --data indexMemoryQuota=256 \
        --data ftsMemoryQuota=256 \
&& echo 'Quotas: 256mb to all'

#Create administrator user with default name and password
curl -u Administrator:password http://${CB_HOST}:8091/settings/web  \
        --data username=Administrator \
        --data password=password \
        --data port=8091 \
&& echo 'Default admin user'


curl -u Administrator:password http://${CB_HOST}:8091/settings/indexes   \
        --data storageMode=forestdb \
&& echo 'Index forestdb enabled'

curl -u Administrator:password http://${CB_HOST}:8091/pools/default/buckets  \
        --data replicaNumber=0 \
        --data name=ideaelection \
        --data ramQuotaMB=128 \
&& echo 'Default bucket ideaelection is created'

sleep 3

curl -G -XPOST -u Administrator:password http://${CB_HOST}:8093/query/service \
        --data-urlencode statement='CREATE INDEX `ideas` ON `ideaelection`(`_type`)' \
&& echo 'Index on type created'

curl -G -XPOST -u Administrator:password http://${CB_HOST}:8093/query/service \
        --data-urlencode statement='CREATE INDEX `ideas-by-ctime` ON `ideaelection`(`_type`,`ctime`)' \
&& echo 'Index on type and ctime created'

curl -u Administrator:password -XPUT http://${CB_HOST}:8094/api/index/idea_fts \
        -H 'cache-control: no-cache' \
        -H 'content-type: application/json' \
        -d @couchbase-test-fts-mapping.json \
&& echo 'FTS is created'