# agent-assurance

## EntityCheckController

---

## `POST /agent/verify-entity`

**Description:** Verifies an agent's entity based on their ARN.

### Sequence of Interactions

1. **API Call:** `GET /registration/personal-details/arn/:arn` to `des` - Get agent record from DES

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant des

    Upstream->>+agent-assurance: POST /agent/verify-entity
    agent-assurance->>+des: GET /registration/personal-details/arn/:arn
    des-->>-agent-assurance: Agent record
    agent-assurance-->>-Upstream: Final Response
```

---

## `POST /client/verify-entity`

**Description:** Verifies an agent's entity based on an identifier provided by a client.

### Sequence of Interactions

1. **API Call:** `GET /registration/personal-details/arn/:arn` to `des` - Get agent record from DES

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant des

    Upstream->>+agent-assurance: POST /client/verify-entity
    agent-assurance->>+des: GET /registration/personal-details/arn/:arn
    des-->>-agent-assurance: Agent record
    agent-assurance-->>-Upstream: Final Response
```
