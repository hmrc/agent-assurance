# agent-assurance

## MaaController

---

## `GET /manually-assured`

**Description:** Retrieves a paginated list of manually assured UTRs.

### Sequence of Interactions

1. **Database:** Read: Find properties in the 'manually-assured' collection from `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: GET /manually-assured
    agent-assurance->>+agent-assurance-mongo: Read: Find properties in manually-assured collection
    agent-assurance-mongo-->>-agent-assurance: List of properties
    agent-assurance-->>-Upstream: Final Response
```

---

## `GET /manually-assured/utr/:identifier`

**Description:** Checks if a UTR is manually assured.

### Sequence of Interactions

1. **Database:** Read: Check if property exists in the 'manually-assured' collection from `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: GET /manually-assured/utr/:identifier
    agent-assurance->>+agent-assurance-mongo: Read: Check if property exists in manually-assured collection
    agent-assurance-mongo-->>-agent-assurance: Existence result
    agent-assurance-->>-Upstream: Final Response
```

---

## `POST /manually-assured`

**Description:** Creates a manually assured property.

### Sequence of Interactions

1. **Database:** Create: Upsert a property into the 'manually-assured' collection in `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: POST /manually-assured
    agent-assurance->>+agent-assurance-mongo: Create: Upsert property into manually-assured collection
    agent-assurance-mongo-->>-agent-assurance: Confirmation
    agent-assurance-->>-Upstream: Final Response
```

---

## `DELETE /manually-assured/utr/:identifier`

**Description:** Deletes a manually assured property.

### Sequence of Interactions

1. **Database:** Delete: Delete a property from the 'manually-assured' collection in `agent-assurance-mongo`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: DELETE /manually-assured/utr/:identifier
    agent-assurance->>+agent-assurance-mongo: Delete: Delete property from manually-assured collection
    agent-assurance-mongo-->>-agent-assurance: Confirmation
    agent-assurance-->>-Upstream: Final Response
```
