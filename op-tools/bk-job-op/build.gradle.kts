plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.tencent.bk.job"
version = "0.0.1-SNAPSHOT"
description = "Op system for bk-job"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

val springAiVersion = "1.1.4"
val commonsLang3Version = "3.18.0"
val jacksonVersion = "2.17.2"
val okhttpVersion = "4.12.0"
val commonsCollectionsVersion = "4.4"
val mybatisSpringBootVersion = "3.0.3"
val mysqlConnectorVersion = "8.0.33"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:$mybatisSpringBootVersion")
    runtimeOnly("com.mysql:mysql-connector-j:$mysqlConnectorVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    implementation("org.apache.commons:commons-collections4:$commonsCollectionsVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:$jacksonVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// OP Web页面前端构建
// 只改后端时可用 ./gradlew build -PskipFrontend 跳过，避免下载Node与npm依赖
// ---------------------------------------------------------------------------
val skipFrontend = project.hasProperty("skipFrontend")
val frontendDir = file("src/frontend")

node {
    // 与 src/frontend/.nvmrc 保持一致
    version.set("16.20.2")
    download.set(true)
    nodeProjectDir.set(frontendDir)
}

val npmInstallFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    args.set(listOf("install", "--no-audit", "--no-fund"))
    workingDir.set(frontendDir)
    inputs.files(
        file("src/frontend/package.json"),
        file("src/frontend/package-lock.json")
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("src/frontend/node_modules"))
}

// webpack 直接把产物写进 src/main/resources/static（清理时保留后端自用的静态提示页）
val buildFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    dependsOn(npmInstallFrontend)
    args.set(listOf("run", "build"))
    workingDir.set(frontendDir)
    inputs.dir(file("src/frontend/src")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(
        file("src/frontend/webpack.config.js"),
        file("src/frontend/index.html"),
        file("src/frontend/.babelrc"),
        file("src/frontend/.browserslistrc")
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("src/main/resources/static"))
}

tasks.named("processResources") {
    if (!skipFrontend) {
        dependsOn(buildFrontend)
    }
}
