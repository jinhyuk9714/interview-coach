#!/bin/bash
set -euo pipefail

# Interview Coach - Local Kubernetes Deployment Script (minikube)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(cd "$K8S_DIR/../.." && pwd)"
OVERLAY="${1:-dev}"

echo "=========================================="
echo " Interview Coach - K8s Local Deploy"
echo " Overlay: $OVERLAY"
echo "=========================================="

# 1. Check prerequisites
echo ""
echo "[1/6] Checking prerequisites..."

if ! command -v minikube &>/dev/null; then
  echo "ERROR: minikube is not installed."
  echo "  Install: brew install minikube"
  exit 1
fi

if ! command -v kubectl &>/dev/null; then
  echo "ERROR: kubectl is not installed."
  echo "  Install: brew install kubectl"
  exit 1
fi

# 2. Start minikube
echo ""
echo "[2/6] Starting minikube..."

if ! minikube status | grep -q "Running"; then
  minikube start --cpus=4 --memory=8192 --driver=docker
  echo "minikube started."
else
  echo "minikube is already running."
fi

# 3. Enable ingress addon
echo ""
echo "[3/6] Enabling ingress addon..."
minikube addons enable ingress 2>/dev/null || true

# 4. Build images inside minikube
echo ""
echo "[4/6] Building Docker images inside minikube..."

eval $(minikube docker-env)

echo "  Building backend services..."
docker build -t ghcr.io/interview-coach/gateway:latest \
  --target gateway -f "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"

docker build -t ghcr.io/interview-coach/user-service:latest \
  --target user-service -f "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"

docker build -t ghcr.io/interview-coach/question-service:latest \
  --target question-service -f "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"

docker build -t ghcr.io/interview-coach/interview-service:latest \
  --target interview-service -f "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"

docker build -t ghcr.io/interview-coach/feedback-service:latest \
  --target feedback-service -f "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"

echo "  Building frontend..."
docker build -t ghcr.io/interview-coach/frontend:latest \
  --build-arg NEXT_PUBLIC_API_URL=http://gateway:8080 \
  -f "$PROJECT_ROOT/frontend/web/Dockerfile" "$PROJECT_ROOT/frontend/web"

# 5. Apply Kustomize
echo ""
echo "[5/6] Applying Kubernetes manifests (overlay: $OVERLAY)..."
kubectl apply -k "$K8S_DIR/overlays/$OVERLAY"

# 6. Wait for pods
echo ""
echo "[6/6] Waiting for all pods to be ready..."
kubectl -n interview-coach wait --for=condition=ready pod --all --timeout=300s 2>/dev/null || true

echo ""
echo "=========================================="
echo " Deployment complete!"
echo "=========================================="
echo ""
echo " Pod status:"
kubectl get pods -n interview-coach
echo ""
echo " Services:"
kubectl get svc -n interview-coach
echo ""

MINIKUBE_IP=$(minikube ip)
echo " Access URLs:"
echo "   minikube tunnel (run in another terminal): minikube tunnel"
echo "   Frontend: http://interview-coach.local"
echo "   API:      http://interview-coach.local/api/v1/auth/login"
echo ""
echo " Add to /etc/hosts:"
echo "   $MINIKUBE_IP interview-coach.local"
echo ""
