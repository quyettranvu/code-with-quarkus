### EVENT BUS WITH KAFKA

# Core concepts
Point-to-point communication, publish/subscribe, acknowledgments, failure handling and backpressure

 => Kafka:can handle a massive number of messages and makes ordering an essential characteristic.
 => AMQP does have a lot more flexibility than Kafka in the way it can be configured and customized. It also has higher elasticity in point-to-point scenarios, as the limit is not constrained by the number of partitions, which can make many subscriber consuming the same messages in a partition in a topic.

# Deployment on Kubernetes cluster:
- kafka-cluster.yaml: file deployment
- topics: processed.yaml, ticks.yaml

# Execution Commands:
```shell
minikube start --memory=4096
```

Create namespace:
```shell
kubectl create ns strimzi
kubectl create ns kafka
```

Install strimzi:
```shell
helm repo add strimzi https://strimzi.io/charts
helm install strimzi strimzi/strimzi-kafka-operator -n strimzi \
--set watchNamespaces={kafka} --wait --timeout 300s
```

Check strimzi operator status:
```shell
kubectl get pods -n strimzi
```

Create a Kafka cluster and topics:
```shell
kubectl apply -f deploy/kafka/kafka-cluster.yaml -n kafka

kubectl get pods -n kafka

kubectl apply -f deploy/kafka/ticks.yaml
kubectl apply -f deploy/kafka/processed.yaml
```

Increase the number of application instances to enhance the concurrency:
```shell
kubectl scale deployment/processor -n event-bus --replicas=3
```

### Reactive application health check with no errors
```
{
    "status": "UP",
        "checks": [
        {
            "name": "SmallRye Reactive Messaging - liveness check",
            "status": "UP",
            "data": {
                "ticks": "[OK]",
                "processed": "[OK]"
            }
        },
        {
            "name": "SmallRye Reactive Messaging - readiness check",
            "status": "UP",
            "data": {
                "ticks": "[OK]",
                "processed": "[OK]"
            }
        }
    ]
}
```

Check status of pods: 
```shell
kubectl get pods
```

### Reactive application health check with errors
```
{
    "status": "DOWN",
    "checks": [
        {
            "name": "SmallRye Reactive Messaging - liveness check",
            "status": "DOWN",
            "data": {
                "ticks": "[KO] - Random failure to process a record.",
                "processed": "[KO] - Multiple exceptions caught:
                [Exception 0] java.util.concurrent.CompletionException:
                java.lang.Throwable: Random failure to process a record.
                [Exception 1] io.smallrye.reactive.messaging.ProcessingException:
                SRMSG00103: Exception thrown when calling the method
                org.acme.Processor#process"
            }
        },
        {
            "name": "SmallRye Reactive Messaging - readiness check",
            "status": "UP",
            "data": {
                "ticks": "[OK] - no subscription yet,
                so no connection to the Kafka broker yet"
                "processed": "[OK]"
            }
        }
    ]
}
```

Path: /q/health/live, /q/health/ready, /q/health

### Metrics with messaging

Monitoring terms: SLA, SLO, SLI
Focus: Queue length, processing time, messages processed in a time window, ack-to-nack ratio.

```shell
minikube service --url observability-processor
```
Path: /q/metrics

### Tracing

- Span: A single operation within a trace (defined next). Many spans can be created within a single service, depending on the level of detail you want to collect.

- Trace: A collection of operations, or spans, representing a single request processed by an application and its components

*** Commands:
```shell
kubectl create ns jaeger
kubectl apply -f deploy/jaeger/jaeger-simplest.yaml -n jaeger

minikube service --url jaeger-ui -n jaeger
minikube service --url observability-viewer,
```

- Nếu dữ liệu trace đã bị xóa do hết thời gian lưu trữ, Jaeger không thể “ghép lại” một trace đầy đủ từ quá khứ. Nó chỉ hiển thị được những span hiện còn trong hệ thống, dẫn đến trace hiển thị không đầy đủ.
=> tăng thời gian retention, long-term storage bên thứ 3 (data lake (S3, GCS, HDFS, …) hoặc logging/metrics system (Prometheus, Grafana Loki)), export thủ công (file excel, CSV, JSON) hoặc dashboard.

Suggest: cấu hình Jaeger + OpenTelemetry Collector để lưu vào S3 hoặc Elasticsearch.



