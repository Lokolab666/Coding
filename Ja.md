This is the ouput: $ mkdir -p $OUTPUT_PATH $ python3 -c " # collapsed multi-line command Generated: policies/cluster/customer-components/cluster-scoped/antrea_clusternetworkpolicy_egress-livelink.yaml $ cat $OUTPUT_PATH$FILE_NAME apiVersion: crd.antrea.io/v1beta1 kind: ClusterNetworkPolicy metadata: name: egress-livelink-mecc labels: ticket: INC-MECC-001 cluster: abrc1-atlas-preview-prd spec: egress:

action: Allow appliedTo:
namespaceSelector: matchExpressions: - key: "kubernetes.io/metadata.name" operator: "In" values: ["livelink-ignition"] enableLogging: true name: ENABLE NFS WRITE ACCESS to:
fqdn: meccstoragegw-prod.medtronic.com
fqdn: abrc1-avis3gw-test.corp.medtronic.com ports:
protocol: TCP port: 2049
protocol: TCP port: 2049
protocol: TCP port: 20048
protocol: TCP port: 20048
protocol: TCP port: 32803
protocol: TCP port: 32803 priority: 80 tier: atlas-tenants
And this is the JSON { "ticket_number": "INC-MECC-001", "requester": "network-team@medtronic.com", "cluster": "abrc1-atlas-preview-prd", "rules": [ { "source_namespace": "livelink-ignition", "destination_ip": "10.76.217.22", "destination_fqdn": "abrc1-avis3gw-test.corp.medtronic.com", "ports": "2049,20048,32803", "purpose": "enable NFS write access" }, { "source_namespace": "livelink-ignition", "destination_ip": "10.76.217.9", "destination_fqdn": "meccstoragegw-prod.medtronic.com", "ports": "2049,20048,32803", "purpose": "enable NFS write access" } ] }

And I need some like the next: apiVersion: crd.antrea.io/v1beta1 kind: ClusterNetworkPolicy metadata: name: egress-livelink-mecc spec: egress:

action: Allow appliedTo:
namespaceSelector: matchExpressions: - key: "kubernetes.io/metadata.name" operator: "In" values: ["livelink-ignition"] enableLogging: true name: STG MECC V2 NFS write access to:
fqdn: meccstoragegw-prod.medtronic.com
fqdn: abrc1-avis3gw-test.corp.medtronic.com ports:
protocol: TCP port: 2049
protocol: TCP port: 20048
protocol: TCP port: 32803 priority: 80 tier: atlas-tenants