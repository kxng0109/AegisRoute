# AegisRoute

A highly available, distributed financial transaction orchestration system. AegisRoute implements a robust Saga pattern to ensure absolute data consistency across multiple microservices, featuring self-healing compensating transactions, strict idempotency, and pessimistic database locking.

## System Architecture

The monorepo consists of three decoupled services communicating via synchronous REST and asynchronous message brokers.

* **Orchestrator Service:** The central state machine. It coordinates complex transactions, tracks the exact state of funds, catches network failures, and publishes compensating commands (refunds) when external systems drop requests.
* **Ledger Service:** The core financial engine. It securely locks user accounts during read modify write operations to prevent race conditions and validates distributed idempotency keys via Redis to eliminate duplicate transactions.
* **Provider Simulator:** A probabilistic chaos engine. It deliberately simulates external network anomalies like 503 Service Unavailable and 504 Gateway Timeout responses to validate the resilience of the Orchestrator.

## Core Engineering Patterns

* **Saga Pattern:** Orchestrates cross service transactions and executes automated rollbacks when external dependencies fail.
* **Idempotency Validation:** Utilizes distributed Redis locks bound to unique user contexts to mathematically guarantee transactions are processed exactly once.
* **Concurrency Control:** Implements PostgreSQL pessimistic write locks (FOR UPDATE) to protect financial ledgers from parallel execution race conditions.
* **Asynchronous Resilience:** Leverages RabbitMQ for guaranteed delivery of compensating transactions, equipped with strict guards against poison pill messages and infinite retry loops.

## Technology Stack

* Language: Java 25
* Framework: Spring Boot 4.0.6
* Database: PostgreSQL 18
* Message Broker: RabbitMQ
* Caching & Locks: Redis
* HTTP Client: Spring RestClient

## Prerequisites

To run this system locally, ensure the following are installed:

* JDK 25
* Maven
* Docker (for infrastructure containers)

## Boot Sequence

1.  Start the supporting infrastructure via Docker (PostgreSQL, Redis, RabbitMQ).
2.  Boot the Ledger Service (Port 8082).
3.  Boot the Provider Simulator (Port 8081).
4.  Boot the Orchestrator Service (Port 8080).

## Local Testing

All transaction requests must be routed through the Orchestrator Service, which will automatically negotiate with the Ledger and the external Provider.

Send a POST request to the Orchestrator transfer endpoint:
`http://localhost:8080/api/v1/transfers/transfer`

Monitor the `transfer_saga_states` database table to watch the state machine track the transaction lifecycle in real time.

## Author

Joshua Ike