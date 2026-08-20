You can send this to the customer:

Hi team,

We reviewed the resource situation on the awby1-atlas-prd cluster and found that the current behavior is slightly different from a general CPU/Memory exhaustion condition.

The cluster still has available CPU and memory capacity across several worker nodes. However, the affected workload itops-nginx is explicitly configured through Helm to run only on the worker node:

awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm

That specific node currently has approximately 99% of its allocatable memory reserved through Kubernetes resource requests, which prevents itops-nginx and some Envoy DaemonSet pods from being scheduled there. The node itself is not reporting MemoryPressure, and its actual memory usage is significantly lower, so this appears to be a scheduling/resource reservation and workload placement issue rather than the entire cluster being out of CPU or memory.

We also verified that itops-nginx is the only regular workload currently using a hard hostname-based nodeSelector, and the Deployment is managed by Helm.

As a next step, we recommend reviewing the Helm configuration for itops-nginx to confirm whether it really needs to be pinned to this specific worker node. If that placement is not required, allowing Kubernetes to schedule the workload across the available worker nodes should resolve the immediate deployment issue.

After that change, we can reassess the remaining resource reservations and determine whether additional Atlas cluster capacity is actually required.

Could you please confirm whether the itops-nginx workload is intentionally required to run specifically on node 5pltm?
