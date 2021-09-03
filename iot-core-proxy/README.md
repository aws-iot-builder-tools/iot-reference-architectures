This code is experimental and is currently for AWS solutions architect testing only.

To deploy:

```
cdk deploy
```

To destroy:

```
cdk destroy
```

To run the load test:

```
./gradlew integrationTest
```

To run the load test with custom parameters:

```
clientCount=50 messageCount=10 ./gradlew integrationTest
```

All load tests will create an HTML report in `build/reports/tests/integrationTest/index.html`.
