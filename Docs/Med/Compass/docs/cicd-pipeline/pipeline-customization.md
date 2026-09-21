---
label: Pipeline Customization
icon: code
order: 15
---

# Pipeline Customization

Use these copy/paste examples in your application's `.gitlab-ci.yml` to customize job behavior without modifying shared CI templates.

## How Overrides Work

If a shared job is defined in an included template, define the same job name in your `.gitlab-ci.yml` and set your own `rules`, variables, or scripts.

```yaml
build-dev:
  extends: .image-build
  rules:
    - if: '$CI_COMMIT_BRANCH == "dev"'
      changes:
        - Dockerfile
        - src/**/*
    - when: never
```

---

## Java / Gradle

### Build only when Java runtime files change

```yaml
build-dev:
  extends: .image-build
  rules:
    - if: '$CI_COMMIT_BRANCH == "dev"'
      changes:
        - Dockerfile
        - src/main/**/*
        - src/test/**/*
        - build.gradle
        - settings.gradle
        - gradle.properties
        - gradle/**/*
    - when: never
```

### Deploy only when app or deployment config changed

```yaml
dev-deploy:
  extends: [.dev, .deploy-kustomize-base]
  rules:
    - if: '$CI_COMMIT_BRANCH == "dev"'
      changes:
        - k8s/**/*
        - containers.yml
        - charts/**/*
        - src/main/**/*
        - build.gradle
    - when: never
```

---

## Node.js

### Build only when Node app files change

```yaml
build-testing:
  extends: .image-build
  rules:
    - if: '$CI_COMMIT_BRANCH == "testing"'
      changes:
        - Dockerfile
        - package.json
        - package-lock.json
        - yarn.lock
        - pnpm-lock.yaml
        - src/**/*
        - public/**/*
    - when: never
```

### Add a custom lint job

```yaml
lint:frontend:
  stage: lint
  image: node:20-alpine
  script:
    - npm ci
    - npm run lint
  rules:
    - if: '$CI_PIPELINE_SOURCE == "push"'
      changes:
        - src/**/*
        - package.json
        - package-lock.json
```

---

## Python

### Build only when Python app files change

```yaml
build-dev:
  extends: .image-build
  rules:
    - if: '$CI_COMMIT_BRANCH == "dev"'
      changes:
        - Dockerfile
        - requirements.txt
        - requirements-dev.txt
        - pyproject.toml
        - poetry.lock
        - src/**/*
        - app/**/*
    - when: never
```

### Add a custom test job

```yaml
test:pytest:
  stage: test
  image: python:3.12-alpine
  script:
    - pip install -r requirements.txt
    - pip install pytest
    - pytest -q
  rules:
    - if: '$CI_PIPELINE_SOURCE == "push"'
      changes:
        - src/**/*
        - app/**/*
        - tests/**/*
        - requirements.txt
```

---

## Reusable Rule Anchors (Optional)

Use YAML anchors to avoid repeating the same file sets across multiple jobs.

```yaml
.runtime_changes: &runtime_changes
  - Dockerfile
  - src/**/*
  - k8s/**/*
  - containers.yml

build-dev:
  extends: .image-build
  rules:
    - if: '$CI_COMMIT_BRANCH == "dev"'
      changes: *runtime_changes
    - when: never

dev-deploy:
  extends: [.dev, .deploy-kustomize-base]
  rules:
    - if: '$CI_COMMIT_BRANCH == "dev"'
      changes: *runtime_changes
    - when: never
```

---

## Validation Checklist

1. Run CI lint in **Build > Pipeline editor**.
2. Use **Simulate pipeline** for `dev`, `testing`, `staging`, and `main`.
3. Verify intended jobs are created/skipped for sample file changes.
4. Confirm release jobs still run on `staging` and `main` when expected.
