propose_job:
  stage: propose
  needs:
    - job: generate_job
      artifacts: true
  script:
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$CI_PROJECT_DIR"

    - |
      TICKET_ID=$(echo "$MOCK_TICKET_DATA" | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
      BRANCH_NAME="feature/egress-rule-$TICKET_ID"
      TARGET_PROJECT_PATH_ENC="io-shared-services%2Fatlas%2Fkubernetes-management%2Fabrc1l-atlas-dev"

    - echo "TICKET_ID=$TICKET_ID"
    - echo "BRANCH_NAME=$BRANCH_NAME"
    - echo "TARGET_BASE_BRANCH=$TARGET_BASE_BRANCH"

    - test -f "$CI_PROJECT_DIR/generated_policy.yaml" || { echo "Artifact missing"; find "$CI_PROJECT_DIR" -maxdepth 3 -type f | sort; exit 1; }

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"

    - rm -rf /tmp/target
    - mkdir -p /tmp/target
    - cd /tmp/target
    - git clone "$TARGET_REPO_URL" repo
    - cd repo

    - git fetch origin "$TARGET_BASE_BRANCH"
    - git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"
    - git switch -c "$BRANCH_NAME"

    - mkdir -p "$OUTPUT_PATH"
    - cp -v "$CI_PROJECT_DIR/generated_policy.yaml" "$OUTPUT_PATH$FILE_NAME"

    - git status
    - git add "$OUTPUT_PATH$FILE_NAME"
    - git commit -m "Auto-update egress rule for $TICKET_ID"
    - git push -u origin "$BRANCH_NAME"

    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GIT_WRITE_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$TARGET_PROJECT_PATH_ENC/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"$TARGET_BASE_BRANCH\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\",\"description\":\"Automated policy update for $TICKET_ID\"}"
