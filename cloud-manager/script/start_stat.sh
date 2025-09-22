#!/bin/bash

HOME_OPT="$BASE_OPT -Dlog4j.configurationFile=$LOG_CFG_HOME/stat_log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -server -XX:CICompilerCount=2 -XX:+UseG1GC -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -Xmx1g -Xms500m -Xmn400m -Xss256k -XX:+ExplicitGCInvokesConcurrent -XX:MaxDirectMemorySize=256m"
STAT_RESOURCE_OPTS="$RESOURCE_OPTS"
if [ $STAT_REST_PORT ];then
  STAT_RESOURCE_OPTS="$STAT_RESOURCE_OPTS -Dservicecomb.rest.address=0.0.0.0:$STAT_REST_PORT"
fi
if [ $STAT_HIGHWAY_PORT ];then
  STAT_RESOURCE_OPTS="$STAT_RESOURCE_OPTS -Dservicecomb.highway.address=0.0.0.0:$STAT_HIGHWAY_PORT"
fi

if [ x$JUSTICE_DEBUG == xtrue ];then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5003"
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
fi

echo "******-----     start stat    ----*******"

java $HOME_OPT $JAVA_OPTS $STAT_RESOURCE_OPTS -jar ${BIN_HOME}/com.lnjoying.justice.stat.jar  1> /dev/null 2> ${LOG_OUT}/stat.out  &
sleep 3