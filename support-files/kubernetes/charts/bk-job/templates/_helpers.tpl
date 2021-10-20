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
Return the proper job-frontend image name
*/}}
{{- define "job-frontend.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.frontendConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-gateway image name
*/}}
{{- define "job-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.gatewayConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-manage image name
*/}}
{{- define "job-manage.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.manageConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-execute image name
*/}}
{{- define "job-execute.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.executeConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-crontab image name
*/}}
{{- define "job-crontab.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.crontabConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-logsvr image name
*/}}
{{- define "job-logsvr.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.logsvrConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-backup image name
*/}}
{{- define "job-backup.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.backupConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-analysis image name
*/}}
{{- define "job-analysis.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.analysisConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-file-gateway image name
*/}}
{{- define "job-file-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.fileGatewayConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-file-worker image name
*/}}
{{- define "job-file-worker.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.fileWorkerConfig.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper job-migration image name
*/}}
{{- define "job-migration.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.migration.image "global" .Values.image) }}
{{- end -}}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "job.imagePullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.gatewayConfig.image .Values.manageConfig.image .Values.executeConfig.image .Values.crontabConfig.image .Values.logsvrConfig.image .Values.backupConfig.image .Values.analysisConfig.image .Values.fileGatewayConfig.image .Values.fileWorkerConfig.image) "global" .Values.image) }}
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
    {{- printf "%d" (.Values.rabbitmq.service.port | int ) -}}
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
Return the MongoDB host
*/}}
{{- define "job.mongodb.host" -}}
{{- if .Values.mongodb.enabled }}
    {{- printf "%s" (include "job.mongodb.fullname" .) -}}
{{- else -}}
    {{- printf "%s" .Values.externalMongoDB.host -}}
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB Port
*/}}
{{- define "job.mongodb.port" -}}
{{- if .Values.mongodb.enabled }}
    {{- printf "27017" -}}
{{- else -}}
    {{- printf "%d" (.Values.externalMongoDB.port | int ) -}}
{{- end -}}
{{- end -}}

{{/*
Return the MongoDB database
*/}}
{{- define "job.mongodb.database" -}}
{{- if .Values.mongodb.enabled }}
    {{- printf "%s" .Values.mongodb.auth.database -}}
{{- else -}}
    {{- printf "%s" .Values.externalMongoDB.database -}}
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
{{- if .Values.externalMongoDB.existingPasswordSecret -}}
    {{- printf "%s" .Values.externalMongoDB.existingPasswordSecret -}}
{{- else if .Values.mongodb.enabled }}
    {{- printf "%s" (include "job.mongodb.fullname" .) -}}
{{- else -}}
    {{- printf "%s-%s" (include "job.fullname" .) "external-mongodb" -}}
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
  image: "groundnuty/k8s-wait-for:1.3"
  imagePullPolicy: IfNotPresent
  args:
  - "job"
  - {{ printf "%s-migration-%d" (include "common.names.fullname" .) .Release.Revision | quote }}
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
