image: registry.medtronic.gitlab-dedicated.com/python:3.11-slim

stages:
  - parse
  - generate
  - propose

variables:
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink.yaml"

before_script:
  - apt-get update && apt-get install -y git curl
  - pip install -r requirements.txt

parse_job:
  stage: parse
  script:
    - echo "📥 Parsing ticket data..."
    - echo $MOCK_TICKET_DATA | python3 -c "import sys,json; data=json.load(sys.stdin); print(f'Ticket: {data.get(\"ticket_number\")}')"

generate_job:
  stage: generate
  script:
    - echo "📝 Generating Antrea ClusterNetworkPolicy..."
    - python3 scripts/generate_rule.py
    - echo "✅ Generated file:"
    - cat $OUTPUT_PATH$FILE_NAME
  artifacts:
    paths:
      - $OUTPUT_PATH

propose_job:
  stage: propose
  script:
    - git config user.name "Network Automation Bot"
    - git config user.email "automation-bot@medtronic.com"
    - |
      TICKET_ID=$(echo $MOCK_TICKET_DATA | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
      BRANCH_NAME="feature/egress-rule-$TICKET_ID"
    - git checkout -b $BRANCH_NAME
    - git add $OUTPUT_PATH
    - git commit -m "Auto-generate egress rule for $TICKET_ID"
    - git push -u origin $BRANCH_NAME
    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$CI_PROJECT_ID/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"main\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\",\"description\":\"Automated policy generation for $TICKET_ID\"}"
    - echo "✅ Merge Request created!"
  only:
    - main
