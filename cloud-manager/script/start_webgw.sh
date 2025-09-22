#!/bin/bash

HOME_OPT="$BASE_OPT -Dlog4j.configurationFile=$LOG_CFG_HOME/webgw_log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -server -XX:CICompilerCount=2 -XX:+UseG1GC -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -Xmx1g -Xms500m -Xmn400m -Xss256k -XX:+ExplicitGCInvokesConcurrent -XX:MaxDirectMemorySize=256m"
WEBGW_RESOURCE_OPTS="$RESOURCE_OPTS"
if [ $WEBGW_REST_PORT ];then
  WEBGW_RESOURCE_OPTS="$WEBGW_RESOURCE_OPTS -Dservicecomb.rest.address=0.0.0.0:$WEBGW_REST_PORT"
fi

if [ x$JUSTICE_DEBUG == xtrue ];then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5001"
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
fi

echo "******-----    start webgw   ----*******"

java $HOME_OPT $JAVA_OPTS $WEBGW_RESOURCE_OPTS -jar ${BIN_HOME}/com.lnjoying.justice.webgw.jar  1> /dev/null 2> ${LOG_OUT}/webgw.out  &
sleep 3