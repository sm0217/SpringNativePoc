Multi-Tenant Scheme Verification Platform

Architecture, bounded contexts, API contract, OAuth flow and security decisions

Status: Proposed target architecture for review
Audience: Solution architects, security architects, platform
engineers, scheme stakeholders
Purpose: Consolidate the proposed design into a single document
suitable for further refinement in Microsoft Copilot.

> **Important assumption to validate:** The platform provider is
> registered with the scheme and is permitted to act as the OAuth/OIDC
> client or broker for multiple downstream relying-party businesses
> (tenants). This must be explicitly confirmed with the scheme and
> participating authorization servers before implementation. If each
> downstream business must remain the OAuth client, callback ownership,
> token exchange and result delivery must be redesigned accordingly.

────────

1. Executive summary

Build a multi-tenant verification platform that offers relying-party
businesses (for example, Waitrose) a simplified verification capability.
The platform owns a hosted, white-label authorization-server selector
and coordinates authorization journeys with multiple independent
authorization servers (ASs) / identity providers (IdPs).

The platform exposes: - A lightweight Node.js backend SDK for
server-to-server integration. - A hosted white-label journey UI for
provider selection and journey continuation. - A stable API for creating
journeys, checking status and retrieving authorized verification
results. - A centrally governed, versioned representation of scheme use
cases, claim requirements, provider capabilities and scope mappings. -
Audit-event coordination and reconciliation, without silently assuming
another participant’s regulatory obligations.

The intended user experience is: a tenant asks to verify a business
attribute (e.g. age over 18), the platform creates a journey, the user
selects an eligible authorization server in the hosted UI, the selected
server authenticates the user and obtains consent as required, the
platform receives and validates the authorization response, and the
tenant retrieves a normalized result through an authenticated backend
API.

Core principle: the platform coordinates the journey; the scheme
defines normative rules; the authorization server remains responsible
for its authentication, consent, authorization and token issuance; and
the tenant remains responsible for its business purpose and permitted
use of the result.

2. Context, actors and trust relationships

Actors

• Tenant / downstream relying party (RP): e.g. Waitrose. A
customer of the platform, integrating its backend with the platform
API.
• Platform provider / relying-party service: the organization
operating the verification platform. Assumed to be the
scheme-registered RP and OAuth/OIDC client in the broker model.
• Scheme / regulator: defines scheme requirements and receives
audit events as required.
• Authorization server / identity provider: independent company
that authenticates the user, manages its consent experience, and
issues authorization responses/tokens/claims.
• End user: completes the authorization journey.
• SDKs and UI: integration/presentation components; neither is the
security authority.

Identity distinctions

Keep these identifiers distinct: - platformClientId / scheme RP
identity: identifies the platform to the scheme and/or authorization
servers. - tenantId: identifies Waitrose or another customer within
the platform. - journeyId: identifies one verification transaction. -
authorizationServerId: identifies the selected provider. - useCaseId
and policyVersion: identify the requested business purpose and the
policy definition applied.

The scheme may know only the platform as the registered RP, while the
platform knows the downstream tenants. That model is acceptable only if
scheme rules permit it and downstream tenant identity, data use,
consent, audit attribution and accountability are properly governed.

3. Logical architecture

flowchart TB
    subgraph RP["Tenant / Relying Party Domain"]
        WF["Tenant web / iOS / Android app"]
        WB["Tenant backend"]
        WF --> WB
    end

    subgraph SDK["Integration and presentation"]
        NS["Node.js backend SDK"]
        NATIVE["Optional native mobile UI SDK"]
        HOSTED["Hosted white-label journey UI"]
    end

    subgraph PLATFORM["Multi-tenant verification platform"]
        GW["API gateway and tenant authentication"]
        TENANT["Tenant registry and access policies"]
        JOURNEY["Journey orchestration service"]
        CATALOG["Versioned use-case and provider registry"]
        ADAPTER["Authorization-server adapters"]
        VALIDATE["Token validation and claims service"]
        RESULT["Tenant-scoped result API"]
        AUDIT["Audit and evidence service"]
        STORE[("Journey / result store")]
    end

    subgraph SCHEME["Scheme governance"]
        POLICY["Normative scheme policies"]
        REG["Scheme regulator / audit ingestion"]
    end

    subgraph AS["Independent authorization servers"]
        A["Company A"]
        B["Company B"]
        C["Company C"]
    end

    WF --> WB
    WB --> NS
    WF -. optional native UI .-> NATIVE
    NS --> GW
    NATIVE --> GW
    GW --> TENANT
    GW --> JOURNEY
    JOURNEY --> CATALOG
    JOURNEY <--> HOSTED
    JOURNEY --> ADAPTER
    ADAPTER <--> A
    ADAPTER <--> B
    ADAPTER <--> C
    ADAPTER --> VALIDATE
    VALIDATE --> RESULT
    JOURNEY --> STORE
    VALIDATE --> STORE
    RESULT --> STORE
    NS --> RESULT
    POLICY -. governs .-> CATALOG
    POLICY -. requirements .-> A
    POLICY -. requirements .-> B
    POLICY -. requirements .-> C
    JOURNEY --> AUDIT
    VALIDATE --> AUDIT
    AUDIT --> REG
    A --> REG
    B --> REG
    C --> REG

This is a logical view, not a mandated microservice topology. Start with
a modular application if appropriate; split deployment units when
security isolation, scaling, reliability or team ownership requires it.

4. Bounded contexts and responsibilities

4.1 Tenant management and access control

Owns tenant onboarding, tenant identity, API client registration,
credentials, scopes/permissions, registered return URLs, branding
configuration, tenant status, quotas and revocation.

Invariant: tenant identity is derived from validated
credentials/token claims, never trusted from a caller-supplied
tenantId.

4.2 Verification journey management

Owns journey creation, lifecycle, expiration, transaction correlation,
selected provider, completion state and idempotency.

Suggested states:

CREATED -> PROVIDER_SELECTION -> AUTHORIZATION_PENDING
        -> CALLBACK_RECEIVED -> RESULT_VALIDATION
        -> COMPLETED

Alternative terminal states: DENIED, CANCELLED, EXPIRED, FAILED

State transitions must be validated server-side. Duplicate callbacks and
retries must be idempotent.

4.3 Scheme and provider integration

Owns the platform’s scheme-facing RP/client identity, provider metadata,
OAuth/OIDC integrations, provider adapters, protocol differences,
provider-specific error mapping and scheme conformance.

It must not assume every provider supports the same scopes, claims,
consent semantics or callback mechanisms.

4.4 Use-case and policy catalogue

Owns a versioned operational representation of scheme use cases,
mandatory and optional claims, evidence requirements, permitted claim
selection, provider eligibility and provider-specific mappings.

The scheme remains the normative authority. If the scheme has no
machine-readable catalogue, the platform’s representation must have an
explicit policy owner, approval process, versioning, effective dates and
conformance tests.

4.5 Claims and verification results

Owns token validation, claim provenance, normalization, claim-level
disclosure policy, verification outcome and result schema.

Distinguish: - an attribute asserted by an authorization server; -
successful validation of the upstream token and claim; - a
scheme-defined verification conclusion derived from evidence.

Do not mark a journey successful merely because the OAuth exchange
completed.

4.6 Journey presentation

Owns the hosted selector UI, branding, localization, accessibility,
responsive layout and browser/mobile handoff.

The UI may request eligible provider options and submit a provider
choice. It is not the authority for tenant identity, eligibility, claim
access or successful completion.

4.7 Audit and regulatory evidence

Owns event schema validation, correlation, durable delivery, retry,
idempotency, delivery status and reconciliation. Preserve which
participant asserted each event. Do not assume that the platform can
take over audit obligations assigned by the scheme to tenants or
authorization servers.

5. Use cases, claims and scopes

Use a generic verification API rather than a separate SDK method for
every use case. The platform resolves mandatory claims and permitted
optional claims from the policy catalogue.

Example use cases: - Personal details: e.g. name, date of birth, address
and age-over-18 evidence may all be mandatory. - Payment verification:
account ownership mandatory; other attributes may be optional. - Address
verification: verified address mandatory; supporting evidence may be
optional. - Passport verification: passport name and expiry mandatory;
photo optional.

These are examples only. Actual rules must come from the scheme.

Example policy representation

{
  "useCaseId": "passport-verification",
  "policyVersion": 3,
  "claims": {
    "passport.name": { "requirement": "mandatory" },
    "passport.expiryDate": { "requirement": "mandatory" },
    "passport.photo": { "requirement": "optional" }
  },
  "providerMappings": {
    "company-b": {
      "supportedClaims": [
        "passport.name",
        "passport.expiryDate",
        "passport.photo"
      ],
      "scopeMapping": {
        "passport.name": ["passport.read"],
        "passport.expiryDate": ["passport.read"],
        "passport.photo": ["passport.photo.read"]
      }
    }
  }
}

The mapping is illustrative. Provider scopes and claims must reflect
actual provider capabilities and scheme rules.

Important distinctions

• Use case: business purpose requested by the tenant.
• Claim: attribute or evidence requested/returned.
• OAuth scope: provider-specific authorization mechanism; not
necessarily one-to-one with a claim.
• Mandatory request: the claim must be requested.
• Mandatory outcome: the journey cannot be reported successful
unless the required claim/evidence is present and valid.
• Consent: the authorization server applies its own consent and
authorization obligations.

The platform should filter out providers that cannot satisfy mandatory
requirements before showing the selector. It must also revalidate the
selected provider when the transaction starts.

6. External API and Node.js SDK

Suggested SDK methods

const scheme = new SchemeClient({
  // Backend-only credential/configuration.
  apiKey: process.env.SCHEME_API_KEY!
});

const useCases = await scheme.verification.getUseCases();

const requirements =
  await scheme.verification.getRequirements({
    useCase: "passport-verification"
  });

const journey = await scheme.verification.start({
  useCase: "passport-verification",
  reference: "waitrose-session-123",
  optionalClaims: ["passport.photo"]
});

// Return this URL to the tenant frontend for browser navigation.
return { redirectUrl: journey.redirectUrl };

The API key example is illustrative. For production, prefer OAuth client
credentials with appropriate client authentication, scopes, token
lifetime, rotation and revocation. The SDK must be used on the tenant
backend; never expose its secret credentials in browser or mobile
application code.

Suggested operations: - verification.getUseCases() -
verification.getRequirements({ useCase }) -
verification.start({ useCase, reference, optionalClaims? }) -
verification.getStatus({ journeyId }) -
verification.getResult({ journeyId }) -
verification.cancel({ journeyId }), if supported

These are business-level methods. The SDK should not contain the
provider registry, authoritative policy, selector UI, OAuth
orchestration engine or token-validation rules. The server must enforce
all security decisions independently.

Suggested API endpoints

GET    /v1/use-cases
GET    /v1/use-cases/{useCaseId}/requirements
GET    /v1/use-cases/{useCaseId}/authorization-servers
POST   /v1/verification-journeys
GET    /v1/verification-journeys/{journeyId}
GET    /v1/verification-journeys/{journeyId}/result
POST   /v1/verification-journeys/{journeyId}/cancel
POST   /v1/audit-events                 (only where appropriate)

All endpoints require suitable authentication and authorization.
Administrative onboarding/configuration endpoints should be separated
from runtime APIs.

7. Full end-to-end sequence

Assumption: the platform is allowed to be the OAuth/OIDC client for the
relevant authorization servers. The authorization code is returned to
the platform’s registered callback.

sequenceDiagram
    autonumber
    actor User
    participant RP as Waitrose App
    participant SDK as Node SDK
    participant API as Verification API
    participant Policy as Policy / Provider Registry
    participant UI as Hosted White-label UI
    participant AS as Selected Authorization Server
    participant Audit as Audit Service
    participant Reg as Scheme Regulator

    User->>RP: Select "Verify age"
    RP->>SDK: verification.start(useCase, reference)
    SDK->>API: Create journey using tenant credentials
    API->>API: Authenticate tenant and authorize use case
    API->>Policy: Resolve requirements and eligible providers
    Policy-->>API: Policy version, mandatory claims, optional claims, providers
    API->>API: Create tenant-bound journey
    API-->>SDK: journeyId + hosted URL
    SDK-->>RP: Journey details
    RP-->>User: Redirect to hosted URL

    User->>UI: Open hosted journey
    UI->>API: Load journey context
    API-->>UI: Branding and eligible provider options
    User->>UI: Select Company B
    UI->>API: Submit provider selection
    API->>Policy: Revalidate eligibility and capability
    Policy-->>API: Selection permitted
    API->>API: Create OAuth transaction
    API-->>User: Redirect to authorization endpoint

    User->>AS: Authenticate and review consent
    AS->>AS: Apply authorization and consent rules
    AS-->>API: Callback with authorization code and state
    API->>API: Validate transaction, state and issuer
    API->>AS: Exchange code using registered client authentication
    AS-->>API: ID token / authorized claims
    API->>API: Validate token and claim provenance
    API->>Policy: Validate mandatory claim/evidence requirements
    Policy-->>API: Satisfied or failed
    API->>Audit: Record platform events
    Audit->>Reg: Deliver applicable platform events
    AS->>Reg: Deliver authorization-server events

    API-->>User: Redirect to registered tenant completion URL
    RP->>SDK: Request journey result
    SDK->>API: Authenticated result request
    API->>API: Check tenant, journey, status and claim permissions
    API-->>SDK: Normalized verification result
    SDK-->>RP: Return result
    RP->>RP: Apply business decision

If the scheme requires each tenant to be the OAuth client, revise the
callback and token exchange: the authorization code would go to the
tenant’s registered callback, and the tenant would exchange it. Do not
implement the broker flow until the scheme and authorization servers
explicitly support it.

8. Hosted white-label UI and mobile

Recommended initial model

• Host the selector and journey UI on a platform-controlled domain.
• Return a short-lived hosted journey URL from verification.start().
• Load branding and eligible providers using server-side journey
context.
• Validate provider selection again on the backend.
• Redirect only to registered/approved destinations.
• After completion, redirect to the tenant’s registered completion URL
with an opaque journey reference.

White-label configuration

Per tenant, configure approved brand name/logo/assets, colours, journey
copy, localization, eligible use cases, eligible providers, registered
completion URLs and other approved presentation settings. Do not trust
browser-supplied tenant identifiers or arbitrary logo URLs.

Mobile options

• Hosted UI: easiest cross-platform initial option; browser-based
selector followed by the supported provider app/browser handoff.
• Native selector SDK: optional iOS/Android presentation layer
using the same platform journey APIs and provider registry.
• Embedded web UI: optional if there is a clear need; assess OAuth
user-agent requirements and avoid unsafe embedded authentication web
views.

The UI technology must not alter the backend trust model. Use supported
system-browser or app-to-app mechanisms, with validated
callback/deep-link handling.

9. Tenant authentication and API security

Recommended baseline

• Register a distinct API client identity for each tenant.
• Use OAuth 2.0 Client Credentials for backend-to-backend access.
• Prefer asymmetric client authentication (private_key_jwt or mTLS)
for higher-assurance integrations where operationally appropriate.
• Issue short-lived, audience-restricted access tokens with explicit
scopes.
• Use mTLS where required by the scheme or justified by the threat
model; mTLS and OAuth client credentials are complementary, not
mutually exclusive.
• Keep credentials only in tenant backend secret storage.
• Rotate and revoke credentials, rate-limit APIs and audit access.

Tenant isolation

For each result request: 1. Authenticate the calling client. 2. Derive
tenant identity from the validated credential/token. 3. Load the journey
by ID. 4. Check that the journey belongs to the authenticated tenant. 5.
Check journey state and required permissions. 6. Apply claim-level
disclosure policy. 7. Return only the authorized result.

Return a generic not-found response for both nonexistent and
other-tenant journeys if appropriate to avoid existence disclosure. A
journey ID is a transaction identifier, not an authorization credential.

10. Result contract and token handling

By default, do not return the original upstream ID token to the
tenant. An ID token issued to the platform has the platform as its
intended audience; it is not automatically a valid ID token for
Waitrose.

Return a normalized verification result, for example:

{
  "journeyId": "jrn_abc123",
  "status": "completed",
  "useCaseId": "age-verification",
  "policyVersion": 3,
  "verification": {
    "ageOver18": {
      "status": "verified",
      "value": true
    }
  },
  "evidence": {
    "issuer": "company-b",
    "assuranceLevel": "scheme-defined-level"
  }
}

The exact schema must follow scheme definitions. Minimize data, preserve
provenance and distinguish: - upstream assertion; - successful
token/claim validation; - derived verification conclusion.

If downstream parties need portable cryptographic evidence, define a
separate signed verification assertion with explicit issuer, audience,
expiry and claim scope, subject to scheme approval.

11. Audit and regulatory evidence

Separate the events and responsibilities: - Tenant/RP events:
initiation of the business journey and receipt/use of the result, where
required. - Authorization-server events: authentication, consent,
authorization and token issuance, as defined by the scheme. - Platform
events: journey creation, provider selection, callback processing,
validation outcome, result delivery and orchestration failures.

Use a shared correlation ID where permitted, but preserve which
participant asserted each event. Implement durable delivery, retry,
idempotency, delivery status and reconciliation. Do not assume the
platform can take over obligations assigned to another participant.
Whether audit delivery must be synchronous is a scheme-specific
decision.

12. Security threats and required mitigations

────────

Threat                              Required control

────────

Cross-tenant result disclosure      Authenticated tenant identity and
per-request journey ownership
checks

Authorization-code injection/replay Transaction-bound state, exact
redirect URIs, PKCE where
applicable, issuer validation and
single-use code handling

Token substitution or invalid       Signature, issuer, audience,
claims                              expiry, nonce where applicable, and
protocol-specific validation

Unauthorized claim disclosure       Use-case policy, tenant entitlement
checks and data minimization

Forged completion redirect          Redirect reference is not proof of
success; result retrieved
server-to-server

Credential or token compromise      Short-lived tokens,
rotation/revocation, asymmetric
authentication where appropriate

Duplicate callbacks/retries         Idempotent transaction state
machine

Audit loss or duplication           Durable event handling, retry,
idempotency and reconciliation

Stale or inconsistent scheme policy Versioned policies, controlled
rollout, conformance tests and
effective dates

Provider outage                     Timeouts, bounded retries, circuit
breaking and explicit failure
states

Open redirect / malicious UI        Registered return URLs, approved configuration                       assets and server-side validation

13. Approval conditions and open decisions

The architecture is a proposed target, not proof of scheme
compliance. Before production approval, confirm:

1. Scheme acceptance: may the platform act as the scheme-registered
RP/OAuth client for multiple downstream tenants?
2. Authorization-server acceptance: are client registration,
redirect, token exchange and claim-disclosure arrangements
supported?
3. Policy authority: who owns the normative use-case catalogue, how
is it distributed, and how are versions enforced?
4. Consent and data rights: what must the user be told, what can be
disclosed to each tenant, and who is accountable for downstream use?
5. Audit responsibility: which participant submits each event, what
is mandatory, and what delivery guarantees apply?
6. Client authentication: what does the scheme require for
tenant-to-platform and platform-to-provider connections?
7. Result trust model: is a normalized API result sufficient, or is
signed portable evidence required?
8. Mobile handoff: what authorization-server app-to-app mechanisms
and callback schemes are approved?
9. Retention and privacy: what personal data, tokens and evidence
may be stored, for how long, and with what access controls?
10. Operational assurance: threat model, independent penetration
testing, tenant-isolation tests, OAuth/OIDC conformance and
incident/recovery procedures.

14. Recommended implementation sequence

1. Confirm the scheme’s broker/client model and audit obligations.
2. Define tenant identity, client authentication and access-control
model.
3. Agree the use-case/claim policy schema and provider capability
contract.
4. Implement the journey state machine and provider adapter interface.
5. Implement token validation, normalized result schema and
tenant-scoped result API.
6. Implement the hosted white-label UI and registered return
destinations.
7. Add audit delivery, reconciliation and operational monitoring.
8. Publish the lightweight Node.js SDK.
9. Run conformance, threat-model and tenant-isolation tests.
10. Consider native mobile selector SDKs after the hosted journey is
proven.

15. Architecture decision summary

Recommended design: a multi-tenant verification platform, with the
platform as scheme-facing relying party/OAuth client (subject to
explicit scheme approval), a versioned scheme-policy representation,
provider adapters, hosted white-label selector, lightweight Node.js
backend SDK, normalized tenant-scoped result API and auditable journey
lifecycle.

Do not: - expose upstream ID tokens to tenants by default; - trust
journey IDs as credentials; - trust browser-provided tenant identity; -
hard-code use-case rules in SDKs; - let the orchestration platform
override scheme or authorization-server decisions; - assume audit
responsibilities can be transferred to the platform without scheme
agreement.

Primary architectural principle: centralize the integration and
journey experience, but keep identity, authorization, claim provenance,
tenant entitlement and regulatory accountability explicit and
independently enforceable.