branch 'feature/egress-rule-INC-MECC-003' set up to track 'origin/feature/egress-rule-INC-MECC-003'.
Switched to a new branch 'feature/egress-rule-INC-MECC-003'
$ export POLICY_FULL_PATH="$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME"
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