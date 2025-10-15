# agent-assurance

## AgentAssuranceController

---

## GET /irSaAgentEnrolment

**Description:** Checks if the agent is enrolled for IR-SA.

### Sequence of Interactions

(No interactions)

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance

    Upstream->>+agent-assurance: GET /irSaAgentEnrolment
    agent-assurance-->>-Upstream: 204 NoContent
```

---

## GET /activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference

**Description:** Checks for an active CESA relationship for a given UTR.

### Sequence of Interactions

1. **API Call:** `GET /registration/relationship/:identifierType/:identifier` to `des`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant des

    Upstream->>+agent-assurance: GET /activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference
    agent-assurance->>+des: GET /registration/relationship/:identifierType/:identifier
    des-->>-agent-assurance: Response
    agent-assurance-->>-Upstream: Final Response
```

---

## GET /activeCesaRelationship/nino/:nino/saAgentReference/:saAgentReference

**Description:** Checks for an active CESA relationship for a given NINO.

### Sequence of Interactions

1. **API Call:** `GET /registration/relationship/:identifierType/:identifier` to `des`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant des

    Upstream->>+agent-assurance: GET /activeCesaRelationship/nino/:nino/saAgentReference/:saAgentReference
    agent-assurance->>+des: GET /registration/relationship/:identifierType/:identifier
    des-->>-agent-assurance: Response
    agent-assurance-->>-Upstream: Final Response
```

---

## GET /acceptableNumberOfClients/service/IR-PAYE

**Description:** Checks if the agent has an acceptable number of PAYE clients.

### Sequence of Interactions

1. **API Call:** `GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count` to `enrolment-store-proxy`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant enrolment-store-proxy

    Upstream->>+agent-assurance: GET /acceptableNumberOfClients/service/IR-PAYE
    agent-assurance->>+enrolment-store-proxy: GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count
    enrolment-store-proxy-->>-agent-assurance: Response
    agent-assurance-->>-Upstream: Final Response
```

---

## GET /acceptableNumberOfClients/service/IR-SA

**Description:** Checks if the agent has an acceptable number of IR-SA clients.

### Sequence of Interactions

1. **API Call:** `GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count` to `enrolment-store-proxy`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant enrolment-store-proxy

    Upstream->>+agent-assurance: GET /acceptableNumberOfClients/service/IR-SA
    agent-assurance->>+enrolment-store-proxy: GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count
    enrolment-store-proxy-->>-agent-assurance: Response
    agent-assurance-->>-Upstream: Final Response
```

---

## GET /acceptableNumberOfClients/service/HMCE-VATDEC-ORG

**Description:** Checks if the agent has an acceptable number of VAT DEC clients.

### Sequence of Interactions

1. **API Call:** `GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count` to `enrolment-store-proxy`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant enrolment-store-proxy

    Upstream->>+agent-assurance: GET /acceptableNumberOfClients/service/HMCE-VATDEC-ORG
    agent-assurance->>+enrolment-store-proxy: GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count
    enrolment-store-proxy-->>-agent-assurance: Response
    agent-assurance-->>-Upstream: Final Response
```

---

## GET /acceptableNumberOfClients/service/IR-CT

**Description:** Checks if the agent has an acceptable number of IR-CT clients.

### Sequence of Interactions

1. **API Call:** `GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count` to `enrolment-store-proxy`

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-assurance
    participant enrolment-store-proxy

    Upstream->>+agent-assurance: GET /acceptableNumberOfClients/service/IR-CT
    agent-assurance->>+enrolment-store-proxy: GET /enrolment-store-proxy/enrolment-store/enrolments/service/:service/client-count
    enrolment-store-proxy-->>-agent-assurance: Response
    agent-assurance-->>-Upstream: Final Response
```
