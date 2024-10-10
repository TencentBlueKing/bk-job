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
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "job.imagePullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.gatewayConfig.image .Values.manageConfig.image .Values.executeConfig.image .Values.crontabConfig.image .Values.logsvrConfig.image .Values.backupConfig.image .Values.analysisConfig.image .Values.fileGatewayConfig.image .Values.fileWorkerConfig.image) "global" .Values.global) }}
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
{{- if .Values.mariadb.enabled }}
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
Return the MariaDB jdbc connection url properties
*/}}
{{- define "job.mariadb.connection.properties" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.connection.properties -}}
{{- else -}}
    {{- printf "%s" .Values.externalMariaDB.connection.properties -}}
{{- end -}}
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
{{- end -}}
{{- else }}
password: {{ .Values.externalRedis.existingPasswordKey | default "redis-password" | printf "${%s}" }}
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
{{- define "job.rabbitmq.username" -}}
{{- if .Values.rabbitmq.enabled }}
    {{- printf "%s" .Values.rabbitmq.auth.username -}}
{{- else -}}
    {{- printf "%s" .Values.externalRabbitMQ.username -}}
{{- end -}}
{{- end -}}

{{/*
Return the RabbitMQ secret name
*/}}
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
  {{- printf "mongodb://%s:%s@%s/?authSource=%s" (include "job.mongodb.username" .) (printf "${%s}" "mongodb-passwords") (include "job.mongodb.hostsAndPorts" .) (include "job.mongodb.authenticationDatabase" .) -}}
{{- else -}}
  {{- if .Values.externalMongoDB.uri -}}
    {{- printf "%s" .Values.externalMongoDB.uri -}}
  {{- else -}}
    {{- $uri := (printf "mongodb://%s:%s@%s/?authSource=%s" (include "job.mongodb.username" .) (.Values.externalMongoDB.existingPasswordKey | default "mongodb-password" | printf "${%s}" ) (include "job.mongodb.hostsAndPorts" .) (include "job.mongodb.authenticationDatabase" .)) -}}
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
Return the Job Profile
*/}}
{{- define "job.profile" -}}
    {{- printf "%s" .Values.job.profile -}}
{{- end -}}

{{/*
Return the Job InitContainer WaitForMigration Content
*/}}
{{- define "job.initContainer.waitForMigration" -}}
- name: "migration-init"
  image: {{ include "common.images.image" (dict "imageRoot" .Values.waitForMigration.image "global" .Values.global) }}
  imagePullPolicy: {{ .Values.waitForMigration.image.pullPolicy }}
  resources:
    {{- toYaml .Values.waitForMigration.resources | nindent 4 }}
  args:
  - "job-wr"
  - {{ printf "%s-migration-%s" (include "common.names.fullname" .) .Chart.Version | quote }}
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
{{- end -}}
{{- end -}}

{{/*
Return the gse secret
*/}}
{{- define "gse.secretName" -}}
{{- if .Values.gse.existingTlsSecret -}}
    {{- printf "%s" .Values.gse.existingTlsSecret -}}
{{- else -}}
    {{ printf "%s-gse-%s" (include "common.names.fullname" .) "tls-cert" }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Web Scheme
*/}}
{{- define "job.web.scheme" -}}
    {{- printf "%s" .Values.bkDomainScheme -}}
{{- end -}}

{{/*
Return the Job Web URL
*/}}
{{- define "job.web.url" -}}
{{- if .Values.job.web.extraWebUrls -}}
{{ printf "%s://%s" (include "job.web.scheme" .) .Values.job.web.domain }}{{ printf ",%s" .Values.job.web.extraWebUrls }}
{{- else -}}
{{ printf "%s://%s" (include "job.web.scheme" .) .Values.job.web.domain }}
{{- end -}}
{{- end -}}

{{/*
Return the Job Web API URL
*/}}
{{- define "job.web.api.url" -}}
{{ printf "%s://%s" (include "job.web.scheme" .) .Values.job.web.domain }}
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
    - {{ .Values.job.web.domain }}
  secretName: {{ include "common.names.fullname" . }}-ingress-tls
{{- end -}}
{{- end -}}

{{/*
Return the Job Ingress Gateway TLS Config
*/}}
{{- define "job.ingress.gateway.tls" -}}
{{- if .Values.gatewayConfig.ingress.tls -}}
tls: {{- include "common.tplvalues.render" ( dict "value" .Values.gatewayConfig.ingress.tls "context" $) | nindent 0 -}}
{{- else -}}
tls:
- hosts:
    - {{ .Values.job.web.apiDomain }}
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
Return the Archive MariaDB secret name
*/}}
{{- define "job.archiveMariadb.secretName" -}}
{{ printf "%s-%s" (include "job.fullname" .) "archive-mariadb" }}
{{- end -}}

{{/*
Return the storage PVC name
*/}}
{{- define "job.storage.pvc.name" -}}
{{ printf "%s-pv-claim-%s" (include "common.names.fullname" .) .Values.persistence.storageClass }}
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
username: {{ .Values.crontabConfig.database.username }}
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
