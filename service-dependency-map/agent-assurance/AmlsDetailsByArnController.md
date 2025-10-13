# agent-assurance

## AmlsDetailsByArnController

---

## `GET /amls/arn/:arn`

**Description:** Retrieves AMLS details for a given ARN.

### Sequence of Interactions

1. **Database:** Read: Get UK or Overseas AMLS details by ARN from `agent-assurance-mongo`
2. **API Call:** `GET /anti-money-laundering/subscription/:amlsRegistrationNumber/status` to `des` - Get AMLS subscription status

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo
    participant des

    Upstream->>+agent-assurance: GET /amls/arn/:arn
    agent-assurance->>+agent-assurance-mongo: Read: Get UK or Overseas AMLS details by ARN
    agent-assurance-mongo-->>-agent-assurance: AMLS details
    agent-assurance->>+des: GET /anti-money-laundering/subscription/:amlsRegistrationNumber/status
    des-->>-agent-assurance: AMLS subscription status
    agent-assurance-->>-Upstream: Final Response
```

---

## `POST /amls/arn/:arn`

**Description:** Stores AMLS details for a given ARN.

### Sequence of Interactions

1. **Database:** Create/Update: Store AMLS request details in `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: POST /amls/arn/:arn
    agent-assurance->>+agent-assurance-mongo: Create/Update: Store AMLS request details
    agent-assurance-mongo-->>-agent-assurance: Confirmation
    agent-assurance-->>-Upstream: Final Response
```
