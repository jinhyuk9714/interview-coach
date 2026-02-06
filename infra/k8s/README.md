# Kubernetes 배포 가이드

## 구조

```
infra/k8s/
├── base/                          # 공통 매니페스트
│   ├── kustomization.yaml         # 리소스 목록
│   ├── namespace.yaml             # interview-coach 네임스페이스
│   ├── configmap.yaml             # DB URL, Redis, 서비스 URL
│   ├── secret.yaml                # DB 비밀번호, JWT Secret, API 키
│   ├── ingress.yaml               # nginx, SSE proxy-buffering off
│   ├── network-policy.yaml        # gateway만 백엔드 접근
│   ├── gateway/                   # Deployment + Service
│   ├── user-service/              # Deployment + Service
│   ├── question-service/          # Deployment + Service
│   ├── interview-service/         # Deployment + Service
│   ├── feedback-service/          # Deployment + Service
│   ├── frontend/                  # Deployment + Service
│   ├── postgres/                  # StatefulSet + Service + init.sql
│   ├── redis/                     # Deployment + Service + PVC
│   └── chromadb/                  # Deployment + Service + PVC
├── overlays/
│   ├── dev/                       # 리소스 축소, replicas=1
│   └── prod/                      # HPA, replicas=2+
└── scripts/
    └── deploy-local.sh            # minikube 자동 배포
```

## 빠른 시작 (minikube)

```bash
# 로컬 배포 (dev overlay)
./infra/k8s/scripts/deploy-local.sh

# prod overlay로 배포
./infra/k8s/scripts/deploy-local.sh prod
```

## 수동 배포

```bash
# Dev 환경
kubectl apply -k infra/k8s/overlays/dev

# Prod 환경
kubectl apply -k infra/k8s/overlays/prod

# 상태 확인
kubectl get pods -n interview-coach
kubectl get svc -n interview-coach
```

## 리소스 할당

| 서비스 | Memory Req/Limit | CPU Req/Limit |
|--------|-----------------|---------------|
| gateway | 384Mi / 512Mi | 250m / 500m |
| user-service | 384Mi / 512Mi | 250m / 500m |
| question-service | 640Mi / 768Mi | 500m / 1000m |
| interview-service | 384Mi / 512Mi | 250m / 500m |
| feedback-service | 640Mi / 768Mi | 500m / 1000m |
| frontend | 128Mi / 256Mi | 100m / 250m |
| postgres | 512Mi / 1Gi | 250m / 500m |
| redis | 128Mi / 256Mi | 100m / 250m |
| chromadb | 256Mi / 512Mi | 100m / 500m |

## Health Probes

모든 백엔드 서비스는 Spring Boot Actuator 활용:
- **startupProbe**: 15s 초기, 5s 간격, 20회 실패 허용
- **readinessProbe**: 30s 초기, 10s 간격
- **livenessProbe**: 60s 초기, 15s 간격

## HPA (prod overlay)

gateway, question-service, feedback-service에 CPU 70% 기준 자동 스케일링:
- minReplicas: 2
- maxReplicas: 5

## 시크릿 관리

프로덕션에서는 `base/secret.yaml`의 값을 실제 시크릿으로 교체:

```bash
kubectl create secret generic interview-coach-secret \
  -n interview-coach \
  --from-literal=DATABASE_PASSWORD=<실제 비밀번호> \
  --from-literal=JWT_SECRET=<256비트 이상 키> \
  --from-literal=CLAUDE_API_KEY=<API 키> \
  --from-literal=OPENAI_API_KEY=<API 키>
```

## 트러블슈팅

```bash
# Pod 상태 확인
kubectl get pods -n interview-coach
kubectl describe pod <pod-name> -n interview-coach

# 로그 확인
kubectl logs -f <pod-name> -n interview-coach

# 서비스 간 통신 확인
kubectl exec -it <gateway-pod> -n interview-coach -- curl http://user-service:8081/actuator/health

# PVC 상태 확인
kubectl get pvc -n interview-coach

# 전체 삭제
kubectl delete -k infra/k8s/overlays/dev
```
