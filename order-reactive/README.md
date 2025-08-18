### ORDER REACTIVE SYSTEM WITH QUARKUS

## Essential conversions:
1. Uni<List<String>> itemsAsList = multi.collect().asList();
2. Uni<Map<String, String>> itemsAsMap = multi.collect().asMap(item -> getKeyForItem(item));
3. Uni<Long> count = multi.collect().with(Collectors.counting());