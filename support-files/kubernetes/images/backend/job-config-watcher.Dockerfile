FROM bkjob/jdk17:3.12.1

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.12.1"

ARG VERSION=3.0.0
RUN curl -L -o /app.jar \
    "https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-kubernetes-configuration-watcher/${VERSION}/spring-cloud-kubernetes-configuration-watcher-${VERSION}.jar"

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app.jar"]
