# agent-assurance

## ManagedUtrsController

---

## GET /managed-utrs/collection/:collection

**Description:** Lists UTRs for a given collection with pagination.

### Sequence of Interactions

1. **Database:** Read: Find UTRs in the specified collection.
2. **API Call:** `GET /registration/business-details/utr/:utr` to `des` to get business names.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo
    participant des

    Upstream->>+agent-assurance: GET /managed-utrs/collection/:collection
    agent-assurance->>+agent-assurance-mongo: Read: Find UTRs in collection
    agent-assurance-mongo-->>-agent-assurance: UTRs
    agent-assurance->>+des: GET /registration/business-details/utr/:utr
    des-->>-agent-assurance: Business Names
    agent-assurance-->>-Upstream: Paginated list of UTRs with business names
```

---

## POST /managed-utrs/collection/:collection

**Description:** Adds or updates a UTR in a specified collection.

### Sequence of Interactions

1. **Database:** Create/Update: Upsert the UTR property into the specified collection.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: POST /managed-utrs/collection/:collection
    agent-assurance->>+agent-assurance-mongo: Create/Update: Upsert UTR
    agent-assurance-mongo-->>-agent-assurance: Success
    agent-assurance-->>-Upstream: 201 Created
```

---

## GET /managed-utrs/utr/:utr

**Description:** Gets details for a specific UTR, including assurance status and optionally the business name.

### Sequence of Interactions

1. **Database:** Read: Check if UTR exists in the 'manually-assured' collection.
2. **Database:** Read: Check if UTR exists in the 'refusal-to-deal-with' collection.
3. **API Call:** `GET /registration/business-details/utr/:utr` to `des` to get business name if requested.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo
    participant des

    Upstream->>+agent-assurance: GET /managed-utrs/utr/:utr
    agent-assurance->>+agent-assurance-mongo: Read: Check if UTR is in 'manually-assured'
    agent-assurance-mongo-->>-agent-assurance: Boolean
    agent-assurance->>+agent-assurance-mongo: Read: Check if UTR is in 'refusal-to-deal-with'
    agent-assurance-mongo-->>-agent-assurance: Boolean
    alt if nameRequired is true
        agent-assurance->>+des: GET /registration/business-details/utr/:utr
        des-->>-agent-assurance: Optional Business Name
    end
    agent-assurance-->>-Upstream: UTR Details
```

---

## DELETE /managed-utrs/collection/:collection/utr/:utr

**Description:** Removes a UTR from a specified collection.

### Sequence of Interactions

1. **Database:** Delete: Delete the UTR property from the specified collection.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant agent-assurance-mongo

    Upstream->>+agent-assurance: DELETE /managed-utrs/collection/:collection/utr/:utr
    agent-assurance->>+agent-assurance-mongo: Delete: Remove UTR from collection
    agent-assurance-mongo-->>-agent-assurance: Success
    agent-assurance-->>-Upstream: 204 NoContent
```
