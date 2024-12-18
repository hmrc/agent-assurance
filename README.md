# agent-assurance

[![Build Status](https://travis-ci.org/hmrc/agent-assurance.svg)](https://travis-ci.org/hmrc/agent-assurance)

This microservice is responsible for agents Know Your Customer operations.

It has checks for:
- anti-money laundering supervision (AMLS)
- refusal to deal with list
- manually assured list
- legacy enrolment check (IR-SA-AGENT)
- active 'legacy' client/agent relationship in CESA (IR-PAYE, IR-SA, HMCE-VATDEC-ORG, IR-CT)

These endpoints are called during the agent subscription journey. 

The "refusal to deal with" and "manually assured" lists are maintained by HMRC helpdesk staff (stride users) in agent-helpdesk-frontend.

## Running the tests

    sbt test it/test

## Running the tests with coverage

    sbt clean coverageOn test it/test coverageReport

## Run the application locally

    sm2 -start AGENT_AUTHORISATION
    sm2 -stop AGENT_ASSURANCE

To run the application execute
```
./run.sh
```

Running locally, the services will run on http://localhost:9565

#### Check for IRSAAgent enrolment for the current logged in user
```
GET   	/agent-assurance/irSaAgentEnrolment 
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | The current user is enrolled for IR-SA-AGENT |
| 403 | The current user is not enrolled for IR-SA-AGENT |

#### Check if there is an active client/agent relationship in CESA with NINO
```
GET   	/activeCesaRelationship/nino/:nino/saAgentReference/:saAgentReference
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | The user provides a valid NINO and an IRAgentReference identifier that matches an active agent/client relationship in CESA |
| 403 | There is no active agent/client relationship in CESA between the NINO and an IRAgentReference|
| 401 | The user is not authenticated|

##### Example

Assuming there is an active CESA agent relationship with IRAgentReference of SA6012.
```
curl -v -X GET   http://localhost:9565/agent-assurance/activeCesaRelationship/nino/AA123456A/saAgentReference/SA1062   -H 'authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82'   -H 'cache-control: no-cache'   -H 'postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008'
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 9565 (#0)
> GET /agent-assurance/activeCesaRelationship/nino/AA123456A/saAgentReference/SA1062HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.47.0
> Accept: */*
> authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82
> cache-control: no-cache
> postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008
>
< HTTP/1.1 204 No Content
< Date: Fri, 13 Oct 2017 13:55:13 GMT
<
* Connection #0 to host localhost left intact
```

Assuming the user provides IRAgentReference of SA6012 but there is no active agent/client relationship in CESA:
```
curl -v -X GET   http://localhost:9565/agent-assurance/activeCesaRelationship/nino/AB002913A/saAgentReference/SA1062   -H 'authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82'   -H 'cache-control: no-cache'   -H 'postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008'
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 9565 (#0)
> GET /agent-assurance/activeCesaRelationship/nino/AB002913A/saAgentReference/SA1062 HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.47.0
> Accept: */*
> authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82
> cache-control: no-cache
> postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008
>
< HTTP/1.1 403 Forbidden
< Content-Length: 0
< Date: Fri, 13 Oct 2017 14:01:09 GMT
<
* Connection #0 to host localhost left intact
```

#### Check if there is an active client/agent relationship in CESA with UTR
```
GET   	/activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | The user provides a valid UTR and an IRAgentReference identifier that matches an active agent/client relationship in CESA |
| 403 | There is no active agent/client relationship in CESA between the UTR and IRAgentReference|
| 401 | The user is not authenticated|

##### Example

Assuming user has an active CESA agent relationship with IRAgentReference of SA6012.
```
curl -v -X GET   http://localhost:9565/agent-assurance/activeCesaRelationship/utr/7000000002/saAgentReference/SA1062   -H 'authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82'   -H 'cache-control: no-cache'   -H 'postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008'
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 9565 (#0)
> GET /agent-assurance/activeCesaRelationship/utr/7000000002/saAgentReference/SA1062 HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.47.0
> Accept: */*
> authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82
> cache-control: no-cache
> postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008
>
< HTTP/1.1 204 No Content
< Date: Fri, 13 Oct 2017 13:55:13 GMT
<
* Connection #0 to host localhost left intact
```

Assuming user provides IRAgentReference of SA6012 but there is no active agent/client relationship in CESA:
```
curl -v -X GET   http://localhost:9565/agent-assurance/activeCesaRelationship/utr/7000000002/saAgentReference/SA1062   -H 'authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82'   -H 'cache-control: no-cache'   -H 'postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008'
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 9565 (#0)
> GET /agent-assurance/activeCesaRelationship/utr/7000000002/saAgentReference/SA1062 HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.47.0
> Accept: */*
> authorization: Bearer XlLM91CY3hEHqHlrKX9N0QiipuC7OgyTZ/X4lZsBP7LjP1u/FTyg3BP6cBwPlI2mRKgg5SSYGc5YQV4ey85p4+kGWh90x366Iwc5dACuF/ME56mEsOk9zoM3xmXgD34UDcJvR2BEcijUSTzqB3fFrL8GXJCatRwgUb/Zd4VGpwo1TnU/CoN5cH3wc88qbn82
> cache-control: no-cache
> postman-token: 97bcf028-5bc5-b080-ac5f-0f7e7a383008
>
< HTTP/1.1 403 Forbidden
< Content-Length: 0
< Date: Fri, 13 Oct 2017 14:01:09 GMT
<
* Connection #0 to host localhost left intact
```

#### Check if the agent has enough clients to pass specified in config requirement
```
GET   	/agent-assurance/acceptableNumberOfClients/service/IR-PAYE
GET   	/agent-assurance/acceptableNumberOfClients/service/IR-SA
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | The logged in user has more or enough clients for service|
| 403 | The amount of clients did not pass the requirement for service|

#### Get value containing all identifiers in collection
```
GET   	/agent-assurance/<collectionName>
e.g. GET   	/agent-assurance/(refusal-to-deal-with/manually-assured)
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | body contains comma separated list|
| 204 | Property(collection) has not been found/initialised|

##### Example
```
curl -v -X GET http://localhost:9565/agent-assurance/refusal-to-deal-with
**
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 9565 (#0)
> GET /agent-assurance/refusal-to-deal-with HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 204 No Content
< Cache-Control: no-cache,no-store,max-age=0
< Date: Mon, 19 Mar 2018 15:23:54 GMT
< 
* Connection #0 to host localhost left intact
```

#### Check if the UTR exists in collection specified in URL
```
GET   	/agent-assurance/<collectionName>/utr:identifier
e.g. GET   	/agent-assurance/(refusal-to-deal-with/manually-assured)/utr:identifier
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | if collection is manually-assured UTR exists, if refusal-to-deal-with UTR does not exist in collection|
| 403 | if collection is refusal-to-deal-with UTR exists, if manually-assured UTR does not exist in collection|

##### Example
```
curl -v -X GET http://localhost:9565/agent-assurance/refusal-to-deal-with/utr/7110118304
**
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 9565 (#0)
> GET /agent-assurance/refusal-to-deal-with/utr/7110118304 HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 403 Forbidden
< Cache-Control: no-cache,no-store,max-age=0
< Content-Length: 0
< Date: Mon, 19 Mar 2018 15:26:44 GMT
< 
* Connection #0 to host localhost left intact
```

#### Create/Initialise property/collection which will hold identifiers in its value
```
POST   	/agent-assurance/<collectionName>
e.g. POST   	/agent-assurance/(refusal-to-deal-with/manually-assured)
```
Response Code(s)

| Status Code | Description |
|---|---|
| 201 | Property created storing the value specified in body|
| 403 | Property already exists|

##### Example
```
curl -v -X POST -H 'Content-Type: application/json' --data '{"key":"refusal-to-deal-with","value": "2110118265, 6110118246, 4110118247, 2110118248"}' http://localhost:9565/agent-assurance/refusal-to-deal-with
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 9565 (#0)
> POST /agent-assurance/refusal-to-deal-with HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 88
> 
* upload completely sent off: 88 out of 88 bytes
< HTTP/1.1 201 Created
< Cache-Control: no-cache,no-store,max-age=0
< Content-Length: 0
< Date: Mon, 19 Mar 2018 15:24:43 GMT
< 
* Connection #0 to host localhost left intact
```

#### Add identifier/Update existing property's value/collection
```
PUT   	/agent-assurance/<collectionName>
e.g. PUT   	/agent-assurance/(refusal-to-deal-with/manually-assured)
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | identifier added|
| 404 | Property not found|
| 409 | identifier already exists in collection|
| 500 | Failed to update property with specified value|

##### Example
```
curl -v -X PUT -H 'Content-Type: application/json' --data '{"key":"refusal-to-deal-with","value": "2110118249, 7110118304"}' http://localhost:9565/agent-assurance/refusal-to-deal-with
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 9565 (#0)
> PUT /agent-assurance/refusal-to-deal-with HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 64
> 
* upload completely sent off: 64 out of 64 bytes
< HTTP/1.1 204 No Content
< Cache-Control: no-cache,no-store,max-age=0
< Date: Mon, 19 Mar 2018 15:25:22 GMT
< 
* Connection #0 to host localhost left intact
```

#### Remove identifier from property/collection
```
DELETE   	/agent-assurance/<collectionName>/utr/:identifier
e.g. DELETE   	/agent-assurance/(refusal-to-deal-with/manually-assured)/utr/:7110118304
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | identifier removed|
| 404 | Property/collection not found|
| 500 | Failed to update property, identifier not removed|

##### Example
```
curl -v -X DELETE http://localhost:9565/agent-assurance/refusal-to-deal-with/utr/7110118304
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 9565 (#0)
> DELETE /agent-assurance/refusal-to-deal-with/utr/7110118304 HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 204 No Content
< Cache-Control: no-cache,no-store,max-age=0
< Date: Mon, 19 Mar 2018 15:27:14 GMT
< 
* Connection #0 to host localhost left intact
```

#### Deletes entire property/collection, all of identifiers are lost
```
DELETE   	/agent-assurance/<collectionName>
e.g. DELETE   	/agent-assurance/(refusal-to-deal-with
```
Response Code(s)

| Status Code | Description |
|---|---|
| 204 | property deleted with all identifiers|
| 404 | Property/collection not found|

##### Example
```
curl -v -X DELETE http://localhost:9565/agent-assurance/refusal-to-deal-with
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 9565 (#0)
> DELETE /agent-assurance/refusal-to-deal-with HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 404 Not Found
< Cache-Control: no-cache,no-store,max-age=0
< Content-Length: 0
< Date: Fri, 04 May 2018 15:28:09 GMT
< 
* Connection #0 to host localhost left intact
```

#### Stores AMLS Details of an Agent
```
POST   	/agent-assurance/amls
```
Response Code(s)

| Status Code | Description |
|---|---|
| 201 | AMLS Details has been stored for the Agent|
| 400 | Can't accept ARN during creating AMLS Details for the first time |
| 403 | AMLS Details already exist with an ARN |
| 500 | Unexpected server error during AMLS record creation |

##### Example
```
curl -v -X POST http://localhost:9565/agent-assurance/amls -H 'Content-Type: application/json' --data '{"utr":"12345678","amlsDetails":{"supervisoryBody":"supervisory","membershipNumber":"123456","membershipExpiresOn":"2018-11-30"}}'
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 9565 (#0)
> POST /agent-assurance/amls HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 112
> 
* upload completely sent off: 112 out of 112 bytes
< HTTP/1.1 201 Created
< Cache-Control: no-cache,no-store,max-age=0
< Content-Length: 0
< Date: Mon, 26 Nov 2018 16:06:19 GMT
< 
* Connection #0 to host localhost left intact
```

#### Stores AMLS Details of an Overseas Agent
```
POST   	/agent-assurance/overseas-agents/amls
```
Response Code(s)

| Status Code | Description |
|---|---|
| 201 | AMLS Details has been stored for the Agent|
| 400 | Invalid arn in the request body |
| 409 | AMLS Details already exists for the ARN|
| 500 | Unexpected server error during AMLS record creation |

##### Example
```
curl -v -X POST http://localhost:9565/agent-assurance/overseas-agents/amls -H 'Content-Type: application/json' --data '{"arn":"AARN0000002",
"amlsDetails":{"supervisoryBody":"supervisory",
               "membershipNumber":"123456" //optional }}'
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 9565 (#0)
> POST /agent-assurance/overseas-agents/amls HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 112
>
* upload completely sent off: 112 out of 112 bytes
< HTTP/1.1 201 Created
< Cache-Control: no-cache,no-store,max-age=0
< Content-Length: 0
< Date: Mon, 26 Nov 2018 16:06:19 GMT
<
* Connection #0 to host localhost left intact
```

#### Get AMLS Details
```
GET     /agent-assurance/amls/utr/:identifier
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | AMLS Record is found and returned |
| 404 | Couldn't find existing AMLS record in the db for the given UTR |

#### Update AMLS Details of an ARN
```
PUT   	/agent-assurance/amls/utr/:identifier
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | Existing AMLS record has been updated with given ARN|
| 400 | ARN should be unique for each UTR |
| 409 | Update AMLS request for the same ARN |
| 403 | Can't update existing AMLS record (with a ARN) with a new ARN |
| 404 | Couldn't find existing AMLS record in the db for the given UTR |
| 500 | Unexpected server error during updating AMLS record |

##### Example
```
curl -v -X PUT http://localhost:9565/agent-assurance/amls/utr/7000000002 -H 'Content-Type: application/json' --data '{"value":"AARN0000002"}'
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 9565 (#0)
> PUT /agent-assurance/amls/utr/7000000002 HTTP/1.1
> Host: localhost:9565
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 23
> 
* upload completely sent off: 23 out of 23 bytes
< HTTP/1.1 200 OK
< Cache-Control: no-cache,no-store,max-age=0
< Content-Length: 139
< Content-Type: application/json
< Date: Mon, 26 Nov 2018 16:24:00 GMT
< 
* Connection #0 to host localhost left intact
{"utr":"7000000002","supervisoryBody":"supervisory","membershipNumber":"0123456789","membershipExpiresOn":"2018-11-26","arn":"AARN0000002"}
```

#### Get Agent's suspension status
```
GET     /agent-assurance/agent/verify-entity
```
Response Code(s)

| Status Code | Description      |
|---|------------------|
| 200 | Default response |

#### Get Agent's suspension status on behalf of Clients
```
GET     /agent-assurance/client/verify-entity
```
Response Code(s)

| Status Code | Description      |
|---|------------------|
| 200 | Default response |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")


