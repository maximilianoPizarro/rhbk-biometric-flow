{{/*
Expand the name of the chart.
*/}}
{{- define "rhbk-neuroface.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "rhbk-neuroface.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "rhbk-neuroface.labels" -}}
helm.sh/chart: {{ include "rhbk-neuroface.name" . }}
{{ include "rhbk-neuroface.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.rhbk.image.tag | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "rhbk-neuroface.selectorLabels" -}}
app.kubernetes.io/name: {{ include "rhbk-neuroface.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
NeuroFace backend service name.
When the subchart is enabled, the service is named "<release>-backend"
(the subchart's fullname helper sees "neuroface" is contained in the release name).
When disabled, use the explicit backendService value.
*/}}
{{- define "rhbk-neuroface.neurofaceBackendService" -}}
{{- if and .Values.neuroface.enabled (not .Values.neuroface.internalUrl) }}
{{- printf "%s-backend" .Release.Name }}
{{- else }}
{{- .Values.neuroface.backendService }}
{{- end }}
{{- end }}

{{/*
NeuroFace backend internal URL
*/}}
{{- define "rhbk-neuroface.neurofaceUrl" -}}
{{- if .Values.neuroface.internalUrl }}
{{- .Values.neuroface.internalUrl }}
{{- else }}
{{- printf "http://%s:%d/api" (include "rhbk-neuroface.neurofaceBackendService" .) (int .Values.neuroface.backendPort) }}
{{- end }}
{{- end }}
