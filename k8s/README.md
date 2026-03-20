# Kubernetes Config (mcart-product-search-service)

Apply resources in this order:

1. `namespace.yaml`
2. `configmap.yaml`
3. `secret-template.yaml` (replace placeholders first)
4. `deployment.yaml`
5. `service.yaml`
6. `ingress.yaml`

## Placeholder values to update

- `ES_URL` in `k8s/base/configmap.yaml`
- `ES_BASE64_API_KEY` in `k8s/base/secret-template.yaml`

The namespace for all resources is `mcart`.