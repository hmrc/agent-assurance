# agent-kyc

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-kyc/images/download.svg) ](https://bintray.com/hmrc/releases/agent-kyc/_latestVersion)

This micro service is responsible for agents Know Your Customer operations.

## Run the application locally

To run the application execute
```
sbt run
```

Running locally, the services will run on http://localhost:9564

#### Check for IRSAAgent enrolment for the current logged in user
```
GET   	/agent-kyc/irSaAgentEnrolment 
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | The current user is enrolled for IR-SA-AGENT |
| 403 | The current user is not enrolled for IR-SA-AGENT |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")


