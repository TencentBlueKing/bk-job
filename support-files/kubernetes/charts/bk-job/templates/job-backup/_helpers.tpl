{{/*
Return the job-backup archive MariaDB jdbc connection url base properties (without ssl config)
*/}}
{{- define "job.backup.archive.mariadb.base.connection.properties" -}}
{{- printf "%s" .Values.backupConfig.archive.mariadb.connection.properties -}}
{{- end -}}

{{/*
Return the job-backup archive MariaDB jdbc connection sslMode
*/}}
{{- define "job.backup.archive.mariadb.sslMode" -}}
{{- if .Values.backupConfig.archive.mariadb.tls.verifyHostname }}
    {{- printf "VERIFY_IDENTITY" -}}
{{- else -}}
    {{- printf "VERIFY_CA" -}}
{{- end -}}
{{- end -}}

{{/*
Return the job-backup archive MariaDB trustStore password key
*/}}
{{- define "job.backup.archive.mariadb.trustStorePasswordKey" -}}
{{ printf "${archive-mariadb-truststore-password}" }}
{{- end -}}

{{/*
Return the job-backup archive MariaDB keyStore password key
*/}}
{{- define "job.backup.archive.mariadb.keyStorePasswordKey" -}}
{{ printf "${archive-mariadb-keystore-password}" }}
{{- end -}}

{{/*
Return the job-backup archive MariaDB jdbc connection ssl properties
*/}}
{{- define "job.backup.archive.mariadb.ssl.properties" -}}
{{- if (not .Values.backupConfig.archive.mariadb.tls.enabled) }}
    {{- printf "" -}}
{{- else -}}
    {{- if (not .Values.backupConfig.archive.mariadb.tls.keyStoreFilename) }}
        {{- printf "&sslMode=%s&trustCertificateKeyStoreType=%s&trustCertificateKeyStoreUrl=file:/etc/certs/archive-mariadb/%s&trustCertificateKeyStorePassword=%s" (include "job.backup.archive.mariadb.sslMode" .) .Values.backupConfig.archive.mariadb.tls.trustStoreType .Values.backupConfig.archive.mariadb.tls.trustStoreFilename (include "job.backup.archive.mariadb.trustStorePasswordKey" .) -}}
    {{- else -}}
        {{- printf "&sslMode=%s&trustCertificateKeyStoreType=%s&trustCertificateKeyStoreUrl=file:/etc/certs/archive-mariadb/%s&trustCertificateKeyStorePassword=%s&clientCertificateKeyStoreType=%s&clientCertificateKeyStoreUrl=file:/etc/certs/archive-mariadb/%s&clientCertificateKeyStorePassword=%s" (include "job.backup.archive.mariadb.sslMode" .) .Values.backupConfig.archive.mariadb.tls.trustStoreType .Values.backupConfig.archive.mariadb.tls.trustStoreFilename (include "job.backup.archive.mariadb.trustStorePasswordKey" .) .Values.backupConfig.archive.mariadb.tls.keyStoreType .Values.backupConfig.archive.mariadb.tls.keyStoreFilename (include "job.backup.archive.mariadb.keyStorePasswordKey" .) -}}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the job-backup archive MariaDB jdbc connection url properties
*/}}
{{- define "job.backup.archive.mariadb.connection.properties" -}}
{{- $baseProps := include "job.backup.archive.mariadb.base.connection.properties" . -}}
{{- $sslProps := include "job.backup.archive.mariadb.ssl.properties" . -}}
{{- printf "%s%s" $baseProps $sslProps -}}
{{- end -}}



{{/*
Return the job-backup archive execute MariaDB jdbc connection url base properties (without ssl config)
*/}}
{{- define "job.backup.archive.execute.mariadb.base.connection.properties" -}}
{{- printf "%s" .Values.backupConfig.archive.execute.mariadb.connection.properties -}}
{{- end -}}

{{/*
Return the job-backup archive execute MariaDB jdbc connection url properties
*/}}
{{- define "job.backup.archive.execute.mariadb.connection.properties" -}}
{{- $baseProps := include "job.backup.archive.execute.mariadb.base.connection.properties" . -}}
{{- $sslProps := include "job.mariadb.ssl.properties" . -}}
{{- printf "%s%s" $baseProps $sslProps -}}
{{- end -}}


{{/*
Return the job-backup archive MariaDB certs volumeMount
*/}}
{{- define "job.backup.archive.mariadb.certsVolumeMount" -}}
{{- if .Values.backupConfig.archive.mariadb.tls.enabled -}}
- name: archive-mariadb-certs
  mountPath: /etc/certs/archive-mariadb
  readOnly: true
{{- end -}}
{{- end -}}

{{/*
Return the job-backup archive MariaDB certs volume
*/}}
{{- define "job.backup.archive.mariadb.certsVolume" -}}
{{- if .Values.backupConfig.archive.mariadb.tls.enabled -}}
- name: archive-mariadb-certs
  secret:
    secretName: {{ .Values.backupConfig.archive.mariadb.tls.existingSecret }}
{{- end -}}
{{- end -}}
