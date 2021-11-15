# Flume Json HBase Serializer

## Build
mvn package

## Install
Copy target/flume-jsonhbaseevent-serializer-1.0.0.jar in Flume' classpath

## Configure
hbaseagent.sinks.prices.serializer = com.marketconnect.flume.serializer.JsonHbaseEventSerializer
hbaseagent.sinks.hbase_jmsbor_jms.serializer.colName = data
hbaseagent.sinks.hbase_jmsbor_jms.serializer.colId = remote_id

It will create one column per JSON object in data with the field name remote_id which should be in each json object.