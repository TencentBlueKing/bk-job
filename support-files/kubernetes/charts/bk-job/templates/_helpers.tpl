{{/*
Create the name of the service account to use
*/}}
{{- define "bk-job.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "common.names.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the proper job-gateway image name
*/}}
{{- define "job-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-gateway.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-manage image name
*/}}
{{- define "job-manage.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-manage.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-execute image name
*/}}
{{- define "job-execute.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-execute.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-crontab image name
*/}}
{{- define "job-crontab.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-crontab.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-logsvr image name
*/}}
{{- define "job-logsvr.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-logsvr.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-backup image name
*/}}
{{- define "job-backup.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-backup.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper job-analysis image name
*/}}
{{- define "job-analysis.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.job-analysis.image "global" .Values.global) }}
{{- end -}}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "job.imagePullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.image .Values.job-gateway.image .Values.job-manage.image .Values.job-execute.image .Values.job-crontab.image .Values.job-logsvr.image .Values.job-backup.image .Values.job-analysis.image) "global" .Values.global) }}
{{- end -}}


