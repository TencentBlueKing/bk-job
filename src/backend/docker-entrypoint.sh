#!/bin/sh

echo "The application start..."
exec java ${JAVA_OPTS} ${JAVA_SYS_OPTS} -jar /app/app.jar
