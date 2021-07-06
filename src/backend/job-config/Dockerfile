FROM gradle:6-jdk8-hotspot as builder

# 加速构建
# COPY docker/gradle/init.gradle /home/gradle/.gradle/init.gradle
WORKDIR /app
COPY release ./release
# RUN gradle -i clean build -x test

FROM openjdk:8-jdk
LABEL maintainer="llitfkitfk@gmail.com"
COPY docker-entrypoint.sh .
COPY --from=builder /app/release/job-config-1.0.0.jar /app/app.jar
ENV TZ="Asia/Shanghai"
ENV JAVA_OPTS="-Xms256m -Xmx512g -Xmn256m"
ENV JAVA_GC_OPTS="-verbose:gc -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ENV JAVA_SYS_OPTS="-Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["/docker-entrypoint.sh"]
