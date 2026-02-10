variables:
  RUN_AIKIDO_SCAN: "true"
  AIKIDO_LOCAL_SCANNER_TOKEN: $AIKIDO_LOCAL_SCANNER_TOKEN
  TRIVY_DB_REPOSITORY: "ghcr.io/aquasecurity/trivy-db"

default:
  timeout: 3h

before_script:
  - echo "🔍 Checking access to ghcr.io"
  - curl -v https://ghcr.io 2>&1 | grep "HTTP/2 200" || (echo "❌ ghcr.io unreachable" && exit 1)

include:
  - project: 'CST_Aible_Hub/converged-planning/cp-cicd-tools'
    ref: $AIKIDO_TEMPLATE_REF
    file: '/templates/aikido-scan.yml'

stages:
  - code-check
  - docker-image-build
  - monitor



before_script:
  - curl -v https://ghcr.io 2>&1 | grep "HTTP/2 200" || (echo "❌ ghcr.io unreachable" && exit 1)
