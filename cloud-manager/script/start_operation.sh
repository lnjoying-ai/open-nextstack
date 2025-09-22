HOME_OPT="$BASE_OPT -Dlog4j.configurationFile=$LOG_CFG_HOME/operation_log4j2.xml"
JAVA_OPTS="$JAVA_OPTS -server -XX:CICompilerCount=2 -XX:+UseG1GC -XX:ConcGCThreads=1 -XX:ParallelGCThreads=2 -Xmx1g -Xms500m -Xmn400m -Xss256k -XX:+ExplicitGCInvokesConcurrent -XX:MaxDirectMemorySize=256m"
if [ x$JUSTICE_DEBUG == xtrue ];then
    DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5011"
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
fi

IMAGE_RESOURCE_OPTS="$RESOURCE_OPTS"
if [ $IMAGE_REST_PORT ];then
  IMAGE_RESOURCE_OPTS="$IMAGE_RESOURCE_OPTS -Dservicecomb.rest.address=0.0.0.0:$IMAGE_REST_PORT"
fi
if [ $IMAGE_HIGHWAY_PORT ];then
  IMAGE_RESOURCE_OPTS="$IMAGE_RESOURCE_OPTS -Dservicecomb.highway.address=0.0.0.0:$IMAGE_HIGHWAY_PORT"
fi

#echo "******-----   start image      ----*******"
#java $HOME_OPT $JAVA_OPTS -jar ${BIN_HOME}/com.justice.image.jar  1> /dev/null 2> ${LOG_OUT}/image.out  &

echo "******-----   start operation      ----*******"
java $HOME_OPT $JAVA_OPTS $IMAGE_RESOURCE_OPTS -jar ${BIN_HOME}/com.lnjoying.justice.operation.jar  1> /dev/null 2> ${LOG_OUT}/operation.out  &
