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
    mavenLocal()

    // 用户通过命令行方式指定的Maven仓库
    var mavenRepoUrls: String? = System.getProperty("mavenRepoUrl")
    if (mavenRepoUrls.isNullOrBlank()) {
        // gradle.properties中的Maven仓库，优先级低于命令行-D参数
        mavenRepoUrls = project.findProperty("MAVEN_REPO_URL")?.toString()
    }
    if (!mavenRepoUrls.isNullOrBlank()) {
        mavenRepoUrls.trim()
            .replace(" ", "")
            .split(",")
            .filter { it.isNotBlank() }
            .forEach { repoUrl ->
                println("Add maven repo:$repoUrl")
                maven { url = uri(repoUrl) }
            }
    }

    mavenCentral()
}

val springAiVersion = "1.1.4"
val commonsLang3Version = "3.18.0"
val jacksonVersion = "2.17.2"
val okhttpVersion = "4.12.0"
val commonsCollectionsVersion = "4.4"
val mybatisSpringBootVersion = "3.0.3"
val mysqlConnectorVersion = "8.0.33"
val testcontainersVersion = "1.20.4"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:$mybatisSpringBootVersion")
    runtimeOnly("com.mysql:mysql-connector-j:$mysqlConnectorVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    implementation("org.apache.commons:commons-collections4:$commonsCollectionsVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:mysql:$testcontainersVersion")
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

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("mysqlIntegration")
    }
}

tasks.register<Test>("mysqlIntegrationTest") {
    description = "Runs MySQL 5.7 integration tests"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("mysqlIntegration")
    }
    shouldRunAfter(tasks.named("test"))
}

// ---------------------------------------------------------------------------
// OP Web页面前端构建
// 只改后端时可用 ./gradlew build -PskipFrontend 跳过，避免下载Node与npm依赖
// ---------------------------------------------------------------------------
val skipFrontend = project.hasProperty("skipFrontend")
// 前端工程与后端工程在 op-tools/bk-job-op/src 下并列
val frontendDir = file("../frontend")

node {
    // 与 ../frontend/.nvmrc 保持一致
    version.set("16.20.2")
    download.set(true)
    nodeProjectDir.set(frontendDir)
}

val npmInstallFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    args.set(listOf("install", "--no-audit", "--no-fund"))
    workingDir.set(frontendDir)
    inputs.files(
        file("../frontend/package.json"),
        file("../frontend/package-lock.json")
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("../frontend/node_modules"))
}

// webpack 直接把产物写进 src/main/resources/static（清理时保留后端自用的静态提示页）
val buildFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    dependsOn(npmInstallFrontend)
    args.set(listOf("run", "build"))
    workingDir.set(frontendDir)
    inputs.dir(file("../frontend/src")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(
        file("../frontend/webpack.config.js"),
        file("../frontend/index.html"),
        file("../frontend/.babelrc"),
        file("../frontend/.browserslistrc")
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("src/main/resources/static"))
}

tasks.named("processResources") {
    if (!skipFrontend) {
        dependsOn(buildFrontend)
    }
}
