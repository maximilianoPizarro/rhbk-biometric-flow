---
layout: default
title: RHBK NeuroFace Biometric Flow
---

<div class="hero">
  <img src="logo.png" alt="Keycloak" class="logo" />
  <h1>RHBK NeuroFace Biometric Flow</h1>
  <p class="subtitle">
    Red Hat Build of Keycloak with biometric facial recognition authentication via NeuroFace
  </p>
  <div class="badges">
    <span class="badge badge-red">RHBK 26.0</span>
    <span class="badge badge-blue">Keycloak SPI</span>
    <span class="badge badge-gold">Facial 2FA</span>
    <span class="badge badge-cyan">OpenShift</span>
  </div>
</div>

<div class="container">

<h2>Demo Videos</h2>

<div class="video-section">
<div class="video-grid">

<div class="video-card">
  <div class="video-wrapper shorts">
    <iframe src="https://www.youtube.com/embed/_PcsflxvJWY" title="RHBK Biometric Flow - Short Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
  </div>
  <div class="video-info">
    <h4>Biometric Authentication Flow</h4>
    <p>Quick demo of the delegated creation and 2FA facial recognition flow with RHBK and NeuroFace</p>
  </div>
</div>

<div class="video-card">
  <div class="video-wrapper">
    <iframe src="https://www.youtube.com/embed/lvFu5u7slXg" title="NeuroFace - Facial Recognition Usage" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
  </div>
  <div class="video-info">
    <h4>NeuroFace — Facial Recognition in Action</h4>
    <p>Full walkthrough of the NeuroFace webapp: training, recognition, and model configuration</p>
  </div>
</div>

</div>
</div>

<h2>Overview</h2>

<div class="card-grid">
<div class="card">
  <div class="icon">🔐</div>
  <h4>Delegated Creation + Biometric Enrollment</h4>
  <p>Admin creates users in Keycloak. On first login, users enroll their face via webcam — 3 to 5 captures from different angles sent to NeuroFace for model training.</p>
</div>
<div class="card">
  <div class="icon">👤</div>
  <h4>Second Factor Authentication (2FA)</h4>
  <p>After password login, users verify their identity through facial recognition. The SPI calls NeuroFace <code>/api/recognize</code> and matches against the enrolled profile.</p>
</div>
<div class="card">
  <div class="icon">📦</div>
  <h4>Single Helm Install</h4>
  <p>One <code>helm install</code> deploys both RHBK and NeuroFace in the same namespace with pre-configured realm, clients, flows, and roles.</p>
</div>
</div>

<h2>Architecture</h2>

<div class="arch-diagram"><pre>
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
</pre></div>

<h2>Screenshots</h2>

<div class="screenshot">
  <img src="neuroface.png" alt="NeuroFace Application" />
  <div class="caption">NeuroFace — Facial Recognition Webapp with ML (OpenCV LBPH / dlib)</div>
</div>

<div class="screenshot">
  <img src="topology.png" alt="OpenShift Topology" />
  <div class="caption">OpenShift Topology — RHBK + NeuroFace deployed in the same namespace</div>
</div>

<div class="screenshot">
  <img src="helm%20catalog.png" alt="Helm Catalog" />
  <div class="caption">Helm Chart Catalog — rhbk-neuroface available on Artifact Hub</div>
</div>

<div class="screenshot">
  <img src="helm%20catalog%20neuroface.png" alt="Helm Catalog NeuroFace" />
  <div class="caption">Helm Chart Catalog — NeuroFace dependency chart</div>
</div>

<h2>NeuroFace — Facial Recognition Service</h2>

<p>
  <a href="https://github.com/maximilianoPizarro/neuroface" target="_blank">NeuroFace</a> is a facial recognition webapp built with <strong>FastAPI</strong> and <strong>Angular 17</strong>, containerized with Red Hat UBI9 certified images. It provides the ML backend that powers the biometric authentication.
</p>

<h3>API Endpoints Used by the SPI</h3>

| Endpoint | Method | Usage |
|----------|--------|-------|
| `/api/health` | `GET` | Health check before biometric operations |
| `/api/images` | `POST` | Upload facial images during enrollment (multipart) |
| `/api/train` | `POST` | Train the recognition model after enrollment |
| `/api/recognize` | `POST` | Verify facial identity during 2FA login |
| `/api/labels` | `GET` | List registered biometric labels |

<h2>Authentication Flows</h2>

<h3>1. Delegated Creation with Biometric Enrollment</h3>

<div class="arch-diagram"><pre>
KC Admin ──► Creates user ──► Assigns Required Action "Biometric Enrollment"
                                          │
                                          ▼
                               User logs in with
                               temporary credentials
                                          │
                                          ▼
                               Webcam: captures 3-5 images
                               from different angles
                                          │
                                          ▼
                               SPI → POST /api/images (label=username)
                               SPI → POST /api/train
                                          │
                                          ▼
                               biometric_enrolled = true
                               User joins group "biometric-enrolled"
</pre></div>

<h3>2. Login with Biometric Second Factor (2FA)</h3>

<div class="arch-diagram"><pre>
User ──► Login page ──► username + password
                                │
                                ▼
                       Biometric verification (2FA)
                       Webcam captures facial image
                                │
                                ▼
                       SPI → POST /api/recognize { "image": base64 }
                                │
                                ▼
                       label == username AND
                       confidence >= threshold?
                          │              │
                         YES             NO
                          ▼              ▼
                       Access         Access
                       granted        denied
</pre></div>

<h2>Quick Start</h2>

<div class="install-box">
<h4>From Helm Repository</h4>

```bash
helm repo add rhbk-neuroface https://maximilianopizarro.github.io/rhbk-biometric-flow/
helm repo update

helm install rhbk-neuroface rhbk-neuroface/rhbk-neuroface \
  -n neuroface --create-namespace \
  --set admin.password=changeme
```
</div>

<div class="install-box">
<h4>From Source</h4>

```bash
git clone https://github.com/maximilianoPizarro/rhbk-biometric-flow.git
cd rhbk-biometric-flow

helm dependency update helm/rhbk-neuroface
helm install rhbk-neuroface ./helm/rhbk-neuroface \
  -n neuroface --create-namespace \
  --set admin.password=changeme
```
</div>

<h2>Helm Chart Values</h2>

<h3>RHBK (Keycloak)</h3>

| Parameter | Default | Description |
|-----------|---------|-------------|
| `rhbk.image.repository` | `registry.redhat.io/rhbk/keycloak-rhel9` | RHBK image |
| `rhbk.image.tag` | `26.0` | Image tag |
| `rhbk.replicas` | `1` | Replicas |
| `rhbk.resources.limits.cpu` | `1` | CPU limit |
| `rhbk.resources.limits.memory` | `1Gi` | Memory limit |

<h3>Admin & Realm</h3>

| Parameter | Default | Description |
|-----------|---------|-------------|
| `admin.username` | `admin` | Bootstrap admin user |
| `admin.password` | `admin` | Bootstrap admin password |
| `realm.name` | `neuroface` | Realm name |
| `realm.displayName` | `NeuroFace Biometric` | Display name |

<h3>Biometric Settings</h3>

| Parameter | Default | Description |
|-----------|---------|-------------|
| `biometric.confidenceThreshold` | `65.0` | Minimum confidence (0-100) |
| `biometric.maxEnrollmentImages` | `5` | Max enrollment images |
| `biometric.webcamWidth` | `640` | Webcam width (px) |
| `biometric.webcamHeight` | `480` | Webcam height (px) |

<h3>SPI Image</h3>

| Parameter | Default | Description |
|-----------|---------|-------------|
| `spi.image.repository` | `quay.io/maximilianopizarro/rhbk-neuroface-spi` | SPI image |
| `spi.image.tag` | `latest` | Tag |

<h3>NeuroFace Subchart Overrides</h3>

| Parameter | Default | Description |
|-----------|---------|-------------|
| `neuroface.enabled` | `true` | Deploy NeuroFace subchart |
| `neuroface.backend.image.tag` | `latest` | Backend image tag |
| `neuroface.backend.replicas` | `1` | Backend replicas |
| `neuroface.backend.aiModel` | `lbph` | AI model (`lbph` / `dlib`) |
| `neuroface.frontend.image.tag` | `latest` | Frontend image tag |
| `neuroface.frontend.replicas` | `1` | Frontend replicas |
| `neuroface.chat.enabled` | `true` | Enable AI chat feature |
| `neuroface.persistence.enabled` | `true` | Enable persistent storage |
| `neuroface.persistence.size` | `1Gi` | PVC size |
| `neuroface.route.enabled` | `true` | Create NeuroFace Route |

<h3>Route & Service</h3>

| Parameter | Default | Description |
|-----------|---------|-------------|
| `route.enabled` | `true` | Create RHBK OpenShift Route |
| `route.tls.termination` | `edge` | TLS termination |
| `service.type` | `ClusterIP` | Service type |
| `service.httpPort` | `8080` | HTTP port |
| `service.port` | `8443` | HTTPS port |

<h2>Realm Configuration</h2>

The chart auto-imports a pre-configured realm:

| Component | Details |
|-----------|---------|
| **Clients** | `neuroface-app` (public, PKCE S256), `neuroface-backend` (bearer-only) |
| **Browser Flow** | `biometric browser` — cookie OR (password + facial 2FA) |
| **Registration Flow** | `biometric registration` — delegated creation |
| **Required Action** | `biometric-enrollment` — facial enrollment on first login |
| **Roles** | `biometric-user`, `biometric-admin` |
| **Group** | `biometric-enrolled` — auto-assigned after enrollment |

<h2>SPI Components</h2>

| Provider | Type | ID | Description |
|----------|------|----|-------------|
| BiometricAuthenticator | Authenticator | `biometric-authenticator` | 2FA via NeuroFace `/api/recognize` |
| BiometricEnrollment | Required Action | `biometric-enrollment` | Multi-image facial enrollment |
| NeuroFaceClient | Internal | — | HTTP client for NeuroFace REST API |

<h2>Links</h2>

<div class="card-grid">
<div class="card">
  <h4>📂 Source Code</h4>
  <p><a href="https://github.com/maximilianoPizarro/rhbk-biometric-flow">github.com/maximilianoPizarro/rhbk-biometric-flow</a></p>
</div>
<div class="card">
  <h4>🧠 NeuroFace</h4>
  <p><a href="https://github.com/maximilianoPizarro/neuroface">github.com/maximilianoPizarro/neuroface</a></p>
</div>
<div class="card">
  <h4>📖 RHBK Docs</h4>
  <p><a href="https://docs.redhat.com/en/documentation/red_hat_build_of_keycloak/26.0/">Red Hat Build of Keycloak 26.0 Documentation</a></p>
</div>
</div>

</div>
