#!/bin/bash

HOME_OPT="$BASE_OPT -Dlog4j.configurationFile=$LOG_CFG_HOME/ecrm_log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -server -XX:CICompilerCount=2 -XX:+UseG1GC -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -Xmx1g -Xms500m -Xmn400m -Xss256k -XX:+ExplicitGCInvokesConcurrent -XX:MaxDirectMemorySize=256m"
ECRM_RESOURCE_OPTS="$RESOURCE_OPTS"
if [ $ECRM_REST_PORT ];then
  ECRM_RESOURCE_OPTS="$ECRM_RESOURCE_OPTS -Dservicecomb.rest.address=0.0.0.0:$ECRM_REST_PORT"
fi
if [ $ECRM_HIGHWAY_PORT ];then
  ECRM_RESOURCE_OPTS="$ECRM_RESOURCE_OPTS -Dservicecomb.highway.address=0.0.0.0:$ECRM_HIGHWAY_PORT"
fi

if [ x$JUSTICE_DEBUG == xtrue ];then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5008"
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
fi

echo "******-----    start ecrm    ----*******"

java $HOME_OPT $JAVA_OPTS $ECRM_RESOURCE_OPTS -jar ${BIN_HOME}/com.lnjoying.justice.ecrm.jar  1 > /dev/null 2> ${LOG_OUT}/ecrm.out  &
sleep 2