#!/bin/bash
set -euo pipefail

REGISTRY="${1:-quay.io}"
NAMESPACE="${2:-maximilianopizarro}"
IMAGE_NAME="rhbk-neuroface-spi"
TAG="${3:-latest}"
FULL_IMAGE="${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${TAG}"

echo "=== Building RHBK NeuroFace Biometric SPI ==="
echo "Image: ${FULL_IMAGE}"

cd "$(dirname "$0")/spi"

echo "--- Building container image ---"
podman build -t "${FULL_IMAGE}" .

echo "--- Pushing to registry ---"
podman push "${FULL_IMAGE}"

echo "=== Done ==="
echo "Image pushed: ${FULL_IMAGE}"
echo ""
echo "Update helm/rhbk-neuroface/values.yaml:"
echo "  spi.image.repository: ${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}"
echo "  spi.image.tag: ${TAG}"
