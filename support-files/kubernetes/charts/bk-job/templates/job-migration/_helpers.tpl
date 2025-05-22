{{/*
Return the job-migration mysqlSchema sslMode
*/}}
{{- define "job.migration.mysqlSchema.sslModeOption" -}}
{{- if .Values.job.migration.mysqlSchema.tls.verifyHostname -}}
{{ printf "--ssl-verify-server-cert" }}
{{- else -}}
{{ printf "" }}
{{- end -}}
{{- end -}}

{{/*
Return the job-migration mysqlSchema certsDir
*/}}
{{- define "job.migration.mysqlSchema.certsDir" -}}
{{ printf "/etc/certs/mariadb" }}
{{- end -}}

{{/*
Return the job-migration mysqlSchema sslCA
*/}}
{{- define "job.migration.mysqlSchema.sslCA" -}}
{{ printf "%s/%s" (include "job.migration.mysqlSchema.certsDir" .) .Values.job.migration.mysqlSchema.tls.certCAFilename }}
{{- end -}}

{{/*
Return the job-migration mysqlSchema sslCert
*/}}
{{- define "job.migration.mysqlSchema.sslCert" -}}
{{ printf "%s/%s" (include "job.migration.mysqlSchema.certsDir" .) .Values.job.migration.mysqlSchema.tls.certFilename }}
{{- end -}}

{{/*
Return the job-migration mysqlSchema sslKey
*/}}
{{- define "job.migration.mysqlSchema.sslKey" -}}
{{ printf "%s/%s" (include "job.migration.mysqlSchema.certsDir" .) .Values.job.migration.mysqlSchema.tls.certKeyFilename }}
{{- end -}}

{{/*
Return the job-migration mysqlSchema tlsOptions
*/}}
{{- define "job.migration.mysqlSchema.tlsOptions" -}}
{{- $baseTlsOptions := printf "%s --ssl-ca=%s" (include "job.migration.mysqlSchema.sslModeOption" .) (include "job.migration.mysqlSchema.sslCA" .) -}}
{{- if .Values.job.migration.mysqlSchema.tls.certFilename -}}
{{- $mTlsOptions := printf "--ssl-cert=%s --ssl-key=%s" (include "job.migration.mysqlSchema.sslCert" .) (include "job.migration.mysqlSchema.sslKey" .) -}}
{{- printf "%s %s" $baseTlsOptions $mTlsOptions -}}
{{- else -}}
{{- printf "%s" $baseTlsOptions -}}
{{- end -}}
{{- end -}}
