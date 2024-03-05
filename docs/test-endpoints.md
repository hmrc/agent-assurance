# Test endpoints

## Acceptable number of clients
```
GET        /test-only/acceptableNumberOfClients/service/:service/minimumAcceptableNumberOfClients/:minimumAcceptableNumberOfClients
```
// TODO

## AMLS endpoint
```
POST       /test-only/add-amls-data 
```

### Example JSON

#### Overseas
Note: isHmrc is ignored, doesn't matter
```json
{
  "isUk": false,
  "membershipNumber": "AMLS Body 101",
  "isHmrc": true
}
```

#### UK, no membership number, no expiry date (pending)
```json
{
  "isUk": true,
  "membershipNumber": "",
  "isHmrc": true
}
```

#### UK, with membership number and with expiry date
```json
{
  "isUk": true,
  "membershipNumber": "anyt41ng",
  "isHmrc": false,
  "isExpired": true
}
```

#### Stubbed AMLS HMRC membership numbers

These have a response from the existing agents-external-stubs endpoint that mocks the HMRC AMLS service

`/anti-money-laundering/subscription/:amlsRegistrationNumber/status`

 - "XAML00000100000" //Pending
 - "XAML00000200000" //Approved
 - "XAML00000300000" //Suspended
 - "XAML00000400000" //Rejected


Response Codes

| Status Code | Description                                                           |
|-------------|-----------------------------------------------------------------------|
| 201         | New AMLS record has been inserted with the ARN of the logged in agent |
| 400         | JSON for AmlsDataRequest is invalid                                   |
| 500         | Unexpected server error during create AMLS record                     |

