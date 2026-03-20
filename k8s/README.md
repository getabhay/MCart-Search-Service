# Kubernetes Deployment Runbook (mcart demo)

This guide covers full setup from AWS EC2 creation to CI/CD deployment and verification for both services in namespace `mcart`.

## 1) Create EC2 Instance (AWS)

Use these settings:

- AMI: `Amazon Linux 2023`
- Instance type: `t3.small` (recommended minimum for k3s + 2 Java services)
- Storage: `8 GiB` to start (increase to `20+ GiB` if needed)
- Public IP: `Enabled`
- Key pair: create/download `.pem`

Security Group inbound rules:

- `22` (SSH) from `0.0.0.0/0` for quick demo (tighten later)
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
sudo k3s kubectl get nodes -o wide
sudo k3s kubectl get pods -A
```

## 4) Add swap (recommended)

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

Run from local PowerShell:

```powershell
$KEY = "C:\\path\\to\\your-key.pem"
$HOST = "ec2-user@<EC2_PUBLIC_IP>"

scp -i $KEY "<local-path>\\mcart-product-service\\k8s\\base\\*" "$HOST:/opt/mcart-product-service/k8s/base/"
scp -i $KEY "<local-path>\\mcart-product-search-service\\k8s\\base\\*" "$HOST:/opt/mcart-product-search-service/k8s/base/"
```

## 7) Configure RDS connectivity

In AWS RDS Security Group:

1. Add inbound `PostgreSQL 5432`
2. Source = EC2 instance security group

Test from EC2:

```bash
sudo dnf install -y nmap-ncat
nc -vz <RDS_ENDPOINT> 5432
```

## 8) Apply namespace and config

```bash
sudo k3s kubectl apply --validate=false -f /opt/mcart-product-service/k8s/base/namespace.yaml
sudo k3s kubectl apply --validate=false -f /opt/mcart-product-service/k8s/base/configmap.yaml
```

## 9) Deploy both services

```bash
sudo k3s kubectl apply --validate=false -f /opt/mcart-product-service/k8s/base/deployment.yaml
sudo k3s kubectl apply --validate=false -f /opt/mcart-product-service/k8s/base/service.yaml
sudo k3s kubectl apply --validate=false -f /opt/mcart-product-search-service/k8s/base/deployment.yaml
sudo k3s kubectl apply --validate=false -f /opt/mcart-product-search-service/k8s/base/service.yaml
```

## 10) Deploy single ingress

Use one ingress for both routes:

- `/api/search` and `/api/search/products` -> search service
- all other paths `/` -> product service

Apply combined ingress:

```bash
sudo k3s kubectl apply --validate=false -f /tmp/mcart-ingress.yaml
```

If old ingresses exist, remove:

```bash
sudo k3s kubectl delete ingress mcart-product-service-ingress -n mcart --ignore-not-found
sudo k3s kubectl delete ingress mcart-product-search-service-ingress -n mcart --ignore-not-found
```

## 11) Elastic Cloud setup

Set:

- `ES_URL=https://<elastic-cloud-endpoint>:443`
- `ES_BASE64_API_KEY=<api-key-value>`

Verify from local:

```powershell
curl.exe -s "https://<elastic-cloud-endpoint>:443" -H "Authorization: ApiKey <ES_BASE64_API_KEY>"
```

## 12) GitHub Actions setup

In each repo -> `Settings` -> `Environments` -> `production`:

Add environment secrets:

- `EC2_HOST` = EC2 public IP
- `EC2_USER` = `ec2-user`
- `EC2_SSH_KEY` = private key content (`-----BEGIN...-----` to `-----END...-----`)
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `DB_PASSWORD`
- `ES_BASE64_API_KEY`

Branch protection on `main`:

- require pull request
- require status check `CI / build-and-test`

## 13) Current CD behavior

CD pipeline now:

- applies `namespace.yaml` and `configmap.yaml`
- creates/applies `mcart-shared-secrets` from GitHub environment secrets
- applies deployment/service/ingress
- updates image to `sha-${GITHUB_SHA}` and waits rollout

This means both ConfigMap and Secret values get refreshed by CD on merge to `main`.

## 14) Test deployment

1. Create a small commit and merge to `main`.
2. Wait for `CD` job success.
3. Verify:

```bash
sudo k3s kubectl get pods,svc,ingress -n mcart
sudo k3s kubectl get deploy -n mcart
```

From local:

```bash
curl -i http://<EC2_PUBLIC_IP>/actuator/health
curl -i http://<EC2_PUBLIC_IP>/api/search
curl -i http://<EC2_PUBLIC_IP>/api/search/products
```

## 15) Troubleshooting

If CD/apply fails with `TLS handshake timeout`:

```bash
free -h
sudo systemctl restart k3s
sleep 30
sudo k3s kubectl get nodes
sudo k3s kubectl get pods -A
```

If needed, reduce control-plane load:

```bash
sudo k3s kubectl -n kube-system scale deployment metrics-server --replicas=0
```

If app health is `DOWN` due to Elasticsearch:

- verify `ES_URL` and `ES_BASE64_API_KEY`
- restart deployments:

```bash
sudo k3s kubectl rollout restart deployment/mcart-product-service -n mcart
sudo k3s kubectl rollout restart deployment/mcart-product-search-service -n mcart
```
