# agent-assurance

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-assurance/images/download.svg) ](https://bintray.com/hmrc/releases/agent-assurance/_latestVersion)

This micro service is responsible for agents Know Your Customer operations.

## Run the application locally

To run the application execute
```
sbt run
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

#### Check if the current user has an active client/agent relationship in CESA with NINO
```
GET   	/activeCesaRelationship/nino/:nino/saAgentReference/:saAgentReference
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | The current user provides a valid NINO and an IRAgentReference identifier that matches an active agent/client relationship in CESA |
| 403 | The current user does not have any active agent/client relationship in CESA between the user and the client|

##### Example

Assuming the user has an active CESA agent relationship with IRAgentReference of SA6012.
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

#### Check if the current user has an active client/agent relationship in CESA with UTR
```
GET   	/activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference
```
Response Code(s)

| Status Code | Description |
|---|---|
| 200 | The current user provides a valid UTR and an IRAgentReference identifier that matches an active agent/client relationship in CESA |
| 403 | The current user does not have any active agent/client relationship in CESA between the user and the client|

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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")


