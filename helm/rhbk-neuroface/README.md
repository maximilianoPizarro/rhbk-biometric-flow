# rhbk-neuroface

![Version: 1.0.0](https://img.shields.io/badge/Version-1.0.0-informational?style=flat-square)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square)
![AppVersion: 26.0](https://img.shields.io/badge/AppVersion-26.0-informational?style=flat-square)

Red Hat Build of Keycloak with NeuroFace biometric SPI integration.

## Description

This Helm chart deploys **RHBK (Keycloak 26)** with a custom SPI that integrates with the [NeuroFace](https://github.com/maximilianoPizarro/neuroface) facial recognition service to provide:

- **Delegated user creation** with biometric facial enrollment
- **Second-factor authentication (2FA)** via facial recognition
- Pre-configured realm (`neuroface`) with authentication flows, clients, and roles

## Source Code

- <https://github.com/maximilianoPizarro/rhbk-biometric-flow>
- <https://github.com/maximilianoPizarro/neuroface>

## Dependencies

| Repository | Name | Version | Condition |
|------------|------|---------|-----------|
| <https://maximilianopizarro.github.io/neuroface/> | neuroface | 1.0.1 | `neuroface.enabled` |

The **neuroface** subchart is enabled by default and deploys the facial recognition backend and frontend in the same namespace. Set `neuroface.enabled=false` if NeuroFace is already deployed externally.

## Prerequisites

- OpenShift 4.x or Kubernetes 1.25+
- Helm 3.x
- Red Hat registry access (`podman login registry.redhat.io`) for the RHBK image
- NeuroFace backend reachable from within the cluster

## Installing the Chart

### From Helm repository

```bash
helm repo add rhbk-neuroface https://maximilianopizarro.github.io/rhbk-biometric-flow/
helm repo update

helm install rhbk-neuroface rhbk-neuroface/rhbk-neuroface \
  -n neuroface --create-namespace \
  --set admin.password=changeme
```

### From source

```bash
git clone https://github.com/maximilianoPizarro/rhbk-biometric-flow.git
cd rhbk-biometric-flow

helm dependency update helm/rhbk-neuroface
helm install rhbk-neuroface ./helm/rhbk-neuroface \
  -n neuroface --create-namespace \
  --set admin.password=changeme
```

## Uninstalling the Chart

```bash
helm uninstall rhbk-neuroface -n neuroface
```

---

## Values

### RHBK (Keycloak)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `rhbk.image.repository` | string | `registry.redhat.io/rhbk/keycloak-rhel9` | RHBK container image |
| `rhbk.image.tag` | string | `"26.0"` | Image tag |
| `rhbk.image.pullPolicy` | string | `IfNotPresent` | Image pull policy |
| `rhbk.replicas` | int | `1` | Number of Keycloak replicas |
| `rhbk.resources.limits.cpu` | string | `"1"` | CPU limit |
| `rhbk.resources.limits.memory` | string | `1Gi` | Memory limit |
| `rhbk.resources.requests.cpu` | string | `500m` | CPU request |
| `rhbk.resources.requests.memory` | string | `512Mi` | Memory request |

### Admin Credentials

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `admin.username` | string | `admin` | Bootstrap admin username |
| `admin.password` | string | `admin` | Bootstrap admin password — **change immediately** |
| `admin.temporary` | bool | `true` | Whether credentials are temporary |

### Realm

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `realm.name` | string | `neuroface` | Realm name |
| `realm.displayName` | string | `"NeuroFace Biometric"` | Realm display name in the login UI |

### NeuroFace Integration

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `neuroface.enabled` | bool | `true` | Deploy the NeuroFace subchart |
| `neuroface.backendService` | string | `neuroface-backend` | K8s service name of the NeuroFace backend |
| `neuroface.backendPort` | int | `8080` | NeuroFace backend port |
| `neuroface.internalUrl` | string | `""` | Full URL override (e.g. `http://my-svc:8080/api`). Auto-generated if empty |

### Biometric SPI

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `spi.image.repository` | string | `quay.io/maximilianopizarro/rhbk-neuroface-spi` | SPI init-container image |
| `spi.image.tag` | string | `latest` | SPI image tag |
| `spi.image.pullPolicy` | string | `Always` | SPI image pull policy |

### Biometric Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `biometric.confidenceThreshold` | float | `65.0` | Minimum confidence score (0-100) for facial match |
| `biometric.maxEnrollmentImages` | int | `5` | Max facial images during enrollment |
| `biometric.webcamWidth` | int | `640` | Webcam capture width (px) |
| `biometric.webcamHeight` | int | `480` | Webcam capture height (px) |

### OpenShift Route

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `route.enabled` | bool | `true` | Create an OpenShift Route |
| `route.host` | string | `""` | Custom hostname (auto-generated if empty) |
| `route.tls.termination` | string | `edge` | TLS termination type |
| `route.tls.insecureEdgeTerminationPolicy` | string | `Redirect` | Insecure traffic policy |

### Service

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `service.type` | string | `ClusterIP` | Kubernetes Service type |
| `service.port` | int | `8443` | HTTPS port |
| `service.httpPort` | int | `8080` | HTTP port |

### Other

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `persistence.enabled` | bool | `false` | Enable persistence (not used in dev mode) |
| `postgresql.enabled` | bool | `false` | Deploy a PostgreSQL instance |

---

## Overriding NeuroFace Subchart Values

Since `neuroface` is a Helm dependency, its values are overridden by nesting them under the `neuroface` key in your values file or via `--set`.

### Using `--set` flags

```bash
helm install rhbk-neuroface ./helm/rhbk-neuroface -n neuroface \
  --set neuroface.backend.image.tag=v1.2.0 \
  --set neuroface.backend.replicas=2 \
  --set neuroface.backend.aiModel=dlib \
  --set neuroface.backend.dlibEnabled=true \
  --set neuroface.frontend.replicas=2 \
  --set neuroface.persistence.size=5Gi \
  --set neuroface.chat.enabled=false \
  --set neuroface.route.enabled=true
```

### Using a custom values file

Create a `my-values.yaml`:

```yaml
admin:
  password: "s3cur3-p@ss"

biometric:
  confidenceThreshold: 70.0
  maxEnrollmentImages: 7

neuroface:
  enabled: true

  backend:
    image:
      repository: quay.io/maximilianopizarro/neuroface-backend
      tag: v1.2.0
    replicas: 2
    aiModel: lbph
    resources:
      limits:
        cpu: "1"
        memory: 1Gi
      requests:
        cpu: 200m
        memory: 512Mi

  frontend:
    image:
      repository: quay.io/maximilianopizarro/neuroface-frontend
      tag: v1.2.0
    replicas: 2

  chat:
    enabled: false

  persistence:
    enabled: true
    size: 5Gi
    storageClassName: gp3-csi

  route:
    enabled: true
    host: neuroface.apps.mycluster.example.com
```

Then install:

```bash
helm install rhbk-neuroface ./helm/rhbk-neuroface \
  -n neuroface --create-namespace \
  -f my-values.yaml
```

### Using an external NeuroFace deployment

If NeuroFace is already deployed in the namespace, disable the subchart and point to the existing service:

```bash
helm install rhbk-neuroface ./helm/rhbk-neuroface -n neuroface \
  --set neuroface.enabled=false \
  --set neuroface.backendService=my-neuroface-backend \
  --set neuroface.backendPort=8080
```

Or with a full URL override:

```bash
helm install rhbk-neuroface ./helm/rhbk-neuroface -n neuroface \
  --set neuroface.enabled=false \
  --set neuroface.internalUrl=http://neuroface-backend.other-ns.svc.cluster.local:8080/api
```

### All NeuroFace subchart values

These are the default values from the NeuroFace chart that can be overridden under the `neuroface.*` key:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `neuroface.backend.image.repository` | string | `quay.io/maximilianopizarro/neuroface-backend` | Backend image |
| `neuroface.backend.image.tag` | string | `latest` | Backend image tag |
| `neuroface.backend.image.pullPolicy` | string | `Always` | Backend pull policy |
| `neuroface.backend.replicas` | int | `1` | Backend replicas |
| `neuroface.backend.port` | int | `8080` | Backend container port |
| `neuroface.backend.aiModel` | string | `lbph` | AI model (`lbph` or `dlib`) |
| `neuroface.backend.dlibEnabled` | bool | `false` | Enable dlib model support |
| `neuroface.backend.resources.limits.cpu` | string | `500m` | Backend CPU limit |
| `neuroface.backend.resources.limits.memory` | string | `512Mi` | Backend memory limit |
| `neuroface.backend.resources.requests.cpu` | string | `100m` | Backend CPU request |
| `neuroface.backend.resources.requests.memory` | string | `256Mi` | Backend memory request |
| `neuroface.frontend.image.repository` | string | `quay.io/maximilianopizarro/neuroface-frontend` | Frontend image |
| `neuroface.frontend.image.tag` | string | `latest` | Frontend image tag |
| `neuroface.frontend.replicas` | int | `1` | Frontend replicas |
| `neuroface.frontend.resources.limits.cpu` | string | `200m` | Frontend CPU limit |
| `neuroface.frontend.resources.limits.memory` | string | `256Mi` | Frontend memory limit |
| `neuroface.chat.enabled` | bool | `true` | Enable AI chat feature |
| `neuroface.chat.modelEndpoint` | string | *(see neuroface chart)* | LLM inference endpoint |
| `neuroface.chat.modelName` | string | `isvc-granite-31-8b-fp8` | LLM model name |
| `neuroface.chat.maxTokens` | int | `512` | Max tokens for chat responses |
| `neuroface.persistence.enabled` | bool | `true` | Enable persistent storage |
| `neuroface.persistence.size` | string | `1Gi` | PVC size |
| `neuroface.persistence.storageClassName` | string | `""` | Storage class |
| `neuroface.route.enabled` | bool | `true` | Create OpenShift Route for NeuroFace |
| `neuroface.route.host` | string | `""` | Custom Route hostname |

---

## Authentication Flows

### Delegated Creation with Biometric Enrollment

1. Admin creates a user in the RHBK admin console (realm `neuroface`)
2. Admin assigns the required action **NeuroFace Biometric Enrollment**
3. Admin sets a temporary password
4. User logs in → prompted to capture 3-5 facial images via webcam
5. SPI sends images to NeuroFace `POST /api/images` and triggers `POST /api/train`
6. User attribute `biometric_enrolled=true` is set; user joins group `biometric-enrolled`

### Second Factor Authentication (2FA)

1. User enters username + password
2. Biometric verification screen appears with webcam
3. SPI sends captured image to NeuroFace `POST /api/recognize`
4. If the recognized label matches the username with confidence >= threshold → access granted

---

## Realm Configuration

The chart auto-imports a `neuroface` realm with:

| Component | Details |
|-----------|---------|
| **Clients** | `neuroface-app` (public, PKCE S256), `neuroface-backend` (bearer-only) |
| **Flows** | `biometric browser` (cookie OR password+2FA), `biometric registration` (delegated) |
| **Required Action** | `biometric-enrollment` |
| **Roles** | `biometric-user`, `biometric-admin` |
| **Group** | `biometric-enrolled` (auto-assigned after enrollment) |

---

## Maintainers

| Name | Email | URL |
|------|-------|-----|
| maximilianoPizarro | maximiliano.pizarro.5@gmail.com | <https://github.com/maximilianoPizarro> |
