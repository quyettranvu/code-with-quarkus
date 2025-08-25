### ORDER REACTIVE SYSTEM WITH QUARKUS

## Essential conversions:
1. Uni<List<String>> itemsAsList = multi.collect().asList();
2. Uni<Map<String, String>> itemsAsMap = multi.collect().asMap(item -> getKeyForItem(item));
3. Uni<Long> count = multi.collect().with(Collectors.counting());

## CDC with Debezium example

Start Zookeeper, Kafka, PostgreSQL, and Kafka Connect:

```shell
docker compose up
```

In another terminal, run the Quarkus application in _/chapter-9/debezium_:

```shell
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## Create Postgres connector

```shell
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @register.json
```

### Verify Postgres connector was created

```shell
curl -H "Accept:application/json" localhost:8083/connectors/
```

### Verify configuration of the Postgres connector

```shell
curl -X GET -H "Accept:application/json" localhost:8083/connectors/customer-connector
```

## Verify topics created for tables

```shell
docker exec -ti kafka bin/kafka-topics.sh --list --zookeeper zookeeper:2181
```

# Consume messages from the Customer table topic

```shell
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic quarkus-db-server.public.customer
```

Upon starting, four messages will be processed representing the four customers that were loaded
by the Quarkus application upon starting.
If you don't want to see those messages,
remove `--from-beginning` in the above command.

# Trigger customer updates

In another terminal, create or update customer data to see the messages appear.

## Update a customer name

```shell
curl -X PUT -H "Content-Type:application/json" http://localhost:8080/customer/2 -d '{"id" : 2, "name" : "Marsha Willis"}'
```

## Create a new customer

```shell
curl -X POST -H "Content-Type:application/json" http://localhost:8080/customer -d '{"name" : "Harry Houdini"}'
```

# Shut down

When finished, run the below to stop and remove the services:

```shell
docker compose stop
docker compose rm
```


### Message and Acknowledgement in Reactive Communication in System

# Supplier & Function
- Supplier: không có đầu vào, dùng khi muốn xác nhận đã xử lý xong, đạt được tính lazy (chỉ chạy khi get())
- Function: có đầu vào, theo dạng <Input type, output type> , dùng khi cần xử lý cả trường hợp thành công và xảy ra lỗi, cũng đạt được tính lazy (chỉ chạy khi được apply reason). 

# Fault Tolerance and Retry Processing
This can be done with onFailture.retry of Multiny API, but in source also demo the use of SmallRye Fault-Tolerance and @Retry annotation.

### Committing Strategies in Kafka:
Quy định cách xác nhận (acknowledge) đã xử lý xong một message:
• Throttled: Quarkus sẽ commit offset định kỳ sau một khoảng thời gian hoặc sau một số lượng bản ghi nhất định, thay vì commit ngay lập tức cho từng record -> giảm overhead khi phải commit liên tục.
• Ignore: Không commit offset, consumer sẽ không thông báo cho Kafka rằng nó đã xử lý xong record -> thích hợp khi muốn kiểm soát offset bên ngoài Kafka (như lưu offset vào DB riêng), hoặc khi cần xử lý lại message nhiều lần.
• Latest: Commit ngay lập tức offset mới nhất, bất kể đã xử lý xong hết message hay chưa -> Nếu ứng dụng chết sau khi commi nhưng trước khi xử lý xong, một số message sẽ bị mất, không được xử lý lại -> risk of data loss.