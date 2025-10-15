# agent-assurance

## AgentServicesController

---

## `GET /agent/agency-details/arn/:arn`

**Description:** Retrieves agency details for a given ARN.

### Sequence of Interactions

1. **API Call:** `GET /registration/personal-details/arn/:arn` to `des` - Get agent record from DES

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant des

    Upstream->>+agent-assurance: GET /agent/agency-details/arn/:arn
    agent-assurance->>+des: GET /registration/personal-details/arn/:arn
    des-->>-agent-assurance: Agent record
    agent-assurance-->>-Upstream: Final Response
```

---

## `POST /agent/agency-details/arn/:arn`

**Description:** Submits agency details to DMS.

### Sequence of Interactions

1. **API Call:** `POST /dms-submission/submit` to `dms-submission` - Submit document to DMS

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant dms-submission

    Upstream->>+agent-assurance: POST /agent/agency-details/arn/:arn
    agent-assurance->>+dms-submission: POST /dms-submission/submit
    dms-submission-->>-agent-assurance: Submission confirmation
    agent-assurance-->>-Upstream: Final Response
```
