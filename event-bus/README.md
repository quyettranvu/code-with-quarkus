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

