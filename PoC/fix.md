generate_job:
  stage: generate
  script:
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$CI_PROJECT_DIR"

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"
    - rm -rf "$CI_PROJECT_DIR/target-repo"
    - git clone "$TARGET_REPO_URL" "$CI_PROJECT_DIR/target-repo"

    - cd "$CI_PROJECT_DIR/target-repo"
    - git fetch origin "$TARGET_BASE_BRANCH"
    - git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"

    - export POLICY_FULL_PATH="$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME"

    - echo "==== Existing file before parsing ===="
    - nl -ba "$POLICY_FULL_PATH" | sed -n '1,60p'

    - cd "$CI_PROJECT_DIR"
    - python3 scripts/parse_input.py
