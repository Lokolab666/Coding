$K -n "$NS" get pods

NAME                                        READY   STATUS    RESTARTS   AGE
oracleclinicalrdcsso-dev-57f47458d9-cqlh9   1/1     Running   0          7d2h
oracleclinicalrdcsso-dev-57f47458d9-kz5nz   1/1     Running   0          2d
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get events \
>   --sort-by=.metadata.creationTimestamp |
> grep -Ei 'secret|provider|csi|mount|accessdenied|failed'
No resources found in argo-oracleclinicalrdcsso-dev namespace.
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get pods

NAME                                        READY   STATUS    RESTARTS   AGE
oracleclinicalrdcsso-dev-57f47458d9-cqlh9   1/1     Running   0          7d2h
oracleclinicalrdcsso-dev-57f47458d9-kz5nz   1/1     Running   0          2d
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get events \
>   --sort-by=.metadata.creationTimestamp |
> grep -Ei 'secret|provider|csi|mount|accessdenied|failed'
No resources found in argo-oracleclinicalrdcsso-dev namespace.
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get pods -o yaml |
> grep -nEi 'secrets-store|secretProviderClass|csi:|secretName'
219:        secretName: secrets-files
309:        secretName: secrets-providers
661:        secretName: secrets-files
751:        secretName: secrets-providers
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get secretproviderclasses.secrets-store.csi.x-k8s.io 2>/dev/null
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get externalsecrets.external-secrets.io 2>/dev/null
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" get secrets \
>   -o custom-columns='NAME:.metadata.name,TYPE:.type,CREATED:.metadata.creationTimestamp'
NAME                                                      TYPE                 CREATED
dynatrace-bootstrapper-certs                              Opaque               2026-06-11T08:58:24Z
dynatrace-bootstrapper-config                             Opaque               2026-06-11T08:58:23Z
secrets                                                   opaque               2024-06-25T19:29:04Z
secrets-files                                             opaque               2024-06-25T19:29:04Z
secrets-providers                                         opaque               2024-06-25T19:29:04Z
sh.helm.release.v1.oracleclinicalrdcsso-dev-ingress.v10   helm.sh/release.v1   2025-10-22T11:49:28Z
sh.helm.release.v1.oracleclinicalrdcsso-dev-ingress.v11   helm.sh/release.v1   2026-02-01T05:09:57Z
sh.helm.release.v1.oracleclinicalrdcsso-dev-ingress.v12   helm.sh/release.v1   2026-02-12T11:42:37Z
sh.helm.release.v1.oracleclinicalrdcsso-dev-ingress.v13   helm.sh/release.v1   2026-03-17T18:42:06Z
sh.helm.release.v1.oracleclinicalrdcsso-dev-ingress.v14   helm.sh/release.v1   2026-03-31T12:46:22Z
sh.helm.release.v1.oracleclinicalrdcsso-dev.v4            helm.sh/release.v1   2026-03-31T12:59:18Z
sh.helm.release.v1.oracleclinicalrdcsso-dev.v5            helm.sh/release.v1   2026-03-31T13:19:30Z
sh.helm.release.v1.oracleclinicalrdcsso-dev.v6            helm.sh/release.v1   2026-04-01T10:00:43Z
sh.helm.release.v1.oracleclinicalrdcsso-dev.v7            helm.sh/release.v1   2026-05-07T08:10:53Z
sh.helm.release.v1.oracleclinicalrdcsso-dev.v8            helm.sh/release.v1   2026-05-07T14:06:32Z
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ for pod in $($K -n "$NS" get pods -o name); do
>   echo "===== $pod ====="
>   $K -n "$NS" describe "$pod" | sed -n '/Events:/,$p'
> done

===== pod/oracleclinicalrdcsso-dev-57f47458d9-cqlh9 =====
Events:          <none>
===== pod/oracleclinicalrdcsso-dev-57f47458d9-kz5nz =====
Events:          <none>

$K -n kube-system get daemonsets,pods |
> grep -Ei 'secret|csi|provider'
daemonset.apps/ebs-csi-node           5         5         5       5            5           kubernetes.io/os=linux     4y122d
daemonset.apps/ebs-csi-node-windows   0         0         0       0            0           kubernetes.io/os=windows   4y122d
daemonset.apps/efs-csi-node           5         5         5       5            5           kubernetes.io/os=linux     3y179d
pod/ebs-csi-controller-ccd7db55f-2lh7p            6/6     Running   2 (27d ago)      41d
pod/ebs-csi-controller-ccd7db55f-6m6ds            6/6     Running   0                79m
pod/ebs-csi-node-7qxkm                            3/3     Running   0                41d
pod/ebs-csi-node-8cj2f                            3/3     Running   0                41d
pod/ebs-csi-node-mmblv                            3/3     Running   0                3d2h
pod/ebs-csi-node-q7jts                            3/3     Running   0                90m
pod/ebs-csi-node-q9zq5                            3/3     Running   0                41d
pod/efs-csi-controller-7765c66789-qfnt8           3/3     Running   0                41d
pod/efs-csi-controller-7765c66789-wz8sq           3/3     Running   0                41d
pod/efs-csi-node-6bx7q                            3/3     Running   0                41d
pod/efs-csi-node-8f2dg                            3/3     Running   0                41d
pod/efs-csi-node-j56sf                            3/3     Running   0                90m
pod/efs-csi-node-mxdfm                            3/3     Running   0                41d
pod/efs-csi-node-tm596                            3/3     Running   0                3d2h

for pod in $($K -n "$NS" get pods -o name); do
  echo "===== $pod ====="

  $K -n "$NS" logs "$pod" \
    --all-containers \
    --since=4h \
    --tail=500 2>&1 |
  grep -Ei 'secret|credential|password|accessdenied|ORA-|JDBC|authentication|failed'
done
1:15:33:725 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:15:53:733 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:16:28:738 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:16:43:743 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:16:58:746 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:17:13:755 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:17:28:759 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:17:53:767 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:18:08:772 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:18:28:781 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:18:48:789 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:19:08:793 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:19:23:798 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:19:38:804 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:19:53:808 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:20:08:814 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:20:23:819 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:20:53:826 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:21:13:831 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:21:33:836 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:21:58:844 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:22:13:848 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:22:38:854 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:22:53:859 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:23:33:865 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:23:53:873 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:24:13:877 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:24:33:884 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:24:58:893 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:25:13:897 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:25:33:904 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:25:48:908 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:26:28:917 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:26:43:920 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:26:58:924 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local
[otel.javaagent 2026-09-28 11:27:23:927 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
[otel.javaagent 2026-09-28 11:27:38:932 -0500] [OkHttp http://obs-otel-agent-collector.observability.svc.cluster.local:4317/...] ERROR io.opentelemetry.exporter.internal.grpc.OkHttpGrpcExporter - Failed to export spans. The request could not be executed. Full error message: obs-otel-agent-collector.observability.svc.cluster.local: Name does not resolve
