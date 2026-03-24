chmod +x /usr/local/bin/sops
$ python3 -m venv venv
$ source venv/bin/activate
$ pip install --no-cache-dir --extra-index-url $PIP_INDEX_URL --trusted-host $PIP_TRUSTED_HOST -r requirements.txt
Looking in indexes: https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple, https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple
Collecting jinja2==3.1.2 (from -r requirements.txt (line 1))
  Downloading https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/packages/packages/bc/c3/f068337a370801f372f2f8f6bad74a5c140f6fda3d9de154052708dd3c65/Jinja2-3.1.2-py3-none-any.whl (133 kB)
Collecting ruamel.yaml>=0.18 (from -r requirements.txt (line 2))
  Downloading https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/packages/packages/b8/0c/51f6841f1d84f404f92463fc2b1ba0da357ca1e3db6b7fbda26956c3b82a/ruamel_yaml-0.19.1-py3-none-any.whl (118 kB)
Collecting MarkupSafe>=2.0 (from jinja2==3.1.2->-r requirements.txt (line 1))
  Downloading https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/packages/packages/89/e0/4486f11e51bbba8b0c041098859e869e304d1c261e59244baa3d295d47b7/markupsafe-3.0.3-cp312-cp312-musllinux_1_2_x86_64.whl (23 kB)
Installing collected packages: ruamel.yaml, MarkupSafe, jinja2
Successfully installed MarkupSafe-3.0.3 jinja2-3.1.2 ruamel.yaml-0.19.1
[notice] A new release of pip is available: 25.0.1 -> 26.0.1
[notice] To update, run: pip install --upgrade pip
$ git --version
git version 2.47.3
$ git config --global user.name "Cristian Fandino"
$ git config --global user.email "cristian.a.fandinomesa@medtronic.com"
$ git config --global --add safe.directory "$CI_PROJECT_DIR"
$ TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"
$ rm -rf "$CI_PROJECT_DIR/target-repo"
$ git clone "$TARGET_REPO_URL" "$CI_PROJECT_DIR/target-repo"
Cloning into '/builds/bcp_web/automation/kubernetes/network-policy-automation/target-repo'...
$ cd "$CI_PROJECT_DIR/target-repo"
$ git fetch origin "$TARGET_BASE_BRANCH"
From https://code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev
 * branch            feature/egress-rule-INC-MECC-003 -> FETCH_HEAD
$ git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"
branch 'feature/egress-rule-INC-MECC-003' set up to track 'origin/feature/egress-rule-INC-MECC-003'.
Switched to a new branch 'feature/egress-rule-INC-MECC-003'
$ export POLICY_FULL_PATH="$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME"
$ echo "==== Existing file before parsing ===="
==== Existing file before parsing ====
$ nl -ba "$POLICY_FULL_PATH" | sed -n '1,60p'
     1	---
     2	apiVersion: crd.antrea.io/v1beta1
     3	kind: ClusterNetworkPolicy
     4	metadata:
     5	  name: egress-livelink-mecc
     6	  labels:
     7	    ticket: "INC-MECC-003"
     8	    cluster: "abrc1l-atlas-stg"
     9	spec:
    10	  egress:
    11	    - action: Allow
    12	    appliedTo:
    13	      - namespaceSelector:
    14	        matchExpressions:
    15	          - key: "kubernetes.io/metadata.name"
    16	          operator: "In"
    17	          values: ["livelink-ignition"]
    18	    enableLogging: true
    19	    name: destination_fqdn
    20	    to:
    21	      - fqdn: abrc1-avis3gw-test.corp.medtronic.com
    22	    ports:
    23	      - protocol: TCP
    24	      port: 2049
    25	      - protocol: TCP
    26	      port: 20048
    27	      - protocol: TCP
    28	      port: 32803
    29	  priority: 80
    30	  tier: atlas-tenants
$ cd "$CI_PROJECT_DIR"
$ python3 scripts/parse_input.py
Traceback (most recent call last):
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/scripts/parse_input.py", line 230, in <module>
    main()
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/scripts/parse_input.py", line 217, in main
    doc = load_existing_yaml(full_path)
          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/scripts/parse_input.py", line 177, in load_existing_yaml
    return yaml.load(f)
           ^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/main.py", line 454, in load
    return constructor.get_single_data()
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/constructor.py", line 117, in get_single_data
    node = self.composer.get_single_node()
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 77, in get_single_node
    document = self.compose_document()
               ^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 100, in compose_document
    node = self.compose_node(None, None)
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 144, in compose_node
    node = self.compose_mapping_node(anchor)
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 227, in compose_mapping_node
    item_value = self.compose_node(node, item_key)
                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 144, in compose_node
    node = self.compose_mapping_node(anchor)
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 227, in compose_mapping_node
    item_value = self.compose_node(node, item_key)
                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 142, in compose_node
    node = self.compose_sequence_node(anchor)
           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/composer.py", line 187, in compose_sequence_node
    while not self.parser.check_event(SequenceEndEvent):
              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/parser.py", line 141, in check_event
    self.current_event = self.state()
                         ^^^^^^^^^^^^
  File "/builds/bcp_web/automation/kubernetes/network-policy-automation/venv/lib/python3.12/site-packages/ruamel/yaml/parser.py", line 546, in parse_block_sequence_entry
    raise ParserError(
ruamel.yaml.parser.ParserError: while parsing a block collection
  in "/builds/bcp_web/automation/kubernetes/network-policy-automation/target-repo/policies/cluster/customer-components/cluster-scoped/antrea_clusternetworkpolicy_egress-livelink.yaml", line 11, column 5
expected <block end>, but found '?'
  in "/builds/bcp_web/automation/kubernetes/network-policy-automation/target-repo/policies/cluster/customer-components/cluster-scoped/antrea_clusternetworkpolicy_egress-livelink.yaml", line 12, column 5
Cleaning up project directory and file based variables
00:01
ERROR: Job failed: command terminated with exit code 1
