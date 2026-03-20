# Kubernetes Deployment Runbook (mcart demo)

This guide covers full setup from AWS EC2 creation to ingress verification for both services in namespace `mcart`.

## 1) Create EC2 Instance (AWS)

Use these settings:

- AMI: `Amazon Linux 2023`
- Instance type: `t3.micro` (demo)
- Storage: `8 GiB` (you can increase later)
- Public IP: `Enabled`
- Key pair: create/download `.pem`

Security Group inbound rules:

- `22` (SSH) from `My IP`
- `80` (HTTP) from `0.0.0.0/0`
- `443` (HTTPS) from `0.0.0.0/0`

## 2) Connect to Instance

```bash
ssh -i <path-to-key.pem> ec2-user@<EC2_PUBLIC_IP>
```

## 3) Install k3s

```bash
curl -sfL https://get.k3s.io | sh -
```

Prepare kubeconfig for `ec2-user`:

```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown ec2-user:ec2-user ~/.kube/config
chmod 600 ~/.kube/config
```

Verify:

```bash
sudo k3s kubectl get nodes
sudo k3s kubectl get pods -A
```

## 4) Stabilize small node (recommended for t3.micro)

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
grep -q '/swapfile' /etc/fstab || echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h
```

## 5) Prepare deployment folders on EC2

```bash
sudo mkdir -p /opt/mcart-product-service/k8s/base
sudo mkdir -p /opt/mcart-product-search-service/k8s/base
sudo chown -R ec2-user:ec2-user /opt/mcart-product-service /opt/mcart-product-search-service
```

## 6) Copy manifests from local machine to EC2

Run from your local machine (PowerShell), not on EC2:

```powershell
$KEY = "C:\\path\\to\\your-key.pem"
$HOST = "ec2-user@<EC2_PUBLIC_IP>"

scp -i $KEY "<local-path>\\mcart-product-service\\k8s\\base\\*" "$HOST:/opt/mcart-product-service/k8s/base/"
scp -i $KEY "<local-path>\\mcart-product-search-service\\k8s\\base\\*" "$HOST:/opt/mcart-product-search-service/k8s/base/"
```

## 7) Configure RDS connectivity (required before app start)

In AWS:

1. Open RDS instance SG inbound rules.
2. Add rule: `PostgreSQL 5432`.
3. Source: EC2 instance security group ID.

From EC2, verify DB port reachability:

```bash
sudo dnf install -y nmap-ncat
nc -vz mcart-db.cfaqum0egx32.ap-south-1.rds.amazonaws.com 5432
```

Expected: connection `succeeded`.

## 8) Apply namespace and shared config

```bash
sudo k3s kubectl apply -f /opt/mcart-product-service/k8s/base/namespace.yaml
sudo k3s kubectl apply -f /opt/mcart-product-service/k8s/base/configmap.yaml
```

## 9) Create shared secret

Replace placeholders with real values:

```bash
sudo k3s kubectl -n mcart create secret generic mcart-shared-secrets \
  --from-literal=AWS_ACCESS_KEY='<...>' \
  --from-literal=AWS_SECRET_KEY='<...>' \
  --from-literal=DB_PASSWORD='postgres' \
  --from-literal=ES_BASE64_API_KEY='<...>' \
  --dry-run=client -o yaml | sudo k3s kubectl apply -f -
```

## 10) Deploy both services

```bash
sudo k3s kubectl apply -f /opt/mcart-product-service/k8s/base/deployment.yaml
sudo k3s kubectl apply -f /opt/mcart-product-service/k8s/base/service.yaml

sudo k3s kubectl apply -f /opt/mcart-product-search-service/k8s/base/deployment.yaml
sudo k3s kubectl apply -f /opt/mcart-product-search-service/k8s/base/service.yaml
```

## 11) Deploy single ingress (no domain required)

Create one combined ingress:

```bash
cat > /tmp/mcart-ingress.yaml <<'EOF'
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mcart-ingress
  namespace: mcart
spec:
  ingressClassName: traefik
  rules:
    - http:
        paths:
          - path: /api/search/products
            pathType: Prefix
            backend:
              service:
                name: mcart-product-search-service
                port:
                  number: 8081
          - path: /api/search
            pathType: Prefix
            backend:
              service:
                name: mcart-product-search-service
                port:
                  number: 8081
          - path: /
            pathType: Prefix
            backend:
              service:
                name: mcart-product-service
                port:
                  number: 8080
EOF

sudo k3s kubectl apply -f /tmp/mcart-ingress.yaml
```

If old ingresses exist, remove them:

```bash
sudo k3s kubectl delete ingress mcart-product-service-ingress -n mcart --ignore-not-found
sudo k3s kubectl delete ingress mcart-product-search-service-ingress -n mcart --ignore-not-found
```

## 12) Verification

Cluster objects:

```bash
sudo k3s kubectl get nodes
sudo k3s kubectl get pods,svc,ingress -n mcart
```

Route verification from local machine:

```bash
curl -i http://<EC2_PUBLIC_IP>/api/search
curl -i http://<EC2_PUBLIC_IP>/api/search/products
curl -i http://<EC2_PUBLIC_IP>/actuator/health
```

## 13) Troubleshooting quick checks

Pod crash logs:

```bash
sudo k3s kubectl logs -n mcart deploy/mcart-product-service --previous --tail=200
sudo k3s kubectl logs -n mcart deploy/mcart-product-search-service --previous --tail=200
```

Rollout status:

```bash
sudo k3s kubectl rollout status deployment/mcart-product-service -n mcart
sudo k3s kubectl rollout status deployment/mcart-product-search-service -n mcart
```

If app health shows Elasticsearch DOWN, update:

- `ES_URL` in `k8s/base/configmap.yaml`
- `ES_BASE64_API_KEY` in `k8s/base/secret-template.yaml` (and recreate/apply secret)

Restart deployments after config/secret updates:

```bash
sudo k3s kubectl rollout restart deployment/mcart-product-service -n mcart
sudo k3s kubectl rollout restart deployment/mcart-product-search-service -n mcart
```

## 14) GitHub Actions prerequisites

In each repo settings:

- Create environment: `production` with required reviewers
- Add secrets:
  - `EC2_HOST` = `<EC2_PUBLIC_IP>`
  - `EC2_USER` = `ec2-user`
  - `EC2_SSH_KEY` = private key content
- Branch protection on `main`:
  - require PR
  - require status check `CI / build-and-test`