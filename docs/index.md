# RHBK NeuroFace Biometric Flow

<p align="center">
  <img src="logo.png" alt="Keycloak Logo" width="128" />
</p>

<p align="center">
  <strong>Red Hat Build of Keycloak + NeuroFace Biometric Authentication</strong><br/>
  Helm chart &middot; v1.0.0 &middot; App version 26.0
</p>

<p align="center">
  <a href="https://github.com/maximilianoPizarro/rhbk-biometric-flow">GitHub</a> &middot;
  <a href="https://github.com/maximilianoPizarro/neuroface">NeuroFace</a> &middot;
  <a href="https://docs.redhat.com/en/documentation/red_hat_build_of_keycloak/26.0/">RHBK Docs</a>
</p>

---

## Overview

A Helm chart that deploys **Red Hat Build of Keycloak (RHBK) 26** with a custom SPI provider for **biometric facial recognition** powered by the [NeuroFace](https://github.com/maximilianoPizarro/neuroface) service.

The chart enables two key authentication scenarios:

| Scenario | Description |
|----------|-------------|
| **Delegated Creation with Biometric Enrollment** | An admin creates a user and assigns the biometric enrollment required action. On first login, the user captures facial images via webcam, which are sent to NeuroFace for training. |
| **Second Factor Authentication (2FA) via Facial Recognition** | After password-based authentication, the user is prompted for a webcam facial scan. The image is verified against the trained NeuroFace model. |

---

## Architecture

```
┌─────────────────────────────────┐     ┌──────────────────────────────────┐
│  RHBK (Keycloak 26 - UBI9)     │     │  NeuroFace Backend (FastAPI)     │
│                                 │     │                                  │
│  ┌───────────────────────────┐  │     │  POST /api/images   ← enrollment│
│  │ Biometric SPI (JAR)       │  │     │  POST /api/train    ← training  │
│  │                           │──┼─────┼─►POST /api/recognize ← verify   │
│  │ • BiometricAuthenticator  │  │     │  GET  /api/health   ← health    │
│  │   (2FA facial login)      │  │     │  GET  /api/labels   ← labels    │
│  │                           │  │     │                                  │
│  │ • BiometricEnrollment     │  │     └──────────────────────────────────┘
│  │   (delegated registration)│  │
│  └───────────────────────────┘  │     ┌──────────────────────────────────┐
│                                 │     │  NeuroFace Frontend (Angular 17) │
│  Realm: neuroface               │     │  ← Protected by OIDC client     │
│  Client: neuroface-app          │     │     "neuroface-app"              │
│  Flow: biometric browser        │     └──────────────────────────────────┘
│  Flow: biometric registration   │
└─────────────────────────────────┘
```

---

## Components

### Keycloak SPI Provider

The custom SPI JAR is loaded into RHBK via an init-container and provides:

| Provider | Type | ID | Description |
|----------|------|----|-------------|
| **BiometricAuthenticator** | Authenticator | `biometric-authenticator` | Second factor that verifies the user's face via NeuroFace `/api/recognize` |
| **BiometricEnrollment** | Required Action | `biometric-enrollment` | Captures multiple facial images and trains the NeuroFace model on first login |
| **NeuroFaceClient** | Internal | — | HTTP client that communicates with the NeuroFace REST API |

### Realm Configuration

The chart imports a pre-configured `neuroface` realm with:

- **Clients**: `neuroface-app` (public, PKCE S256) and `neuroface-backend` (bearer-only)
- **Authentication flows**: `biometric browser` (password + facial 2FA) and `biometric registration` (delegated creation)
- **Roles**: `biometric-user`, `biometric-admin`
- **Group**: `biometric-enrolled` (auto-assigned after successful enrollment)
- **Required action**: `biometric-enrollment` (assignable to users by admins)

### FreeMarker Templates

| Template | Screen | Description |
|----------|--------|-------------|
| `biometric-login.ftl` | 2FA Verification | Webcam capture with face-guide overlay, single-shot capture and submit |
| `biometric-enroll.ftl` | Enrollment | Multi-image capture (3-5 shots) with angle guidance, thumbnails, and progress counter |

---

## Quick Start

### Prerequisites

- OpenShift 4.x or Kubernetes 1.25+
- Helm 3.x
- NeuroFace deployed in the same namespace
- Red Hat registry access (`podman login registry.redhat.io`)

### Install

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

helm install rhbk-neuroface ./helm/rhbk-neuroface \
  -n neuroface --create-namespace \
  --set admin.password=changeme
```

---

## Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `rhbk.image.repository` | RHBK container image | `registry.redhat.io/rhbk/keycloak-rhel9` |
| `rhbk.image.tag` | Image tag | `26.0` |
| `admin.username` | Bootstrap admin username | `admin` |
| `admin.password` | Bootstrap admin password | `admin` |
| `realm.name` | Realm name | `neuroface` |
| `neuroface.backendService` | NeuroFace K8s service name | `neuroface-backend` |
| `neuroface.backendPort` | NeuroFace service port | `8080` |
| `biometric.confidenceThreshold` | Minimum confidence score (0-100) | `65.0` |
| `biometric.maxEnrollmentImages` | Max facial images during enrollment | `5` |
| `spi.image.repository` | SPI init-container image | `quay.io/maximilianopizarro/rhbk-neuroface-spi` |
| `route.enabled` | Create an OpenShift Route | `true` |

---

## NeuroFace API Integration

The SPI communicates with the NeuroFace backend over the internal Kubernetes network:

| Endpoint | Method | Used For |
|----------|--------|----------|
| `/api/health` | GET | Health check before any biometric operation |
| `/api/images` | POST | Upload facial images during enrollment (multipart) |
| `/api/train` | POST | Train the recognition model after enrollment |
| `/api/recognize` | POST | Verify facial identity during 2FA login |
| `/api/labels` | GET | List registered biometric labels |

---

## Links

- **Source code**: [github.com/maximilianoPizarro/rhbk-biometric-flow](https://github.com/maximilianoPizarro/rhbk-biometric-flow)
- **NeuroFace**: [github.com/maximilianoPizarro/neuroface](https://github.com/maximilianoPizarro/neuroface)
- **RHBK Documentation**: [docs.redhat.com/en/documentation/red_hat_build_of_keycloak/26.0](https://docs.redhat.com/en/documentation/red_hat_build_of_keycloak/26.0/)

## License

Apache License 2.0
