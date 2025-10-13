# GitHub Copilot Instructions for the Agents Architecture Repository

## About This Project
This repository contains the architectural documentation, service dependency maps, and API definitions for the HMRC Agents Platform.

## How to Understand API Endpoints
To understand the behavior of a specific API endpoint, do not read the source code directly. Instead, refer to the structured documentation located in `service-dependency-map/`.

The documentation for each microservice is organized as follows:

`service-dependency-map/<microservice-name>/`

Inside each microservice directory, you will find:
1.  **JSON Definitions (`*.json`):** These are the primary source of truth. They contain the structured, machine-readable definition of each controller's endpoints and their interaction sequences.
2.  **Markdown Documentation (`*.md`):** Human-readable documentation with embedded sequence diagrams.
3.  **Mermaid Diagrams (`*.mmd`):** The raw sequence diagrams for each endpoint.

**Your Workflow:**
1.  When asked about an API, first locate the relevant microservice directory.
2.  Find the JSON file corresponding to the controller in question.
3.  Use the information in the JSON file as the primary source to answer questions about the API's dependencies, logic, and sequence of operations.
