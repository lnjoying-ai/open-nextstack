#!/bin/bash

HOME_OPT="$BASE_OPT -Dlog4j.configurationFile=$LOG_CFG_HOME/network_log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -server -XX:CICompilerCount=2 -XX:+UseG1GC -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -Xmx1g -Xms500m -Xmn400m -Xss256k -XX:+ExplicitGCInvokesConcurrent -XX:MaxDirectMemorySize=256m"
if [ x$JUSTICE_DEBUG == xtrue ];then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
fi

NETWORK_RESOURCE_OPTS="$RESOURCE_OPTS"
if [ $NETWORK_REST_PORT ];then
  NETWORK_RESOURCE_OPTS="$NETWORK_RESOURCE_OPTS -Dservicecomb.rest.address=0.0.0.0:$NETWORK_REST_PORT"
fi
if [ $NETWORK_HIGHWAY_PORT ];then
  NETWORK_RESOURCE_OPTS="$NETWORK_RESOURCE_OPTS -Dservicecomb.highway.address=0.0.0.0:$NETWORK_HIGHWAY_PORT"
fi

#echo "******-----   start NETWORK      ----*******"
#java $HOME_OPT $JAVA_OPTS -jar ${BIN_HOME}/com.justice.network.jar  1> /dev/null 2> ${LOG_OUT}/network.out  &

echo "******-----   start network      ----*******"
java $HOME_OPT $JAVA_OPTS $NETWORK_RESOURCE_OPTS -jar ${BIN_HOME}/com.lnjoying.justice.network.jar  1> /dev/null 2> ${LOG_OUT}/network.out  &