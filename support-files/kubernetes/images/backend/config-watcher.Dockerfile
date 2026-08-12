ARG VERSION=3.3.3
# Fix CVE-2026-55831, CVE-2026-55833, CVE-2026-56745, CVE-2026-56819 and CVE-2026-59901 in netty 4.1.135.Final.
ARG NETTY_VERSION=4.1.136.Final
# Fix CVE-2024-29371 in jose4j 0.9.4.
ARG JOSE4J_VERSION=0.9.6

FROM bkjob/jdk17:3.10.5 AS builder

ARG VERSION
ARG NETTY_VERSION
ARG JOSE4J_VERSION
WORKDIR /build

RUN curl --fail --show-error --silent --location \
    --retry 3 --retry-delay 5 --retry-all-errors --connect-timeout 30 \
    "https://github.com/spring-cloud/spring-cloud-kubernetes/archive/refs/tags/v${VERSION}.zip" \
    --output source.zip \
    && jar --extract --file source.zip \
    && rm source.zip

RUN WATCHER_DIR="spring-cloud-kubernetes-${VERSION}/spring-cloud-kubernetes-controllers/spring-cloud-kubernetes-configuration-watcher" \
    && sed -i "0,/<\\/dependencies>/s|</dependencies>|\\t<dependency>\\n\\t\\t\\t<groupId>org.bitbucket.b_c</groupId>\\n\\t\\t\\t<artifactId>jose4j</artifactId>\\n\\t\\t\\t<version>${JOSE4J_VERSION}</version>\\n\\t\\t</dependency>\\n\\t</dependencies>|" "${WATCHER_DIR}/pom.xml" \
    && grep -q "<version>${JOSE4J_VERSION}</version>" "${WATCHER_DIR}/pom.xml" \
    && cd "spring-cloud-kubernetes-${VERSION}" \
    && sh ./mvnw --batch-mode --no-transfer-progress \
        --projects spring-cloud-kubernetes-controllers/spring-cloud-kubernetes-configuration-watcher \
        -Dmaven.test.skip=true \
        -Dmaven.javadoc.skip=true \
        -Dnetty.version="${NETTY_VERSION}" \
        -Dspring-boot.build-image.skip=true \
        package \
    && test -f "spring-cloud-kubernetes-controllers/spring-cloud-kubernetes-configuration-watcher/target/spring-cloud-kubernetes-configuration-watcher-${VERSION}-exec.jar"

FROM bkjob/jdk17:3.10.5

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.10.5"

ARG VERSION
COPY --from=builder \
    "/build/spring-cloud-kubernetes-${VERSION}/spring-cloud-kubernetes-controllers/spring-cloud-kubernetes-configuration-watcher/target/spring-cloud-kubernetes-configuration-watcher-${VERSION}-exec.jar" \
    /app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app.jar"]
