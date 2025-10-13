# Analysis of MongoDB Collections in agent-assurance

## 1. agent-amls

- **Repository File:** `app/uk/gov/hmrc/agentassurance/repositories/AmlsRepository.scala`
- **Purpose:** This collection stores Anti-Money Laundering Supervision (AMLS) details for agents. It maintains records of agents' AMLS registration information, which is required for compliance checks during the agent onboarding and assurance processes.
- **Schema Highlights:**
  - `utr`: The agent's Unique Taxpayer Reference
  - `amlsDetails`: Complete AMLS registration details including supervisory body and registration number
  - `createdDate`: Timestamp when the record was created
  - `lastUpdated`: Timestamp of the most recent update to the record

- **Sample Document:**

```json
{
  "utr": "1234567890",
  "amlsDetails": {
    "supervisoryBody": "Association of Chartered Certified Accountants",
    "membershipNumber": "12345678",
    "appliedOn": "2023-01-15",
    "membershipExpiresOn": "2024-01-15"
  },
  "createdDate": { "$date": "2023-01-15T10:00:00.000Z" },
  "lastUpdated": { "$date": "2023-01-15T10:00:00.000Z" }
}
```

---

## 2. overseas-agent-amls

- **Repository File:** `app/uk/gov/hmrc/agentassurance/repositories/OverseasAmlsRepository.scala`
- **Purpose:** This collection stores AMLS details specifically for overseas agents who are not UK-based but need to provide services to UK clients. It maintains similar compliance information but tailored for international regulatory requirements.
- **Schema Highlights:**
  - `arn`: The Agent Reference Number for the overseas agent
  - `amlsDetails`: AMLS registration details specific to overseas jurisdictions
  - `createdDate`: Timestamp when the record was created
  - `lastUpdated`: Timestamp of the most recent update

- **Sample Document:**

```json
{
  "arn": "XARN1234567",
  "amlsDetails": {
    "supervisoryBody": "International Supervisory Authority",
    "membershipNumber": "INT87654321",
    "appliedOn": "2023-02-01",
    "membershipExpiresOn": "2024-02-01"
  },
  "createdDate": { "$date": "2023-02-01T14:30:00.000Z" },
  "lastUpdated": { "$date": "2023-02-01T14:30:00.000Z" }
}
```

---

## 3. agent-assurance

- **Repository File:** `app/uk/gov/hmrc/agentassurance/repositories/PropertiesRepository.scala`
- **Purpose:** This collection stores configuration properties and settings for the agent assurance service. It acts as a centralized store for various operational parameters, feature flags, and system configurations that control the behavior of the assurance checks.
- **Schema Highlights:**
  - `key`: The property identifier/name
  - `value`: The property value (can be string, number, or boolean)
  - `description`: Human-readable description of what the property controls
  - `lastUpdated`: Timestamp of when the property was last modified

- **Sample Document:**

```json
{
  "key": "max-acceptable-clients-ir-sa",
  "value": "1000",
  "description": "Maximum number of IR-SA clients an agent can have",
  "lastUpdated": { "$date": "2023-03-10T09:15:00.000Z" }
}
```

---

## 4. archived-amls

- **Repository File:** `app/uk/gov/hmrc/agentassurance/repositories/ArchivedAmlsRepository.scala`
- **Purpose:** This collection stores historical AMLS records that have been archived. When AMLS details are updated or expire, the previous versions are moved here for audit trail purposes and compliance record-keeping.
- **Schema Highlights:**
  - `utr`: The agent's Unique Taxpayer Reference
  - `amlsDetails`: The archived AMLS registration details
  - `archivedDate`: Timestamp when the record was archived
  - `originalCreatedDate`: Original creation date of the AMLS record
  - `archiveReason`: Reason for archiving (e.g., "updated", "expired", "cancelled")

- **Sample Document:**

```json
{
  "utr": "1234567890",
  "amlsDetails": {
    "supervisoryBody": "Law Society",
    "membershipNumber": "OLD12345",
    "appliedOn": "2022-01-15",
    "membershipExpiresOn": "2023-01-15"
  },
  "archivedDate": { "$date": "2023-01-16T08:00:00.000Z" },
  "originalCreatedDate": { "$date": "2022-01-15T10:00:00.000Z" },
  "archiveReason": "expired"
}
```
