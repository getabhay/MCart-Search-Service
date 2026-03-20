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

## GitHub Actions and Branch Protection

If you do not see `build-and-test` in branch protection checks yet, that is expected until CI runs once.

1. Push workflow files to `main`:
   - `.github/workflows/ci.yml`
   - `.github/workflows/cd.yml`
2. Open GitHub repo `Actions` tab and click `Enable Actions` if prompted.
3. Trigger CI at least once:
   - create a branch from `main`
   - make a tiny change
   - open PR to `main`
4. Wait for CI job `build-and-test` to complete.
5. Configure branch protection:
   - GitHub -> `Settings` -> `Branches` -> `Add rule`
   - branch name pattern: `main`
   - enable `Require a pull request before merging`
   - enable `Require status checks to pass before merging`
   - select check `CI / build-and-test`
   - optionally enable `Restrict who can push to matching branches`
6. Configure deployment approval:
   - GitHub -> `Settings` -> `Environments` -> `production`
   - add required reviewers for manual approval before deploy job

### Required Repository Secrets (Actions)

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`
