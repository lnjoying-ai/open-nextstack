#!/bin/bash

HOME_OPT="$BASE_OPT -Dlog4j.configurationFile=$LOG_CFG_HOME/ims_log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -server -XX:CICompilerCount=2 -XX:+UseG1GC -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -Xmx1g -Xms500m -Xmn400m -Xss256k -XX:+ExplicitGCInvokesConcurrent -XX:MaxDirectMemorySize=256m"
IMS_RESOURCE_OPTS="$RESOURCE_OPTS"
if [ $IMS_REST_PORT ];then
  IMS_RESOURCE_OPTS="$IMS_RESOURCE_OPTS -Dservicecomb.rest.address=0.0.0.0:$IMS_REST_PORT"
fi
if [ $IMS_HIGHWAY_PORT ];then
  IMS_RESOURCE_OPTS="$IMS_RESOURCE_OPTS -Dservicecomb.highway.address=0.0.0.0:$IMS_HIGHWAY_PORT"
fi
if [ $HARBOR_REGISTRY_INSTALL_PATH ];then
  IMS_RESOURCE_OPTS="$IMS_RESOURCE_OPTS -DHARBOR_REGISTRY_INSTALL_PATH=$HARBOR_REGISTRY_INSTALL_PATH"
fi

if [ x$JUSTICE_DEBUG == xtrue ];then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006"
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
fi

echo "******-----    start IMS    ----*******"

java $HOME_OPT $JAVA_OPTS $IMS_RESOURCE_OPTS -jar ${BIN_HOME}/com.lnjoying.justice.ims.jar  1 > /dev/null 2> ${LOG_OUT}/ims.out  &
sleep 2