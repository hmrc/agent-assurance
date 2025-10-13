# agent-assurance

## R2dwController

---

## `GET /refusal-to-deal-with`

**Description:** Retrieves a paginated list of UTRs on the refusal to deal with list.

### Sequence of Interactions

1. **Database:** Read: Find properties in the 'refusal-to-deal-with' collection from `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: GET /refusal-to-deal-with
    agent-assurance->>+agent-assurance-mongo: Read: Find properties in refusal-to-deal-with collection
    agent-assurance-mongo-->>-agent-assurance: List of properties
    agent-assurance-->>-Upstream: Final Response
```

---

## `GET /refusal-to-deal-with/utr/:identifier`

**Description:** Checks if a UTR is on the refusal to deal with list.

### Sequence of Interactions

1. **Database:** Read: Check if property exists in the 'refusal-to-deal-with' collection from `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: GET /refusal-to-deal-with/utr/:identifier
    agent-assurance->>+agent-assurance-mongo: Read: Check if property exists in refusal-to-deal-with collection
    agent-assurance-mongo-->>-agent-assurance: Existence result
    agent-assurance-->>-Upstream: Final Response
```

---

## `POST /refusal-to-deal-with`

**Description:** Creates a refusal to deal with property.

### Sequence of Interactions

1. **Database:** Create: Upsert a property into the 'refusal-to-deal-with' collection in `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: POST /refusal-to-deal-with
    agent-assurance->>+agent-assurance-mongo: Create: Upsert property into refusal-to-deal-with collection
    agent-assurance-mongo-->>-agent-assurance: Confirmation
    agent-assurance-->>-Upstream: Final Response
```

---

## `DELETE /refusal-to-deal-with/utr/:identifier`

**Description:** Deletes a refusal to deal with property.

### Sequence of Interactions

1. **Database:** Delete: Delete a property from the 'refusal-to-deal-with' collection in `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: DELETE /refusal-to-deal-with/utr/:identifier
    agent-assurance->>+agent-assurance-mongo: Delete: Delete property from refusal-to-deal-with collection
    agent-assurance-mongo-->>-agent-assurance: Confirmation
    agent-assurance-->>-Upstream: Final Response
```
