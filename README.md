Below is a prompt you can reuse with another model or internally, followed by a document-style answer that reflects the conclusions we reached and respects your constraints.

Security Architecture Review Prompt

Act as a Principal Security Architect performing a security review of an enterprise digital identity platform.

Context and constraints:

* The platform acts as an OAuth 2.0 Authorization Server.
* Relying parties initiate authorization using PAR (Pushed Authorization Requests).
* A consent record is created at PAR time and assigned a consent_id.
* The consent database is shared between OAuth and consent services and contains both consent_id and request_uri.
* consent_id is the primary transaction identifier used internally.
* consent_id is single-use. Once the transaction completes successfully or reaches a terminal failure state, the same consent_id cannot be reused.
* There is no refresh-token-based continuation of the same consent transaction.
* The platform contains three major service domains:
    * Authentication Service
    * OAuth/Authorization Service
    * Consent Service
* Authentication is performed through separate APIs:
    * preAuth
    * authIdentifier
    * authPassword
    * authMI
    * authSMS
    * reAuthSMS
    * authToken
* API names cannot be changed.
* No new microservices may be introduced.
* No new databases may be introduced.
* No new columns may be added to existing databases.
* No sticky sessions exist.
* The downstream authentication system generates an auth_session_id.
* The auth_session_id must be exposed to the application for analytics purposes.
* There is no authoritative server-side store that maps consent_id to auth_session_id.
* The web channel may use a secure device-binding cookie.
* A transaction JWT may be used between APIs.
* The frontend must not control authentication progression.
* OAuth transaction state is stored in the existing consent/OAuth database.

Produce:

1. A target-state security architecture.
2. Security recommendations.
3. Threat mitigations.
4. JWT design recommendations.
5. Session and transaction binding recommendations.
6. Replay protection controls.
7. Audit and monitoring recommendations.
8. Final implementation guidance that works within the stated constraints.

And here is the document content itself:

Security Architecture Recommendations

Overview

The platform should treat consent_id as the primary authorization transaction identifier and maintain a clear separation between:

* OAuth transaction context
* Authentication runtime context
* Consent decision state

Although all three participate in the same business flow, they should not be treated as interchangeable security artifacts.

Transaction Model

The consent_id is created during PAR processing and acts as the authoritative transaction identifier throughout the lifecycle.

The transaction lifecycle is:

PENDING
→ AUTH_IN_PROGRESS
→ GRANTED
→ AUTHORIZATION_CODE_ISSUED
→ CONSUMED

Terminal failure states may also exist where the transaction can no longer be reused.

Once a consent_id reaches a terminal state, it must never be accepted again.

Transaction JWT Design

The transaction JWT should be issued by preAuth and reused throughout the transaction lifecycle.

The JWT should contain:

* consent_id
* request_uri
* client_id
* issuance time
* expiry time

The JWT must not contain:

* authentication progress
* next step information
* OTP completion state
* MI completion state
* workflow control information

The JWT acts as a context carrier only and must not become the source of truth for workflow progression.

Authentication Session Binding

The architecture does not contain an authoritative server-side mapping between consent_id and auth_session_id.

Therefore, once the downstream authentication system creates an auth_session_id, the platform should issue a new signed transaction JWT that additionally contains:

* auth_session_id

This creates a cryptographically signed association between:

* consent_id
* request_uri
* client_id
* auth_session_id

After this binding occurs, the JWT should remain immutable for the remainder of the authentication journey.

Frontend Security Controls

The frontend must not determine:

* current authentication step
* next authentication step
* completion status

All progression decisions must be made by backend services.

The frontend should act only as a presentation layer.

Authentication APIs

The existing APIs may remain:

* preAuth
* authIdentifier
* authPassword
* authMI
* authSMS
* reAuthSMS
* authToken

Every API should:

* validate the transaction JWT
* validate JWT expiry
* validate consent_id state
* validate device-binding cookie where applicable
* validate auth_session_id consistency once present

No API should trust frontend-provided workflow state.

Replay Protection

The following artifacts must be protected against replay:

* consent_id
* request_uri
* authorization code
* transaction JWT

Controls should include:

* single-use transaction enforcement
* terminal state validation
* short-lived JWT expiry
* authorization code one-time usage validation

Device Binding

For browser-based flows:

* issue a secure HttpOnly cookie
* mark Secure
* apply strict SameSite policy where operationally possible

The cookie should be validated on every authentication request.

The device-binding cookie should not be treated as an authentication credential but as an additional replay-reduction mechanism.

Username Enumeration Protection

The authIdentifier endpoint must not disclose:

* whether a username exists
* whether an account is active
* whether an account is locked

Responses should be uniform in:

* status code
* error structure
* observable behaviour

Rate Limiting

Apply rate limiting at:

* username level
* consent_id level
* auth_session_id level
* device level
* IP level

OTP APIs should use stricter thresholds than username identification APIs.

Audit Requirements

Every authentication event should log:

* consent_id
* request_uri
* client_id
* auth_session_id (when available)
* authentication outcome
* failure reason
* timestamp

Audit records should be generated by backend services and not depend on frontend assertions.

Handling Exposed auth_session_id

Because the downstream auth_session_id must be exposed to the application for analytics:

* treat it as a correlation identifier
* do not treat it as a credential
* do not trust it without JWT validation
* do not use it as the sole authorization input
* avoid transmitting it in URLs
* avoid logging it unnecessarily

Final Security Position

Within the stated constraints, the strongest achievable design is:

* consent_id remains the authoritative transaction identifier
* transaction JWT carries immutable transaction context
* a second JWT version is issued once auth_session_id becomes available
* backend services remain the source of truth for state transitions
* device-binding cookies provide replay resistance
* consent_id remains single-use
* authorization codes remain single-use
* frontend remains presentation-only
* no new database schema changes are required
* no new services are required

This approach provides the best balance between security, auditability, replay resistance, and operational simplicity while respecting all architectural constraints.


# SpringNativePoc


## To build docker image for Spring boot JVM

docker build -f DockerfileSpringJvm --tag=\<Image Name\>:\<Version\> .

## To build docker image for Spring boot Native Graalvm

docker build -f DockerfileSpringGraalvm --tag=\<Image Name\>:\<Version\> .

## Spinning up docker containers

docker-compose up mysql -d
docker-compose up springjvm springgraalvm quarkus -d

To run k6 test

1) Create a script.js file in your desired location. This location should be mounted in the docker compose file for k6 container.
2) Add the test depending on the use case. Sample given below to stress test Spring boot JVM image for 200 VU's. Ref - https://k6.io/docs/test-types/
   
```
import http from 'k6/http';
import {sleep} from 'k6';

export const options = {
  stages: [
    { duration: '5m', target: 100 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
    { duration: '15m', target: 100 }, // stay at 100 users for 30 minutes
    { duration: '5m', target: 0 }, // ramp-down to 0 users
  ],
};
  
export default function () {
  const url1 = 'http://springjvm:8080/api/transactions';
  const payload1 = JSON.stringify({
    "accountFrom": "Account1",
    "accountTo": "Account1",
    "customerId": "123",
    "paymentAmount": 10
    });

  const params1 = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  http.get(`http://springjvm:8080/api/transactions/all`);
  http.post(url1, payload1, params1);

  sleep(1);
}

```
3) docker-compose run k6 run <Location>/script.js
