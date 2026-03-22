# RHBK NeuroFace Biometric Flow

Helm chart for **Red Hat Build of Keycloak (RHBK)** with biometric facial recognition integration via [NeuroFace](https://github.com/maximilianoPizarro/neuroface).

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

## Authentication Flows

### 1. Delegated Creation with Biometric Enrollment

```
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
                               SPI sends images to NeuroFace
                               POST /api/images (label=username)
                                          │
                                          ▼
                               SPI calls POST /api/train
                               Model trained with new user
                                          │
                                          ▼
                               Attribute biometric_enrolled=true
                               User added to "biometric-enrolled" group
```

### 2. Login with Biometric Second Factor (2FA)

```
User ──► Login page ──► username + password
                                │
                                ▼
                       Biometric verification (2FA)
                       Webcam: captures facial image
                                │
                                ▼
                       SPI sends to NeuroFace
                       POST /api/recognize { "image": base64 }
                                │
                                ▼
                       label == username AND
                       confidence >= threshold?
                          │              │
                         YES             NO
                          │              │
                          ▼              ▼
                       Access         Access
                       granted        denied
```

---

## Prerequisites

- **OpenShift** 4.x / Kubernetes 1.25+
- **Helm** 3.x
- **Podman** (to build the SPI image)
- **NeuroFace** deployed in the same namespace
- **Red Hat registry access** (`podman login registry.redhat.io`)

---

## Deployment

### 1. Deploy NeuroFace (same namespace)

```bash
helm install neuroface ./path-to/neuroface/helm/neuroface -n neuroface --create-namespace
```

### 2. Build the SPI image

```bash
./build-spi.sh quay.io maximilianopizarro latest
```

### 3. Deploy RHBK with Helm

```bash
helm install rhbk-neuroface ./helm/rhbk-neuroface \
  -n neuroface \
  --set admin.username=admin \
  --set admin.password=changeme \
  --set neuroface.backendService=neuroface-backend \
  --set biometric.confidenceThreshold=65.0
```

### 4. Verify the deployment

```bash
kubectl get pods -n neuroface

kubectl get route -n neuroface
```

---

## Scenario Configuration

### Create a user with biometric enrollment (Delegated Creation)

1. Access the RHBK admin console: `https://<route>/admin`
2. Select the **neuroface** realm
3. Go to **Users -> Add user**
4. Fill in user details (username, email, etc.)
5. Under **Required User Actions**, select **NeuroFace Biometric Enrollment**
6. Set a temporary password in the **Credentials** tab
7. On first login the user will be redirected to biometric enrollment

### User flow

1. User accesses the Keycloak-protected application
2. Enters username and temporary password
3. The **Biometric Enrollment** screen is presented:
   - The webcam activates automatically
   - User must capture at least 3 facial images from different angles
   - Images are sent to NeuroFace for model training
4. On subsequent logins, after entering the password the **Biometric Verification** screen appears:
   - The webcam captures a single image
   - The image is verified against the NeuroFace trained model
   - If the match confidence meets the threshold, access is granted

---

## Helm Chart Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `rhbk.image.repository` | RHBK container image | `registry.redhat.io/rhbk/keycloak-rhel9` |
| `rhbk.image.tag` | Image tag | `26.0` |
| `admin.username` | Temporary admin username | `admin` |
| `admin.password` | Temporary admin password | `admin` |
| `realm.name` | Realm name | `neuroface` |
| `neuroface.backendService` | K8s service name for the NeuroFace backend | `neuroface-backend` |
| `neuroface.backendPort` | Backend service port | `8080` |
| `biometric.confidenceThreshold` | Minimum confidence score (0-100) | `65.0` |
| `biometric.maxEnrollmentImages` | Max images during enrollment | `5` |
| `spi.image.repository` | SPI init-container image | `quay.io/maximilianopizarro/rhbk-neuroface-spi` |
| `route.enabled` | Create an OpenShift Route | `true` |

---

## Project Structure

```
rhbk-biometric-flow/
├── helm/rhbk-neuroface/           # RHBK Helm chart
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│       ├── _helpers.tpl
│       ├── deployment.yaml        # RHBK + SPI init-container
│       ├── service.yaml
│       ├── route.yaml             # OpenShift Route
│       ├── secret-admin.yaml      # Admin credentials
│       ├── configmap-env.yaml     # NeuroFace config
│       ├── configmap-realm.yaml   # Realm import JSON
│       └── NOTES.txt
├── spi/                           # Keycloak SPI Provider
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/.../keycloak/
│       │   ├── BiometricAuthenticator.java
│       │   ├── BiometricAuthenticatorFactory.java
│       │   ├── BiometricEnrollmentRequiredAction.java
│       │   ├── BiometricEnrollmentRequiredActionFactory.java
│       │   └── NeuroFaceClient.java
│       └── resources/
│           ├── META-INF/services/     # SPI service loaders
│           └── theme-resources/
│               └── templates/
│                   ├── biometric-login.ftl   # 2FA webcam UI
│                   └── biometric-enroll.ftl  # Enrollment webcam UI
├── build-spi.sh                   # Build + push SPI image
└── README.md
```

---

## NeuroFace API Endpoints Used by the SPI

| Endpoint | Method | Usage |
|----------|--------|-------|
| `/api/health` | GET | Health check before operations |
| `/api/images` | POST | Upload facial images during enrollment |
| `/api/train` | POST | Train the model after enrollment |
| `/api/recognize` | POST | Verify facial identity during 2FA login |
| `/api/labels` | GET | Check registered labels |

---

## License

See repository license file.
