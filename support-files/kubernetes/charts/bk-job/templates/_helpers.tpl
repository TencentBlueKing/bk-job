{{/*
Create a default fully qualified name for job.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "job.fullname" -}}
{{- printf "%s" (include "common.names.fullname" .) -}}
{{- end -}}

{{/*
Create the name of the service account for job
*/}}
{{- define "job.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (printf "%s" (include "common.names.fullname" .)) .Values.serviceAccount.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the label key of job image tag
*/}}
{{- define "job.labelKeys.imageTag" -}}
    {{- printf "bk.job.image/tag" -}}
{{- end -}}

{{/*
Return the label key of bk-job scope
*/}}
{{- define "job.labelKeys.jobScope" -}}
    {{- printf "bk.job.scope" -}}
{{- end -}}

{{/*
Return the label value of bk-job scope backend
*/}}
{{- define "job.labelValues.jobScope.backend" -}}
    {{- printf "backend" -}}
{{- end -}}

{{/*
Return the annotation key of bk-job sha256SumCommonConfigMap
*/}}
{{- define "job.annotationKeys.sha256SumCommonConfigMap" -}}
    {{- printf "sha256sum/commonConfigmap" -}}
{{- end -}}

{{/*
Return the annotation key of bk-job sha256SumServiceConfigMap
*/}}
{{- define "job.annotationKeys.sha256SumServiceConfigMap" -}}
    {{- printf "sha256sum/serviceConfigmap" -}}
{{- end -}}

{{/*
Return the proper job-frontend image name
*/}}
{{- define "job-frontend.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.frontendConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-gateway image name
*/}}
{{- define "job-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.gatewayConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-manage image name
*/}}
{{- define "job-manage.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.manageConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-execute image name
*/}}
{{- define "job-execute.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.executeConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-crontab image name
*/}}
{{- define "job-crontab.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.crontabConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-logsvr image name
*/}}
{{- define "job-logsvr.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.logsvrConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-backup image name
*/}}
{{- define "job-backup.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.backupConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-analysis image name
*/}}
{{- define "job-analysis.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.analysisConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-file-gateway image name
*/}}
{{- define "job-file-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.fileGatewayConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-file-worker image name
*/}}
{{- define "job-file-worker.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.fileWorkerConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-migration image name
*/}}
{{- define "job-migration.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.migration.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-config-watcher image name
*/}}
{{- define "job-config-watcher.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.k8sConfigWatcherConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-assemble image name
*/}}
{{- define "job-assemble.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.assembleConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-sync-bk-api-gateway image name
*/}}
{{- define "job-sync-bk-api-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.bkApiGatewayConfig.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-sync-bk-aidev image name
*/}}
{{- define "job-sync-bk-aidev.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.bkai.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "job.imagePullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.k8sConfigWatcherConfig.image .Values.assembleConfig.image .Values.frontendConfig.image .Values.bkApiGatewayConfig.image .Values.bkai.image .Values.migration.image .Values.gatewayConfig.image .Values.manageConfig.image .Values.executeConfig.image .Values.crontabConfig.image .Values.logsvrConfig.image .Values.backupConfig.image .Values.analysisConfig.image .Values.fileGatewayConfig.image .Values.fileWorkerConfig.image) "global" .Values.global) }}
{{- end -}}


{{/*
Fully qualified app name for MariaDB
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "job.mariadb.fullname" -}}
{{- printf "%s-%s" .Release.Name "mariadb" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Return the MariaDB Hostname
*/}}
{{- define "job.mariadb.host" -}}
{{- if .Values.mariadb.enabled }}
    {{- if eq .Values.mariadb.architecture "replication" }}
        {{- printf "%s-%s" (include "job.mariadb.fullname" .) "primary" | trunc 63 | trimSuffix "-" -}}
    {{- else -}}
        {{- printf "%s" (include "job.mariadb.fullname" .) -}}
    {{- end -}}
{{- else -}}
    {{- printf "%s" .Values.externalMariaDB.host -}}
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB Port
*/}}
{{- define "job.mariadb.port" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "3306" -}}
{{- else -}}
    {{- printf "%d" (.Values.externalMariaDB.port | int ) -}}
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB username
*/}}
{{- define "job.mariadb.username" -}}
{{- if eq (include "job.kms.mariadb.active" .) "true" -}}
${mariadb-username}
{{- else if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.auth.username -}}
{{- else -}}
    {{- printf "%s" .Values.externalMariaDB.username -}}
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB root password
*/}}
{{- define "job.mariadb.rootPassword" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.auth.rootPassword -}}
{{- else -}}
    {{- printf "%s" .Values.externalMariaDB.rootPassword -}}
{{- end -}}
{{- end -}}

{{/*
Return the migrate mysqlSchema admin username
*/}}
{{- define "job.migration.mysqlSchema.adminUsername" -}}
{{- printf "%s" .Values.job.migration.mysqlSchema.adminUsername | default "root" -}}
{{- end -}}

{{/*
Return the migrate mysqlSchema admin password
*/}}
{{- define "job.migration.mysqlSchema.adminPassword" -}}
{{- if .Values.job.migration.mysqlSchema.adminPassword -}}
    {{- printf "%s" .Values.job.migration.mysqlSchema.adminPassword -}}
{{- else if .Values.externalMariaDB.rootPassword -}}
    {{- printf "%s" .Values.externalMariaDB.rootPassword -}}
{{- else -}}
    {{- printf "%s" .Values.mariadb.auth.rootPassword -}}
{{- end -}}
{{- end -}}

{{/*
外部 中间件 是否改由 KMS 同步。集群内 中间件(MariaDB、redis等)、或用户自带 existingPasswordSecret 时不接管。
*/}}
{{- define "job.kms.credential.active" -}}
{{- $ctx := .context -}}
{{- $name := .name -}}
{{- $kms := $ctx.Values.kms | default dict -}}
{{- if $kms.enabled -}}
{{- if eq $name "mariadb" -}}
{{- if and (not $ctx.Values.mariadb.enabled) (not $ctx.Values.externalMariaDB.existingPasswordSecret) }}true{{- end -}}
{{- else if eq $name "redis" -}}
{{- if and (not $ctx.Values.redis.enabled) (not $ctx.Values.externalRedis.existingPasswordSecret) }}true{{- end -}}
{{- else if eq $name "rabbitmq" -}}
{{- if and (not $ctx.Values.rabbitmq.enabled) (not $ctx.Values.externalRabbitMQ.existingPasswordSecret) }}true{{- end -}}
{{- else if eq $name "mongodb" -}}
{{- if and (not $ctx.Values.mongodb.enabled) (not $ctx.Values.externalMongoDB.existingPasswordSecret) (not $ctx.Values.externalMongoDB.uri) }}true{{- end -}}
{{- else if eq $name "crontabdb" -}}
{{- if $ctx.Values.crontabConfig.database.host }}true{{- end -}}
{{- else if eq $name "archive-mariadb" -}}
{{- if eq (include "job.archiveMariadb.required" $ctx) "true" }}true{{- end -}}
{{- else if eq $name "common" -}}
true
{{- else if eq $name "migration" -}}
{{- if or $ctx.Values.migration.enabled $ctx.Values.bkApiGatewayConfig.sync }}true{{- end -}}
{{- else if eq $name "manage" -}}
{{- if or (eq $ctx.Values.deploy.mode "lite") $ctx.Values.manageConfig.enabled }}true{{- end -}}
{{- else if eq $name "analysis" -}}
{{- if or (eq $ctx.Values.deploy.mode "lite") $ctx.Values.analysisConfig.enabled }}true{{- end -}}
{{- else if eq $name "file-worker" -}}
{{- if $ctx.Values.fileWorkerConfig.enabled }}true{{- end -}}
{{- else if eq $name "execute" -}}
{{- $executeDs := $ctx.Values.executeConfig.mysql.verticalSharding.datasource | default dict -}}
{{- if and $ctx.Values.executeConfig.mysql.verticalSharding.enabled (gt (len $executeDs) 0) }}true{{- end -}}
{{- else if eq $name "backup" -}}
{{- $backupMariadb := $ctx.Values.backupConfig.archive.execute.mariadb | default dict -}}
{{- $backupVs := index $backupMariadb "verticalSharding" | default dict -}}
{{- $backupDs := index $backupVs "datasource" | default dict -}}
{{- if and $ctx.Values.backupConfig.archive.execute.enabled (eq $backupMariadb.dataSourceMode "vertical_sharding") (gt (len $backupDs) 0) }}true{{- end -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "job.kms.mariadb.active" -}}
{{- include "job.kms.credential.active" (dict "name" "mariadb" "context" .) -}}
{{- end -}}

{{/*
当前工作负载实际挂载的、且已切到 KMS 的凭证。多个私钥写在同一组 env 里。
*/}}
{{- define "job.kms.env" -}}
{{- $ctx := .context -}}
{{- $active := list -}}
{{- range .names -}}
{{- if eq (include "job.kms.credential.active" (dict "name" . "context" $ctx)) "true" -}}
{{- $active = append $active . -}}
{{- end -}}
{{- end -}}
{{- if $active }}
- name: ENABLE_KMS
  value: "true"
- name: BK_JOB_KMS_CREDENTIALS
  value: {{ join "," $active }}
{{- range $name := $active }}
- name: BK_JOB_KMS_PRIVATE_KEY_{{ $name | upper | replace "-" "_" }}
  valueFrom:
    secretKeyRef:
      name: {{ include "job.kms.targetSecretName" (dict "name" $name "context" $ctx) }}
      key: privateKey
{{- end }}
{{- end }}
{{- end -}}

{{/*
ESO 写出的 Secret。原名加 -kms，和 Helm 明文 Secret 分开，避免升级时抢同一份对象。
*/}}
{{- define "job.kms.targetSecretName" -}}
{{- $base := "" -}}
{{- if eq .name "mariadb" -}}
{{- $base = include "job.mariadb.secretName" .context -}}
{{- else if eq .name "redis" -}}
{{- $base = include "job.redis.secretName" .context -}}
{{- else if eq .name "rabbitmq" -}}
{{- $base = include "job.rabbitmq.secretName" .context -}}
{{- else if eq .name "mongodb" -}}
{{- $base = include "job.mongodb.secretName" .context -}}
{{- else if eq .name "crontabdb" -}}
{{- $base = include "job.crontabdb.secretName" .context -}}
{{- else if eq .name "archive-mariadb" -}}
{{- $base = include "job.archiveMariadb.secretName" .context -}}
{{- else if eq .name "common" -}}
{{- $base = include "job.common.secretName" .context -}}
{{- else if eq .name "migration" -}}
{{- $base = printf "%s-migration" (include "job.fullname" .context) -}}
{{- else if or (eq .name "manage") (eq .name "analysis") (eq .name "file-worker") (eq .name "execute") (eq .name "backup") -}}
{{- $base = printf "%s-%s" (include "job.fullname" .context) .name -}}
{{- end -}}
{{- printf "%s-kms" ($base | trim) -}}
{{- end -}}

{{- define "job.kms.credential.valuesKey" -}}
{{- if eq . "archive-mariadb" -}}
archiveMariadb
{{- else if eq . "file-worker" -}}
fileWorker
{{- else -}}
{{- . -}}
{{- end -}}
{{- end -}}

{{- define "job.kms.credential.field" -}}
{{- $key := include "job.kms.credential.valuesKey" .name -}}
{{- index (index .context.Values.kms.credentials $key) .field -}}
{{- end -}}

{{/*
KMS 打开时只挂信封；关闭时整卷挂到原来的 /etc/secrets 路径。
*/}}
{{- define "job.kms.secretVolumeMount" -}}
- name: {{ .volume }}
{{- if eq (include "job.kms.credential.active" (dict "name" .credential "context" .context)) "true" }}
  mountPath: /etc/kms/{{ .credential }}/envelope
  subPath: envelope
  readOnly: true
{{- else }}
  mountPath: {{ .mountPath }}
  readOnly: true
{{- end }}
{{- end -}}

{{/*
开启kms时，只挂secret中的信封文件。
关闭kms时，整卷挂到原来的 /etc/secrets 路径。
*/}}
{{- define "job.kms.secretVolume" -}}
- name: {{ .volume }}
  secret:
{{- if eq (include "job.kms.credential.active" (dict "name" .credential "context" .context)) "true" }}
    secretName: {{ include "job.kms.targetSecretName" (dict "name" .credential "context" .context) }}
    items:
      - key: envelope
        path: envelope
{{- else }}
    secretName: {{ .secretName }}
{{- end }}
{{- end -}}

{{/*
common 凭证的 Secret 名。ESO 目标名是这个名字加 -kms。
信封 JSON 的 key 就是 Spring 属性名，接管时公共 ConfigMap 不再渲染这些项。
*/}}
{{- define "job.common.secretName" -}}
{{- printf "%s-common" (include "job.fullname" .) -}}
{{- end -}}

{{/*
没有对应明文卷的凭证（common、migration）只在被 KMS 接管时挂信封，未接管时什么都不输出。
*/}}
{{- define "job.kms.envelopeVolumeMount" -}}
{{- if eq (include "job.kms.credential.active" (dict "name" .credential "context" .context)) "true" }}
{{ include "job.kms.secretVolumeMount" (dict "volume" .credential "credential" .credential "context" .context) }}
{{- end }}
{{- end -}}

{{- define "job.kms.envelopeVolume" -}}
{{- if eq (include "job.kms.credential.active" (dict "name" .credential "context" .context)) "true" }}
{{ include "job.kms.secretVolume" (dict "volume" .credential "credential" .credential "context" .context) }}
{{- end }}
{{- end -}}

{{/*
Return the MariaDB secret name
*/}}
{{- define "job.mariadb.secretName" -}}
{{- if .Values.externalMariaDB.existingPasswordSecret -}}
    {{- printf "%s" .Values.externalMariaDB.existingPasswordSecret -}}
{{- else if .Values.mariadb.enabled }}
    {{- printf "%s" (include "job.mariadb.fullname" .) -}}
{{- else -}}
    {{- printf "%s-%s" (include "job.fullname" .) "external-mariadb" -}}
{{- end -}}
{{- end -}}

{{/*
Return the JDBC MySQL Driver Class
*/}}
{{- define "job.jdbcMysqlDriverClass" -}}
{{- printf "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver" -}}
{{- end -}}

{{/*
Return the MariaDB jdbc connection url base properties (without ssl config)
*/}}
{{- define "job.mariadb.base.connection.properties" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.connection.properties -}}
{{- else -}}
    {{- printf "%s" .Values.externalMariaDB.connection.properties -}}
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB jdbc connection sslMode
*/}}
{{- define "job.mariadb.sslMode" -}}
{{- if .Values.externalMariaDB.tls.verifyHostname }}
    {{- printf "VERIFY_IDENTITY" -}}
{{- else -}}
    {{- printf "VERIFY_CA" -}}
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB trustStore password key
*/}}
{{- define "job.mariadb.trustStorePasswordKey" -}}
{{ printf "${mariadb-truststore-password}" }}
{{- end -}}

{{/*
Return the MariaDB keyStore password key
*/}}
{{- define "job.mariadb.keyStorePasswordKey" -}}
{{ printf "${mariadb-keystore-password}" }}
{{- end -}}

{{/*
Return the MariaDB jdbc connection ssl properties
*/}}
{{- define "job.mariadb.ssl.properties" -}}
{{- if or (.Values.mariadb.enabled) (not .Values.externalMariaDB.tls.enabled) }}
    {{- printf "" -}}
{{- else -}}
    {{- if (not .Values.externalMariaDB.tls.keyStoreFilename) }}
        {{- printf "&sslMode=%s&trustCertificateKeyStoreType=%s&trustCertificateKeyStoreUrl=file:/etc/certs/mariadb/%s&trustCertificateKeyStorePassword=%s" (include "job.mariadb.sslMode" .) .Values.externalMariaDB.tls.trustStoreType .Values.externalMariaDB.tls.trustStoreFilename (include "job.mariadb.trustStorePasswordKey" .) -}}
    {{- else -}}
        {{- printf "&sslMode=%s&trustCertificateKeyStoreType=%s&trustCertificateKeyStoreUrl=file:/etc/certs/mariadb/%s&trustCertificateKeyStorePassword=%s&clientCertificateKeyStoreType=%s&clientCertificateKeyStoreUrl=file:/etc/certs/mariadb/%s&clientCertificateKeyStorePassword=%s" (include "job.mariadb.sslMode" .) .Values.externalMariaDB.tls.trustStoreType .Values.externalMariaDB.tls.trustStoreFilename (include "job.mariadb.trustStorePasswordKey" .) .Values.externalMariaDB.tls.keyStoreType .Values.externalMariaDB.tls.keyStoreFilename (include "job.mariadb.keyStorePasswordKey" .) -}}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB jdbc connection url properties
*/}}
{{- define "job.mariadb.connection.properties" -}}
{{- $baseProps := include "job.mariadb.base.connection.properties" . -}}
{{- $sslProps := include "job.mariadb.ssl.properties" . -}}
{{- printf "%s%s" $baseProps $sslProps -}}
{{- end -}}

{{/*
Return the JDBC MySQL scheme
*/}}
{{- define "job.jdbcMysqlScheme" -}}
{{- printf "jdbc:otel:mysql" -}}
{{- end -}}


{{/*
Fully qualified app name for Redis
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "job.redis.fullname" -}}
{{- printf "%s-%s" .Release.Name "redis" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Return the Redis hostname
*/}}
{{- define "job.redis.host" -}}
{{- if .Values.redis.enabled }}
    {{- printf "%s-master" (include "job.redis.fullname" .) -}}
{{- else -}}
    {{- printf "%s" .Values.externalRedis.host -}}
{{- end -}}
{{- end -}}

{{/*
Return the Redis port
*/}}
{{- define "job.redis.port" -}}
{{- if .Values.redis.enabled }}
    {{- printf "6379" -}}
{{- else -}}
    {{- printf "%d" (.Values.externalRedis.port | int ) -}}
{{- end -}}
{{- end -}}

{{/*
Return the Redis secret name
*/}}
{{- define "job.redis.secretName" -}}
{{- if .Values.externalRedis.existingPasswordSecret -}}
    {{- printf "%s" .Values.externalRedis.existingPasswordSecret -}}
{{- else if .Values.redis.enabled }}
    {{- printf "%s" (include "job.redis.fullname" .) -}}
{{- else -}}
    {{- printf "%s-%s" (include "job.fullname" .) "external-redis" -}}
{{- end -}}
{{- end -}}

{{/*
Return the Redis config
*/}}
{{- define "job.redis.config" -}}
{{- if .Values.redis.enabled }}
{{- if eq .Values.redis.architecture "standalone" }}
host: {{ include "job.redis.host" . }}
port: {{ include "job.redis.port" . }}
password: {{ .Values.redis.existingPasswordKey | default "redis-password" | printf "${%s}" }}
{{- else }}
fail "Not supported redis architecture"
{{- end }}
ssl:
  enabled: false
{{- else }}
password: {{ .Values.externalRedis.existingPasswordKey | default "redis-password" | printf "${%s}" }}
ssl:
  enabled: {{ .Values.externalRedis.tls.enabled }}
{{- if eq .Values.externalRedis.architecture "standalone" }}
host: {{ include "job.redis.host" . }}
port: {{ include "job.redis.port" . }}
{{- else if eq .Values.externalRedis.architecture "replication" }}
sentinel:
  {{- if .Values.externalRedis.sentinel.auth }}
  password: {{ .Values.externalRedis.sentinel.existingPasswordKey | default "redis-sentinel-password" | printf "${%s}" }}
  {{- end }}
  master: {{ .Values.externalRedis.sentinel.master }}
  nodes: {{ .Values.externalRedis.sentinel.nodes }}
{{- else -}}
fail "Invalid external redis architecture"
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the redis tls config
*/}}
{{- define "job.redis.tls" -}}
{{- $kms := eq (include "job.kms.credential.active" (dict "name" "redis" "context" .)) "true" -}}
{{- if .Values.redis.enabled -}}
enabled: false
{{- else -}}
enabled: {{ .Values.externalRedis.tls.enabled }}
{{- end }}
trustStoreType: {{ .Values.externalRedis.tls.trustStoreType }}
trustStore: /etc/certs/redis/{{ .Values.externalRedis.tls.trustStoreFilename }}
{{- if $kms }}
trustStorePassword: ${redis-truststore-password}
{{- else }}
trustStorePassword: {{ .Values.externalRedis.tls.trustStorePassword }}
{{- end }}
keyStoreType: {{ .Values.externalRedis.tls.keyStoreType }}
{{- if .Values.externalRedis.tls.keyStoreFilename }}
keyStore: /etc/certs/redis/{{ .Values.externalRedis.tls.keyStoreFilename }}
{{- end }}
{{- if .Values.externalRedis.tls.keyStorePassword }}
{{- if $kms }}
keyStorePassword: ${redis-keystore-password}
{{- else }}
keyStorePassword: {{ .Values.externalRedis.tls.keyStorePassword }}
{{- end }}
{{- end }}
verifyHostname: {{ .Values.externalRedis.tls.verifyHostname }}
{{- end -}}


{{/*
Create a default fully qualified app name for RabbitMQ subchart
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "job.rabbitmq.fullname" -}}
{{- if .Values.rabbitmq.fullnameOverride -}}
{{- .Values.rabbitmq.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "rabbitmq" .Values.rabbitmq.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ host
*/}}
{{- define "job.rabbitmq.host" -}}
{{- if .Values.rabbitmq.enabled }}
    {{- printf "%s" (include "job.rabbitmq.fullname" .) -}}
{{- else -}}
    {{- printf "%s" .Values.externalRabbitMQ.host -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ Port
*/}}
{{- define "job.rabbitmq.port" -}}
{{- if .Values.rabbitmq.enabled }}
    {{- printf "%d" (.Values.rabbitmq.service.ports.amqp | int ) -}}
{{- else -}}
    {{- printf "%d" (.Values.externalRabbitMQ.port | int ) -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ username
*/}}
{{- define "job.rabbitmq.usernameLiteral" -}}
{{- if .Values.rabbitmq.enabled }}
    {{- printf "%s" .Values.rabbitmq.auth.username -}}
{{- else -}}
    {{- printf "%s" .Values.externalRabbitMQ.username -}}
{{- end -}}
{{- end -}}

{{- define "job.rabbitmq.username" -}}
{{- if eq (include "job.kms.credential.active" (dict "name" "rabbitmq" "context" .)) "true" -}}
${rabbitmq-username}
{{- else -}}
{{- include "job.rabbitmq.usernameLiteral" . -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ secret name
*/}}
{{- define "job.configWatcher.rabbitmq.secretName" -}}
{{- if eq (include "job.kms.credential.active" (dict "name" "rabbitmq" "context" .)) "true" -}}
{{- printf "%s-%s" (include "job.fullname" .) "config-watcher-rabbitmq" -}}
{{- else -}}
{{- include "job.rabbitmq.secretName" . -}}
{{- end -}}
{{- end -}}

{{- define "job.rabbitmq.secretName" -}}
{{- if .Values.externalRabbitMQ.existingPasswordSecret -}}
    {{- printf "%s" .Values.externalRabbitMQ.existingPasswordSecret -}}
{{- else if .Values.rabbitmq.enabled }}
    {{- printf "%s-%s" (include "job.rabbitmq.fullname" .) "extra" -}}
{{- else -}}
    {{- printf "%s-%s" (include "job.fullname" .) "external-rabbitmq" -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ password key
*/}}
{{- define "job.rabbitmq.passwordKey" -}}
{{ if .Values.externalRabbitMQ.existingPasswordSecret }}
{{ .Values.externalRabbitMQ.existingPasswordKey | default "rabbitmq-password" | printf "${%s}" }}
{{- else -}}
${rabbitmq-password}
{{- end }}
{{- end -}}

{{/*
Return the RabbitMQ vhost
*/}}
{{- define "job.rabbitmq.vhost" -}}
{{- if .Values.rabbitmq.enabled }}
    {{- printf "job" -}}
{{- else -}}
    {{- default "job" (printf "%s" .Values.externalRabbitMQ.vhost) -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ SSL Config
*/}}
{{- define "job.rabbitmq.sslConfig" -}}
{{- if and (not .Values.rabbitmq.enabled) (.Values.externalRabbitMQ.tls.enabled) }}
{{- $kms := eq (include "job.kms.credential.active" (dict "name" "rabbitmq" "context" .)) "true" }}
rabbitmq:
  ssl:
    enabled: {{ .Values.externalRabbitMQ.tls.enabled }}
    trustStoreType: {{ .Values.externalRabbitMQ.tls.trustStoreType }}
    trustStore: file:/etc/certs/rabbitmq/{{ .Values.externalRabbitMQ.tls.trustStoreFilename }}
    {{- if $kms }}
    trustStorePassword: ${rabbitmq-truststore-password}
    {{- else }}
    trustStorePassword: {{ .Values.externalRabbitMQ.tls.trustStorePassword }}
    {{- end }}
    keyStoreType: {{ .Values.externalRabbitMQ.tls.keyStoreType }}
    {{- if .Values.externalRabbitMQ.tls.keyStoreFilename }}
    keyStore: file:/etc/certs/rabbitmq/{{ .Values.externalRabbitMQ.tls.keyStoreFilename }}
    {{- end }}
    {{- if .Values.externalRabbitMQ.tls.keyStorePassword }}
    {{- if $kms }}
    keyStorePassword: ${rabbitmq-keystore-password}
    {{- else }}
    keyStorePassword: {{ .Values.externalRabbitMQ.tls.keyStorePassword }}
    {{- end }}
    {{- end }}
    verifyHostname: {{ .Values.externalRabbitMQ.tls.verifyHostname }}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ trustStore password key
*/}}
{{- define "job.configWatcher.rabbitmq.trustStorePasswordKey" -}}
{{ printf "${rabbitmq-truststore-password}" }}
{{- end -}}

{{/*
Return the RabbitMQ keyStore password key
*/}}
{{- define "job.configWatcher.rabbitmq.keyStorePasswordKey" -}}
{{ printf "${rabbitmq-keystore-password}" }}
{{- end -}}

{{/*
Return the RabbitMQ of config-watcher SSL Env Vars
*/}}
{{- define "job.configWatcher.rabbitmq.sslEnv" -}}
{{- if and (not .Values.rabbitmq.enabled) (.Values.externalRabbitMQ.tls.enabled) }}
- name: spring.rabbitmq.ssl.enabled
  value: {{ .Values.externalRabbitMQ.tls.enabled | quote }}
- name: spring.rabbitmq.ssl.trustStoreType
  value: {{ .Values.externalRabbitMQ.tls.trustStoreType }}
- name: spring.rabbitmq.ssl.trustStore
  value: file:/etc/certs/rabbitmq/{{ .Values.externalRabbitMQ.tls.trustStoreFilename }}
- name: spring.rabbitmq.ssl.trustStorePassword
  value: {{ include "job.configWatcher.rabbitmq.trustStorePasswordKey" . }}
- name: spring.rabbitmq.ssl.keyStoreType
  value: {{ .Values.externalRabbitMQ.tls.keyStoreType }}
{{- if .Values.externalRabbitMQ.tls.keyStoreFilename }}
- name: spring.rabbitmq.ssl.keyStore
  value: file:/etc/certs/rabbitmq/{{ .Values.externalRabbitMQ.tls.keyStoreFilename }}
{{- end }}
{{- if .Values.externalRabbitMQ.tls.keyStorePassword }}
- name: spring.rabbitmq.ssl.keyStorePassword
  value: {{ include "job.configWatcher.rabbitmq.keyStorePasswordKey" . }}
{{- end }}
- name: spring.rabbitmq.ssl.verifyHostname
  value: {{ .Values.externalRabbitMQ.tls.verifyHostname | quote }}
{{- end -}}
{{- end -}}

{{/*
Fully qualified app name for MongoDB
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "job.mongodb.fullname" -}}
{{- printf "%s-%s" .Release.Name "mongodb" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Return the MongoDB hostsAndPorts
*/}}
{{- define "job.mongodb.hostsAndPorts" -}}
{{- if .Values.mongodb.enabled }}
    {{- printf "%s:%d" (include "job.mongodb.fullname" .) (.Values.mongodb.service.port | default 27017 | int ) -}}
{{- else }}
    {{- printf "%s" .Values.externalMongoDB.hostsAndPorts -}}
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB authenticationDatabase
*/}}
{{- define "job.mongodb.authenticationDatabase" -}}
{{- if .Values.mongodb.enabled }}
    {{- printf "%s" .Values.mongodb.auth.database -}}
{{- else -}}
    {{- (.Values.externalMongoDB.authenticationDatabase | default "admin" | printf "%s" ) -}}
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB username
*/}}
{{- define "job.mongodb.username" -}}
{{- if .Values.mongodb.enabled }}
    {{- printf "%s" .Values.mongodb.auth.username -}}
{{- else -}}
    {{- printf "%s" .Values.externalMongoDB.username -}}
{{- end -}}
{{- end -}}

{{/*
URI 里的用户名。KMS 打开时交给 Spring 从信封解析，关闭时仍由 Helm 写入字面量。
*/}}
{{- define "job.mongodb.usernameInUri" -}}
{{- if eq (include "job.kms.credential.active" (dict "name" "mongodb" "context" .)) "true" -}}
${mongodb-username}
{{- else -}}
{{- include "job.mongodb.username" . -}}
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB secret name
*/}}
{{- define "job.mongodb.secretName" -}}
{{- if .Values.mongodb.enabled -}}
    {{- printf "%s" (include "job.mongodb.fullname" .) -}}
{{- else if .Values.externalMongoDB.existingPasswordSecret }}
    {{- printf "%s" .Values.externalMongoDB.existingPasswordSecret -}}
{{- else -}}
    {{- printf "%s-%s" (include "job.fullname" .) "external-mongodb" -}}
{{- end -}}
{{- end -}}



{{/*
Return the MongoDB connect uri
*/}}
{{- define "job.mongodb.connect.uri" -}}
{{- if .Values.mongodb.enabled -}}
  {{- printf "mongodb://%s:%s@%s/?authSource=%s" (include "job.mongodb.usernameInUri" .) (printf "${%s}" "mongodb-passwords") (include "job.mongodb.hostsAndPorts" .) (include "job.mongodb.authenticationDatabase" .) -}}
{{- else -}}
  {{- if .Values.externalMongoDB.uri -}}
    {{- printf "%s" .Values.externalMongoDB.uri -}}
  {{- else -}}
    {{- $uri := (printf "mongodb://%s:%s@%s/?authSource=%s" (include "job.mongodb.usernameInUri" .) (.Values.externalMongoDB.existingPasswordKey | default "mongodb-password" | printf "${%s}" ) (include "job.mongodb.hostsAndPorts" .) (include "job.mongodb.authenticationDatabase" .)) -}}
    {{- if (eq .Values.externalMongoDB.architecture "replicaset") -}}
        {{- printf "%s&replicaSet=%s" $uri .Values.externalMongoDB.replicaSetName -}}
    {{- else -}}
        {{- printf "%s" $uri -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return whether the mongodb is sharded
*/}}
{{- define "job.mongodb.useShardingCluster" -}}
{{- if .Values.mongodb.enabled -}}
  {{- printf "%t" false -}}
{{- else -}}
  {{- if eq "shardedCluster" .Values.externalMongoDB.architecture -}}
    {{- printf "%t" true -}}
  {{- else -}}
    {{- printf "%t" false -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the mongodb tls config
*/}}
{{- define "job.mongodb.tls" -}}
{{- $kms := eq (include "job.kms.credential.active" (dict "name" "mongodb" "context" .)) "true" -}}
{{- if .Values.mongodb.enabled -}}
enabled: false
{{- else -}}
enabled: {{ .Values.externalMongoDB.tls.enabled }}
{{- end }}
trustStoreType: {{ .Values.externalMongoDB.tls.trustStoreType }}
trustStore: /etc/certs/mongodb/{{ .Values.externalMongoDB.tls.trustStoreFilename }}
{{- if $kms }}
trustStorePassword: ${mongodb-truststore-password}
{{- else }}
trustStorePassword: {{ .Values.externalMongoDB.tls.trustStorePassword }}
{{- end }}
keyStoreType: {{ .Values.externalMongoDB.tls.keyStoreType }}
{{- if .Values.externalMongoDB.tls.keyStoreFilename }}
keyStore: /etc/certs/mongodb/{{ .Values.externalMongoDB.tls.keyStoreFilename }}
{{- end }}
{{- if .Values.externalMongoDB.tls.keyStorePassword }}
{{- if $kms }}
keyStorePassword: ${mongodb-keystore-password}
{{- else }}
keyStorePassword: {{ .Values.externalMongoDB.tls.keyStorePassword }}
{{- end }}
{{- end }}
verifyHostname: {{ .Values.externalMongoDB.tls.verifyHostname }}
{{- end -}}

{{/*
Return the Job Profile
*/}}
{{- define "job.profile" -}}
    {{- printf "%s" .Values.job.profile -}}
{{- end -}}

{{/*
Return whether the environment is dev
*/}}
{{- define "job.profileIsDev" -}}
{{- if has "dev" (regexSplit "[ ]*,[ ]*" .Values.job.profile -1) -}}
{{- printf "%t" true -}}
{{- else -}}
{{- printf "%t" false -}}
{{- end -}}
{{- end -}}

{{/*
Return the Job InitContainer WaitForMigration Content
*/}}
{{- define "job.initContainer.waitForMigration" -}}
{{- if .Values.migration.enabled }}
- name: "migration-init"
  image: {{ include "common.images.image" (dict "imageRoot" .Values.waitForMigration.image "global" .Values.global) }}
  imagePullPolicy: {{ .Values.waitForMigration.image.pullPolicy }}
  resources:
    {{- toYaml .Values.waitForMigration.resources | nindent 4 }}
  args:
  - "job-wr"
  - {{ printf "%s-migration-%s" (include "common.names.fullname" .) .Chart.Version | quote }}
{{- end -}}
{{- end -}}

{{/*
Return the Job InitContainer WaitForSyncApiGateway Content
*/}}
{{- define "job.initContainer.waitForSyncApiGateway" -}}
{{- if .Values.bkApiGatewayConfig.sync }}
- name: "wait-sync-api-gateway"
  image: {{ include "common.images.image" (dict "imageRoot" .Values.waitForMigration.image "global" .Values.global) }}
  imagePullPolicy: {{ .Values.waitForMigration.image.pullPolicy }}
  resources:
    {{- toYaml .Values.waitForMigration.resources | nindent 4 }}
  args:
  - "job-wr"
  - {{ printf "%s-sync-api-gateway-%s" (include "common.names.fullname" .) .Chart.Version | quote }}
{{- end -}}
{{- end -}}

{{/*
Return the Job InitContainer WaitForDependServices Content
{{ include "job.initContainer.waitForDependServices" ( dict "appName" "${appName}" "context" $) }}
*/}}
{{- define "job.initContainer.waitForDependServices" -}}
{{- if .context.Values.waitForDependServices.enabled -}}
- name: "wait-for-depend-services"
  image: {{ include "common.images.image" (dict "imageRoot" .context.Values.waitForDependServices.image "global" .context.Values.global) }}
  imagePullPolicy: {{ .context.Values.waitForDependServices.image.pullPolicy }}
  resources:
    {{- toYaml .context.Values.waitForDependServices.resources | nindent 4 }}
  env:
    - name: KUBERNETES_NAMESPACE
      value: {{ .context.Release.Namespace }}
    - name: BK_JOB_CURRENT_SERVICE_NAME
      value: {{ .currentServiceName }}
    - name: BK_JOB_STARTUP_DEPENDENCIES_STR
      value: {{ .context.Values.waitForDependServices.dependencies }}
    - name: BK_JOB_LOG_LEVEL
      value: {{ .context.Values.waitForDependServices.logLevel }}
    - name: BK_JOB_EXPECT_POD_LABELS_COMMON
      value: {{ .context.Values.waitForDependServices.expectPodLabels.common }}
    - name: BK_JOB_EXPECT_POD_LABELS_SERVICE
      value: {{ .context.Values.waitForDependServices.expectPodLabels.service }}
    - name: BK_JOB_EXTERNAL_DEPENDENCY_CHECK_ENABLED
      value: {{ .context.Values.waitForDependServices.externalDependencyCheck.enabled | quote }}
    - name: BK_JOB_EXTERNAL_DEPENDENCY_CHECK_URL
      value: {{ .context.Values.waitForDependServices.externalDependencyCheck.url }}
{{- end -}}
{{- end -}}


{{/*
Return the Job Web Scheme
*/}}
{{- define "job.web.scheme" -}}
    {{- printf "%s" .Values.bkDomainScheme -}}
{{- end -}}

{{/*
Return the Job Web Domain
SubPath: example.com
SubDomain: job.example.com
*/}}
{{- define "job.web.domain" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ .Values.global.bkDomain }}
{{- else -}}
{{ .Values.job.web.domain }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Web Host
SubPath: http(s)://example.com
SubDomain: http(s)://job.example.com
*/}}
{{- define "job.web.host" -}}
{{ printf "%s://%s" (include "job.web.scheme" .) (include "job.web.domain" .) }}
{{- end -}}

{{/*
Return the Job API Path Prefix
SubPath: "job/"
SubDomain: ""
*/}}
{{- define "job.api.path.prefix" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{- $rootPrefix := .Values.bkWebSiteAccess.subpath.rootPrefix | trimPrefix "/" -}}
{{- $finalPath := printf "%s/" $rootPrefix -}}
{{ $finalPath }}
{{- else -}}
{{ printf "" }}
{{- end -}}
{{- end -}}

{{/*
Return the Job URL Base
SubPath: http(s)://example.com/job
SubDomain: http(s)://job.example.com
*/}}
{{- define "job.url.base" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s%s" (include "job.web.host" .) .Values.bkWebSiteAccess.subpath.rootPrefix }}
{{- else -}}
{{ include "job.web.host" . }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Web URL
*/}}
{{- define "job.web.url" -}}
{{- if .Values.job.web.extraWebUrls -}}
{{ printf "%s,%s" (include "job.url.base" .) .Values.job.web.extraWebUrls }}
{{- else -}}
{{ printf "%s" (include "job.url.base" .) }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Web API URL
*/}}
{{- define "job.web.api.url" -}}
{{ printf "%s" (include "job.url.base" .) }}
{{- end -}}

{{/*
Return the default AI agent root url
指向部署时自动注册到AIDev平台的作业平台智能体，在未配置analysisConfig.ai.agentRootUrl时生效
例：http://bkapi.example.com/api/bp-ai-bkjob-web/prod/bk_plugin/plugin_api/
*/}}
{{- define "job.ai.default.agent.root.url" -}}
{{ printf "%s/bp-ai-bkjob-web/prod/bk_plugin/plugin_api/" (.Values.bkApiGatewayApiUrl | trimSuffix "/") }}
{{- end -}}

{{/*
Return the Job Frontend BK Static URL
*/}}
{{- define "job.frontend.bk.static.url" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s/" .Values.bkWebSiteAccess.subpath.rootPrefix }}
{{- else -}}
{{ printf "/" }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Frontend BK Site Path
*/}}
{{- define "job.frontend.bk.site.path" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s/" .Values.bkWebSiteAccess.subpath.rootPrefix }}
{{- else -}}
{{ printf "/" }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Url Path Root Prefix
SubPath: "/job"
SubDomain: ""
*/}}
{{- define "job.url.pathRootPrefix" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s" .Values.bkWebSiteAccess.subpath.rootPrefix }}
{{- else -}}
{{ printf "" }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Frontend Ingress Path
*/}}
{{- define "job.frontend.ingress.path" -}}
{{- if eq "subpath" .Values.bkWebSiteAccess.mode -}}
{{ printf "%s/(.*)" .Values.bkWebSiteAccess.subpath.rootPrefix }}
{{- else -}}
{{ printf "/" }}
{{- end -}}
{{- end -}}

{{/*
Return the sha256sum of configmap
*/}}
{{- define "annotations.sha256sum.configmap" -}}
{{ include "job.annotationKeys.sha256SumCommonConfigMap" . }}: {{ include (print .context.Template.BasePath "/configmap-common.yaml") .context | sha256sum }}
{{ include "job.annotationKeys.sha256SumServiceConfigMap" . }}: {{ include (print .context.Template.BasePath "/" .service "/configmap.yaml") .context | sha256sum }}
{{- end -}}

{{/*
Return the Job Storage Env Content
*/}}
{{- define "job.storage.env" -}}
- name: BK_JOB_STORAGE_BASE_DIR
  value: {{ .Values.persistence.localStorage.path }}
- name: BK_JOB_STORAGE_OUTER_DIR
  value: {{ .Values.persistence.localStorage.path }}
- name: BK_JOB_STORAGE_LOCAL_DIR
  value: {{ .Values.persistence.localStorage.path }}/local
{{- end -}}

{{/*
Return the approval config of job-analysis.
Mock 的"视为审批通过"ID 列表刻意不在此处下发：它随 common-refreshable 配置下发，改桩数据无需重启。
*/}}
{{- define "job.analysis.approval.config" -}}
approval:
  ttlHours: {{ .Values.analysisConfig.approval.ttlHours }}
  contentSimpleMaxBytes: {{ .Values.analysisConfig.approval.contentSimpleMaxBytes | default 20480 }}
  defaultChannel: {{ .Values.analysisConfig.approval.defaultChannel }}
  maxKeepDays: {{ .Values.analysisConfig.approval.maxKeepDays }}
  fastApproveThresholdMillis: {{ .Values.analysisConfig.approval.fastApproveThresholdMillis }}
  channels:
    imate:
      url: {{ .Values.analysisConfig.approval.channels.imate.url | quote }}
      appCode: {{ .Values.analysisConfig.approval.channels.imate.appCode | quote }}
      {{- if ne (include "job.kms.credential.active" (dict "name" "analysis" "context" .)) "true" }}
      openApi:
        # IMate 颁发给作业平台的凭证，回查审批结论时以 x-app-id / x-secret 发出，与蓝鲸凭证无关
        appId: {{ .Values.analysisConfig.approval.channels.imate.openApi.appId | quote }}
        secret: {{ .Values.analysisConfig.approval.channels.imate.openApi.secret | quote }}
      {{- end }}
      mock:
        # 该开关决定 Mock 渠道 Bean 是否注册，只能在启动前就位，改动后必须重启
        enabled: {{ .Values.analysisConfig.approval.channels.imate.mock.enabled }}
{{- end -}}

{{/*
Return the Job Config Env Content
*/}}
{{- define "job.config.env" -}}
- name: BK_JOB_PROFILE
  value: {{ include "job.profile" . }}
- name: JOB_COMMON_CONFIGMAP_NAME
  value: {{ include "common.names.fullname" . }}-common
- name: spring_cloud_kubernetes_secrets_paths
  value: /etc/secrets
{{- end -}}

{{/*
Return the Job Deploy Env Content
*/}}
{{- define "job.deploy.env" -}}
- name: KUBERNETES_NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
- name: BK_JOB_POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: BK_JOB_RELEASE_NAME
  value: {{ .Release.Name }}
{{- end -}}

{{/*
Return JVM Timezone Env
*/}}
{{- define "job.jvm.timezone.env" -}}
- name: BK_JOB_JVM_TIMEZONE
  value: {{ .Values.timezone.default.operation }}
{{- end -}}


{{/*
Return environment variables for a given micro service
*/}}
{{- define "job.service.extra.env" -}}
{{- $extraEnv := . -}}
{{- if $extraEnv -}}
{{- range $extraEnv -}}
- name: {{ .name }}
  value: {{ .value | quote }}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the Job Common Env Content
*/}}
{{- define "job.common.env" -}}
{{ include "job.storage.env" . }}
{{ include "job.config.env" . }}
{{ include "job.deploy.env" . }}
{{ include "job.jvm.timezone.env" . }}
{{- end -}}

{{/*
Return the Job Ingress Frontend TLS Config
*/}}
{{- define "job.ingress.frontend.tls" -}}
{{- if .Values.frontendConfig.ingress.tls -}}
tls: {{- include "common.tplvalues.render" ( dict "value" .Values.frontendConfig.ingress.tls "context" $) | nindent 0 -}}
{{- else -}}
tls:
- hosts:
    - {{ include "job.web.domain" . }}
  secretName: {{ include "common.names.fullname" . }}-ingress-tls
{{- end -}}
{{- end -}}


{{/*
Return the Job Service Probes Config
*/}}
{{- define "job.service.probes" -}}
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ .port }}
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 30
  successThreshold: 1
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ .port }}
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 20
  successThreshold: 1
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: {{ .port }}
  initialDelaySeconds: 10
  periodSeconds: 3
  timeoutSeconds: 3
  failureThreshold: 1
  successThreshold: 1
{{- end -}}


{{/*
是否连接归档库。deleteOnly 只删热库，不需要连归档库。
*/}}
{{- define "job.archiveMariadb.required" -}}
{{- if and .Values.backupConfig.archive.execute.enabled (or (eq .Values.backupConfig.archive.execute.mode "backupThenDelete") (eq .Values.backupConfig.archive.execute.mode "backupOnly")) -}}
true
{{- end -}}
{{- end -}}

{{/*
Return the Archive MariaDB secret name
*/}}
{{- define "job.archiveMariadb.secretName" -}}
{{ printf "%s-%s" (include "job.fullname" .) "archive-mariadb" }}
{{- end -}}

{{- define "job.archiveMariadb.username" -}}
{{- if eq (include "job.kms.credential.active" (dict "name" "archive-mariadb" "context" .)) "true" -}}
${archive-mariadb-username}
{{- else -}}
{{- .Values.backupConfig.archive.mariadb.username -}}
{{- end -}}
{{- end -}}

{{/*
Return the storage PVC name
*/}}
{{- define "job.storage.pvc.name" -}}
{{ printf "%s-pv-claim-%s" (include "common.names.fullname" .) .Values.persistence.storageClass }}
{{- end -}}

{{/*
Return the nfs PVC name, when enable external agent host to distribute files
*/}}
{{- define "job.execute.distribute.externalAgent.PVCName" -}}
{{ printf "%s-pv-claim-nfs-external-agent" (include "common.names.fullname" .) }}
{{- end -}}

{{/*
Return the job-execute or job-file-worker distribute volume
*/}}
{{- define "job.distribute.volume.name" -}}
{{- if .Values.externalGseAgent.enabled -}}
persistentVolumeClaim:
  claimName : {{ include "job.execute.distribute.externalAgent.PVCName" . }}
{{- else -}}
hostPath:
  path: {{ .Values.fileDistribute.hostPath }}
  type: DirectoryOrCreate
{{- end -}}
{{- end -}}

{{/*
Return the job pod terminationGracePeriodSeconds
*/}}
{{- define "job.podTerminationGracePeriodSeconds" -}}
terminationGracePeriodSeconds: {{ .Values.podTerminationGracePeriodSeconds }}
{{- end -}}

{{/*
Return the Crontab DB secret name
*/}}
{{- define "job.crontabdb.secretName" -}}
{{ printf "%s-%s" (include "job.fullname" .) "crontab-db" }}
{{- end -}}


{{/*
Return the job crontab database config
*/}}
{{- define "job.crontab.databaseConfig" -}}
{{- if .Values.crontabConfig.database.host -}}
jdbc-url: {{ include "job.jdbcMysqlScheme" . }}://{{- .Values.crontabConfig.database.host }}:{{- .Values.crontabConfig.database.port }}/job_crontab{{- .Values.crontabConfig.database.connection.properties }}
{{- if eq (include "job.kms.credential.active" (dict "name" "crontabdb" "context" .)) "true" }}
username: ${crontab-db-username}
{{- else }}
username: {{ .Values.crontabConfig.database.username }}
{{- end }}
password: ${crontab-db-password}
{{- else -}}
jdbc-url: {{ include "job.jdbcMysqlScheme" . }}://{{- include "job.mariadb.host" . }}:{{- include "job.mariadb.port" . }}/job_crontab{{ include "job.mariadb.connection.properties" . }}
username: {{ include "job.mariadb.username" . }}
    {{- if .Values.externalMariaDB.existingPasswordSecret }}
password: {{ .Values.externalMariaDB.existingPasswordKey | default "mariadb-password" | printf "${%s}" }}
    {{- else }}
password: ${mariadb-password}
    {{- end }}
{{- end }}
{{- end -}}

{{/*
Return the MariaDB certs volumeMount
*/}}
{{- define "job.mariadb.certsVolumeMount" -}}
{{- if and (not .Values.mariadb.enabled) (.Values.externalMariaDB.tls.enabled) -}}
- name: mariadb-certs
  mountPath: /etc/certs/mariadb
  readOnly: true
{{- end -}}
{{- end -}}

{{/*
Return the MariaDB certs volume
*/}}
{{- define "job.mariadb.certsVolume" -}}
{{- if and (not .Values.mariadb.enabled) (.Values.externalMariaDB.tls.enabled) -}}
- name: mariadb-certs
  secret:
    secretName: {{ .Values.externalMariaDB.tls.existingSecret }}
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB certs volumeMount
*/}}
{{- define "job.mongodb.certsVolumeMount" -}}
{{- if and (not .Values.mongodb.enabled) (.Values.externalMongoDB.tls.enabled) -}}
- name: mongodb-certs
  mountPath: /etc/certs/mongodb
  readOnly: true
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB certs volume
*/}}
{{- define "job.mongodb.certsVolume" -}}
{{- if and (not .Values.mongodb.enabled) (.Values.externalMongoDB.tls.enabled) -}}
- name: mongodb-certs
  secret:
    secretName: {{ .Values.externalMongoDB.tls.existingSecret }}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ certs volumeMount
*/}}
{{- define "job.rabbitmq.certsVolumeMount" -}}
{{- if and (not .Values.rabbitmq.enabled) (.Values.externalRabbitMQ.tls.enabled) -}}
- name: rabbitmq-certs
  mountPath: /etc/certs/rabbitmq
  readOnly: true
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ certs volume
*/}}
{{- define "job.rabbitmq.certsVolume" -}}
{{- if and (not .Values.rabbitmq.enabled) (.Values.externalRabbitMQ.tls.enabled) -}}
- name: rabbitmq-certs
  secret:
    secretName: {{ .Values.externalRabbitMQ.tls.existingSecret }}
{{- end -}}
{{- end -}}

{{/*
Return the Redis certs volumeMount
*/}}
{{- define "job.redis.certsVolumeMount" -}}
{{- if and (not .Values.redis.enabled) (.Values.externalRedis.tls.enabled) -}}
- name: redis-certs
  mountPath: /etc/certs/redis
  readOnly: true
{{- end -}}
{{- end -}}

{{/*
Return the Redis certs volume
*/}}
{{- define "job.redis.certsVolume" -}}
{{- if and (not .Values.redis.enabled) (.Values.externalRedis.tls.enabled) -}}
- name: redis-certs
  secret:
    secretName: {{ .Values.externalRedis.tls.existingSecret }}
{{- end -}}
{{- end -}}

{{/*
Return Extension volumeMounts
*/}}
{{- define "job.volumeExtension.volumeMounts" -}}
{{- $module := .module | default dict -}}
{{- $moduleVE := $module.volumeExtension | default dict -}}
{{- $moduleMounts := $moduleVE.volumeMounts | default list -}}
{{- $global := .global | default dict -}}
{{- $globalMounts := $global.volumeMounts | default list -}}
{{- if gt (len $moduleMounts) 0 -}}
{{- toYaml $moduleMounts -}}
{{- else if gt (len $globalMounts) 0 -}}
{{- toYaml $globalMounts -}}
{{- end -}}
{{- end }}

{{/*
Return Extension volumes
*/}}
{{- define "job.volumeExtension.volumes" -}}
{{- $module := .module | default dict -}}
{{- $moduleVE := $module.volumeExtension | default dict -}}
{{- $moduleVolumes := $moduleVE.volumes | default list -}}
{{- $global := .global | default dict -}}
{{- $globalVolumes := $global.volumes | default list -}}
{{- if gt (len $moduleVolumes) 0 -}}
{{- toYaml $moduleVolumes -}}
{{- else if gt (len $globalVolumes) 0 -}}
{{- toYaml $globalVolumes -}}
{{- end -}}
{{- end }}

{{/*
execute 的 mysql 整块输出。execute 凭证未接管时原样 toYaml。
接管后去掉分片 password：Java 读的是 spring.datasource.<名>，不是这段。
*/}}
{{- define "job.configmap.omitMysqlPasswords" -}}
{{- $mysql := .mysql -}}
{{- if eq (include "job.kms.credential.active" (dict "name" "execute" "context" .context)) "true" -}}
{{- $mysql = deepCopy $mysql -}}
{{- $vs := index $mysql "verticalSharding" | default dict -}}
{{- $dsMap := index $vs "datasource" | default dict -}}
{{- range $name, $ds := $dsMap -}}
{{- $_ := unset $ds "password" -}}
{{- end -}}
{{- end -}}
{{- toYaml $mysql -}}
{{- end -}}

{{/*
归档整块输出。对应凭证未接管时原样 toYaml。
archive-mariadb 接管后去掉归档库 password 和 TLS 口令；backup 接管后去掉热库分片 password。
*/}}
{{- define "job.configmap.omitArchiveSecrets" -}}
{{- $archive := .archive -}}
{{- $kmsArchive := eq (include "job.kms.credential.active" (dict "name" "archive-mariadb" "context" .context)) "true" -}}
{{- $kmsBackup := eq (include "job.kms.credential.active" (dict "name" "backup" "context" .context)) "true" -}}
{{- if or $kmsArchive $kmsBackup -}}
{{- $archive = deepCopy $archive -}}
{{- if $kmsArchive -}}
{{- $mariadb := index $archive "mariadb" | default dict -}}
{{- $_ := unset $mariadb "password" -}}
{{- $tls := index $mariadb "tls" | default dict -}}
{{- $_ := unset $tls "trustStorePassword" -}}
{{- $_ := unset $tls "keyStorePassword" -}}
{{- end -}}
{{- if $kmsBackup -}}
{{- $execute := index $archive "execute" | default dict -}}
{{- $executeMariadb := index $execute "mariadb" | default dict -}}
{{- $vs := index $executeMariadb "verticalSharding" | default dict -}}
{{- $dsMap := index $vs "datasource" | default dict -}}
{{- range $name, $ds := $dsMap -}}
{{- $_ := unset $ds "password" -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- toYaml $archive -}}
{{- end -}}
