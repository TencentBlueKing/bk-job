{{/*
Create the name of the service account to use
*/}}
{{- define "job-execute.serviceAccountName" -}}
{{- if .Values.execute.serviceAccount.create }}
{{ default (printf "%s-execute" (include "common.names.fullname" .)) .Values.execute.serviceAccount.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- default "default" .Values.execute.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the proper job-gateway image name
*/}}
{{- define "job-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.gateway.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-manage image name
*/}}
{{- define "job-manage.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.manage.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-execute image name
*/}}
{{- define "job-execute.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.execute.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-crontab image name
*/}}
{{- define "job-crontab.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.crontab.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-logsvr image name
*/}}
{{- define "job-logsvr.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.logsvr.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-backup image name
*/}}
{{- define "job-backup.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.backup.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-analysis image name
*/}}
{{- define "job-analysis.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.analysis.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "job.imagePullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.gateway.image .Values.manage.image .Values.execute.image .Values.crontab.image .Values.logsvr.image .Values.backup.image .Values.analysis.image) "global" .Values.global) }}
{{- end -}}


