# Design Document — Program #4

## LLM User–Facing Ecosystem Database

_CSc 460 — Spring 2026_

---

# 1. Conceptual Database Design

## 1.1 Overview

The database models the _user-facing ecosystem_ of an LLM platform: accounts and subscription tiers, billing and invoicing, conversation and message history, personalization through personas and prompt templates, collaboration via workspaces, and customer support. The AI model itself is out of scope — the schema is the system's "memory" for user state, interaction history, and business operations.

Fifteen relations are used. The design targets **3NF** (and in fact achieves BCNF on every relation, which strictly implies 3NF).

## 1.2 Entity Sets and Attributes

| Entity                      | Attributes                                                                                                                                                           |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Tier**                    | tier_id (PK), tier_name (UNIQUE), cost, message_limit, pro_access                                                                                                    |
| **User**                    | user_id (PK), tier_id (FK), name, email (UNIQUE), creation_date, language                                                                                            |
| **BillingRecord**           | user_id (PK, FK), payment_method, billing_address                                                                                                                    |
| **Invoice**                 | invoice_id (PK), user_id (FK), tier_id (FK), invoice_date, amount, payment_status                                                                                    |
| **Workspace**               | workspace_id (PK), owner_id (FK → User), name, visibility, created_at                                                                                                |
| **WorkspaceMember**         | workspace_id (FK), user_id (FK), role, joined_at — composite PK                                                                                                      |
| **Persona**                 | persona_id (PK), owner_user_id (FK), name, instructions, created_at                                                                                                  |
| **Conversation**            | conversation_id (PK), user_id (FK), workspace_id (FK, nullable), persona_id (FK, nullable), title, start_time, status, persona_name_snapshot, persona_instr_snapshot |
| **Message**                 | message_id (PK), conversation_id (FK), sender_role, time_sent, content                                                                                               |
| **MessageFeedback**         | feedback_id (PK), message_id (FK, UNIQUE), rating, comment_text, submitted_at                                                                                        |
| **MessageBookmark**         | user_id (FK), message_id (FK), bookmarked_time — composite PK                                                                                                        |
| **PromptTemplate**          | template_id (PK), owner_user_id (FK), name, text, category, visibility, created_at                                                                                   |
| **WorkspacePromptTemplate** | workspace_id (FK), template_id (FK), shared_at — composite PK                                                                                                        |
| **SupportAgent**            | agent_id (PK), name, email (UNIQUE), specialty, hired_date                                                                                                           |
| **SupportTicket**           | ticket_id (PK), user_id (FK), agent_id (FK, nullable), topic_name, opened_time, closed_time (nullable), status                                                       |

**Tier**
| Column | Key | Constraints / Notes |
| ------------- | --- | ------------------- |
| tier_id | PK | |
| tier_name | | UNIQUE |
| cost | | |
| message_limit | | |
| pro_access | | |

**User**
| Column | Key | Constraints / References |
| ------------- | --- | ------------------------ |
| user_id | PK | |
| tier_id | FK | Tier.tier_id |
| name | | |
| email | | UNIQUE |
| creation_date | | |
| language | | |

**BillingRecord**
| Column | Key | Constraints / References |
| --------------- | ------ | ------------------------ |
| user_id | PK, FK | User.user_id |
| payment_method | | |
| billing_address | | |

**Invoice**
| Column | Key | Constraints / References |
| -------------- | --- | ------------------------ |
| invoice_id | PK | |
| user_id | FK | User.user_id |
| tier_id | FK | Tier.tier_id |
| invoice_date | | |
| amount | | |
| payment_status | | |

**Workspace**
| Column | Key | Constraints / References |
| ------------ | --- | ------------------------ |
| workspace_id | PK | |
| owner_id | FK | User.user_id |
| name | | |
| visibility | | |
| created_at | | |

**WorkspaceMember**
| Column | Key | Constraints / References |
| ------------ | ------ | ------------------------ |
| workspace_id | PK, FK | Workspace.workspace_id |
| user_id | PK, FK | User.user_id |
| role | | |
| joined_at | | |

**Persona**
| Column | Key | Constraints / References |
| ------------- | --- | ------------------------ |
| persona_id | PK | |
| owner_user_id | FK | User.user_id |
| name | | |
| instructions | | |
| created_at | | |

**Conversation**
| Column | Key | Constraints / References |
| ---------------------- | --- | --------------------------------- |
| conversation_id | PK | |
| user_id | FK | User.user_id |
| workspace_id | FK | Workspace.workspace_id (nullable) |
| persona_id | FK | Persona.persona_id (nullable) |
| title | | |
| start_time | | |
| status | | |
| persona_name_snapshot | | |
| persona_instr_snapshot | | |

**Message**
| Column | Key | Constraints / References |
| --------------- | --- | ---------------------------- |
| message_id | PK | |
| conversation_id | FK | Conversation.conversation_id |
| sender_role | | |
| time_sent | | |
| content | | |

**MessageFeedback**
| Column | Key | Constraints / References |
| ------------ | --- | -------------------------- |
| feedback_id | PK | |
| message_id | FK | UNIQUE, Message.message_id |
| rating | | |
| comment_text | | |
| submitted_at | | |

**MessageBookmark**
| Column | Key | Constraints / References |
| --------------- | ------ | ------------------------ |
| user_id | PK, FK | User.user_id |
| message_id | PK, FK | Message.message_id |
| bookmarked_time | | |

**PromptTemplate**
| Column | Key | Constraints / References |
| ------------- | --- | ------------------------ |
| template_id | PK | |
| owner_user_id | FK | User.user_id |
| name | | |
| text | | |
| category | | |
| visibility | | |
| created_at | | |

**WorkspacePromptTemplate**
| Column | Key | Constraints / References |
| ------------ | ------ | -------------------------- |
| workspace_id | PK, FK | Workspace.workspace_id |
| template_id | PK, FK | PromptTemplate.template_id |
| shared_at | | |

**SupportAgent**
| Column | Key | Constraints / Notes |
| ---------- | --- | ------------------- |
| agent_id | PK | |
| name | | |
| email | | UNIQUE |
| specialty | | |
| hired_date | | |

**SupportTicket**
| Column | Key | Constraints / References |
| ----------- | --- | -------------------------------- |
| ticket_id | PK | |
| user_id | FK | User.user_id |
| agent_id | FK | SupportAgent.agent_id (nullable) |
| topic_name | | |
| opened_time | | |
| closed_time | | nullable |
| status | | |

## 1.3 Relationships and Cardinalities

| Relationship       | Entities                     | Cardinality                   | Notes                                                                                                                                  |
| ------------------ | ---------------------------- | ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| subscribes_to      | User → Tier                  | N:1, total                    | Every user has exactly one current tier.                                                                                               |
| billed_at_tier     | Invoice → Tier               | N:1, total                    | Captures which tier was billed at invoice time — decouples historical invoices from the user's _current_ tier after upgrade/downgrade. |
| has                | User ↔ BillingRecord         | 1:1, total                    | Every user has exactly one billing record.                                                                                             |
| receives           | User → Invoice               | 1:N                           |                                                                                                                                        |
| owns_workspace     | User → Workspace             | 1:N                           | Foreign key `owner_id`.                                                                                                                |
| joins              | User ↔ Workspace             | M:N (WorkspaceMember)         | Attributes: `role`, `joined_at`.                                                                                                       |
| groups             | Workspace → Conversation     | 1:N, optional                 | A conversation may exist outside any workspace.                                                                                        |
| owns (persona)     | User → Persona               | 1:N                           |                                                                                                                                        |
| attached_to        | Persona → Conversation       | 1:N, optional                 | `ON DELETE SET NULL`; snapshot fields preserve history.                                                                                |
| starts             | User → Conversation          | 1:N                           |                                                                                                                                        |
| contains           | Conversation → Message       | 1:N, total on Message         |                                                                                                                                        |
| rated_by           | Message → MessageFeedback    | 1:0..1                        | Only AI-role messages may have feedback (enforced by trigger).                                                                         |
| creates (bookmark) | User ↔ Message               | M:N (MessageBookmark)         | Attribute: `bookmarked_time`.                                                                                                          |
| authors            | User → PromptTemplate        | 1:N                           |                                                                                                                                        |
| shared_via         | PromptTemplate ↔ Workspace   | M:N (WorkspacePromptTemplate) | Templates with `visibility = 'SHARED'` may be exposed in one or more workspaces.                                                       |
| opens              | User → SupportTicket         | 1:N                           |                                                                                                                                        |
| handles            | SupportAgent → SupportTicket | 1:N, optional                 | `agent_id` nullable until assignment.                                                                                                  |

## 1.4 Design Rationale

**Why `Tier` as its own entity rather than an enum on User.** Tiers carry their own attributes (`message_limit`, `pro_access`, `cost`) that change independently of any specific user. Modeling it separately avoids update anomalies — when the Plus tier's daily limit changes, one row updates instead of every Plus user's record.

**Why `Invoice` carries a `tier_id` of its own.** This is one of the schema's most important historical-integrity choices. An invoice represents a bill for a specific tier at a specific point in time. If you later change a user's `tier_id` (upgrade/downgrade, functionality #6), their old invoices must _not_ retroactively appear as if they were billed at the new tier. Giving Invoice its own `tier_id` FK decouples billing history from the user's current tier. This also means the "Generate a new invoice for a user's monthly tier fee" operation reads the user's current tier once, snapshots it onto the invoice, and subsequent tier changes leave the invoice untouched.

**Why `BillingRecord.user_id` is the primary key.** The assignment mandates _"each user must have a billing record"_ — that's a strict 1:1 relationship with total participation. Using `user_id` as the PK (rather than adding a surrogate `billing_id` with `user_id` as a FK and a separate UNIQUE constraint) enforces the 1:1 at the key level and saves a column. If the schema ever needs to support multiple payment methods per user, this can be relaxed to 1:N with a surrogate key.

**Why `Persona` uses snapshots on Conversation (controlled denormalization).** The spec states: _"if a Persona is updated or deleted, the historical context of existing conversations remains coherent."_ Three options were considered:

1. **Versioned Persona table** — cleanest theoretically (`Persona_Version` with version numbers, conversations reference a version), but doubles query complexity and adds a table.
2. **Block updates and deletions entirely** — violates the explicit deletion use case (Functionality #4).
3. **Snapshot on attach** (chosen) — when a conversation is created with a persona, `persona_name_snapshot` and `persona_instr_snapshot` are copied into the conversation row. The FK `persona_id` is retained for analytics (Query #3) with `ON DELETE SET NULL`.

The snapshot fields are **not** functionally determined by `persona_id` — they record the persona's state _at attach time_, which may differ from the current persona state after an update. This is legitimate point-in-time data (like `price_at_purchase` in an Order table), not redundant storage, so 3NF and even BCNF are preserved (see §3).

**Why `Conversation.status` is useful.** The spec defines the persona deletion rule in terms of _"ongoing conversations"_ (Functionality #4). Without a way to mark a conversation as archived/finished, every conversation is treated as ongoing forever, and personas become progressively harder to delete as the platform accumulates history. `status ∈ {'ACTIVE', 'ARCHIVED'}` gives the persona-deletion check (`count of conversations where persona_id = X AND status = 'ACTIVE'`) a meaningful interpretation, and supports a natural UX affordance (users archive old conversations).

**Why `MessageFeedback` uses a surrogate `feedback_id` with `UNIQUE(message_id)`.** Using `message_id` directly as the PK would force a strict 1:1 forever. The surrogate PK leaves room for future features (feedback revision history, multiple reviewers) without a schema migration, while `UNIQUE(message_id)` enforces the current 1:0..1 rule. There are two candidate keys: `{feedback_id}` and `{message_id}`. Both are superkeys, so all FDs trivially satisfy BCNF.

**Why `MessageFeedback` is a separate relation, not columns on Message.** Feedback is optional and only applies to AI messages. Nullable `rating` and `comment_text` columns on every Message waste space and muddle semantics. As a separate 1:0..1 table, the presence of a row encodes "feedback was given," which matches the domain cleanly and supports Query #3 elegantly.

**Why `PromptTemplate` uses an associative `WorkspacePromptTemplate`.** The spec says _"shared within a Workspace,"_ but the more useful real-world pattern is that a high-quality template authored by one user can be exposed across multiple workspaces they belong to. An M:N junction gives that flexibility without costing anything. The `visibility` column on `PromptTemplate` ('PRIVATE' or 'SHARED') is retained as a fast-path filter and is kept consistent with the junction via a trigger: a PRIVATE template has zero junction rows, a SHARED template has one or more. An alternative — a single nullable `workspace_id` FK on PromptTemplate — is simpler but locks a template to one workspace.

**Why `WorkspaceMember` is a first-class associative entity.** The M:N between User and Workspace carries its own attributes (`role` — typically 'OWNER', 'MEMBER' — and `joined_at`), which requires a separate relation. This also enables the required check (Functionality #3: _verify user belongs to a workspace before moving a conversation_) as a direct lookup.

**Why `sender_role`, `rating`, `status`, `topic_name`, `visibility`, and `pro_access` use CHECK constraints rather than lookup tables.** The value sets are hardcoded platform semantics (e.g. 'USER'/'AI' for role, 'Thumbs Up'/'Thumbs Down' for rating), not user-extensible. A lookup table would add a join with no benefit. The lone exception is `Tier`, which carries genuine structured attributes (cost, limits) and therefore earns its own table.

**Why `SupportTicket` has neither `resolution_duration` nor a separate `outcome` column.**

- Storing `resolution_duration` alongside `opened_time` and `closed_time` would create the FD `{opened_time, closed_time} → resolution_duration` with a non-superkey LHS. This FD fails both 3NF conditions (its LHS is not a superkey _and_ `resolution_duration` is not a prime attribute — it is not part of any candidate key), making it a genuine 3NF violation. Computing duration at query time (e.g. `(closed_time - opened_time) * 24` for hours in Oracle) is the clean fix.
- The `outcome ∈ {'Resolved', 'Escalated'}` attribute the spec mentions is collapsed into `status` by including both values as terminal states: `status ∈ {'Open', 'In Progress', 'Resolved', 'Escalated'}`. A ticket with a terminal status _is_ closed, and its outcome _is_ its status. This avoids the redundancy of storing the same information in two places.

**Why the user-deletion cascade goes through Conversation → Message → Feedback/Bookmark.** The spec allows either deletion or anonymization of message history on user removal; deletion is simpler and more consistent with the "unpaid invoices / open tickets block deletion" rule (which prevents deletion in the only cases where data retention would matter for business reasons). `ON DELETE CASCADE` chains make the operation atomic.

## 1.6 Additional Constraints (Not Expressible in the ERD)

These business rules are enforced in application (JDBC) logic and/or Oracle triggers rather than as simple cardinality constraints:

1. **User deletion** must fail if the user has any invoice with `payment_status = 'Unpaid'` _or_ any ticket with `status IN ('Open', 'In Progress')`. Implemented as a pre-delete check in the JDBC client (and optionally mirrored by a `BEFORE DELETE` trigger for defense-in-depth).
2. **Persona deletion** must fail if more than five conversations reference it with `status = 'ACTIVE'`. Implemented in JDBC via a `COUNT(*)` guard.
3. **Rate-limit check on message insert**: before inserting a new USER-role message, the application counts today's USER-role messages for that user and compares against `Tier.message_limit`.
4. **Workspace membership check** before moving a conversation into a workspace: verify a row exists in `WorkspaceMember` for `(target_workspace_id, conversation_owner_id)`.
5. **Feedback role restriction**: `MessageFeedback` rows may only exist for messages where `Message.sender_role = 'AI'`. Enforced by a `BEFORE INSERT/UPDATE` trigger on `MessageFeedback`.
6. **PromptTemplate visibility consistency**: `visibility = 'PRIVATE'` ⟺ no matching rows in `WorkspacePromptTemplate`; `visibility = 'SHARED'` ⟺ at least one matching row. Enforced by triggers on both tables.
7. **SupportTicket status/closed_time consistency**: `status IN ('Resolved', 'Escalated')` ⟺ `closed_time IS NOT NULL`. Enforced as a table-level CHECK.
8. **Cascade behavior**: `ON DELETE CASCADE` from User → Conversation → Message → MessageFeedback / MessageBookmark; `ON DELETE SET NULL` from Workspace → Conversation.workspace_id and Persona → Conversation.persona_id; `ON DELETE RESTRICT` from Tier → User and Tier → Invoice (tiers shouldn't be deletable while referenced).

---

# 2. Logical Database Design

## 2.1 ER-to-Relational Conversion Applied

- Every regular entity becomes one table whose PK is the ER identifier.
- The 1:1 relationship User ↔ BillingRecord is merged into BillingRecord with `user_id` as the PK+FK (no separate junction).
- 1:N relationships become FKs on the N-side (`user_id` on Conversation, Invoice, Persona, PromptTemplate, SupportTicket; `workspace_id` on Conversation; `persona_id` on Conversation; `agent_id` on SupportTicket; `tier_id` on User and Invoice).
- M:N relationships become associative tables with composite PKs: `WorkspaceMember`, `MessageBookmark`, `WorkspacePromptTemplate`.
- Optional participation becomes a nullable FK (Conversation.workspace_id, Conversation.persona_id, SupportTicket.agent_id, SupportTicket.closed_time).
- The weak 1:0..1 Message ↔ MessageFeedback is realized as a separate relation keyed by a surrogate `feedback_id` with `UNIQUE(message_id)` enforcing the 1:0..1.
- Derived attributes (resolution duration) are _not_ stored; they are computed in queries.

## 2.2 Relational Schema

Primary keys in **bold**. Foreign keys in _italic_. Required (`NOT NULL`) attributes marked †.

### Tier

**tier_id**†, tier_name† (UNIQUE), cost†, message_limit†, pro_access† (CHAR(1), CHECK IN ('Y','N'))

### User

**user_id**†, _tier_id_† → Tier (ON DELETE RESTRICT), name†, email† (UNIQUE), creation_date†, language†

### BillingRecord

**_user_id_**† (PK, FK → User, ON DELETE CASCADE), payment_method†, billing_address†

### Invoice

**invoice_id**†, _user_id_† → User (ON DELETE CASCADE), _tier_id_† → Tier (ON DELETE RESTRICT), invoice_date†, amount†, payment_status† (CHECK IN ('Paid','Unpaid','Overdue'))

### Workspace

**workspace_id**†, _owner_id_† → User (ON DELETE CASCADE), name†, visibility† (CHECK IN ('PRIVATE','SHARED')), created_at†

### WorkspaceMember

**_workspace_id_**† → Workspace (ON DELETE CASCADE), **_user_id_**† → User (ON DELETE CASCADE), role† (CHECK IN ('OWNER','MEMBER')), joined_at†
— **PK = (workspace_id, user_id)**

### Persona

**persona_id**†, _owner_user_id_† → User (ON DELETE CASCADE), name†, instructions†, created_at†
— UNIQUE (owner_user_id, name)

### Conversation

**conversation_id**†, _user_id_† → User (ON DELETE CASCADE), _workspace_id_ → Workspace (ON DELETE SET NULL), _persona_id_ → Persona (ON DELETE SET NULL), title†, start_time†, status† (CHECK IN ('ACTIVE','ARCHIVED')), persona_name_snapshot, persona_instr_snapshot

### Message

**message_id**†, _conversation_id_† → Conversation (ON DELETE CASCADE), sender_role† (CHECK IN ('USER','AI')), time_sent†, content†

### MessageFeedback

**feedback_id**†, _message_id_† → Message (ON DELETE CASCADE, UNIQUE), rating† (CHECK IN ('Thumbs Up','Thumbs Down')), comment_text, submitted_at†
— Trigger: `Message.sender_role` must equal 'AI'.

### MessageBookmark

**_user_id_**† → User (ON DELETE CASCADE), **_message_id_**† → Message (ON DELETE CASCADE), bookmarked_time†
— **PK = (user_id, message_id)**

### PromptTemplate

**template_id**†, _owner_user_id_† → User (ON DELETE CASCADE), name†, text†, category†, visibility† (CHECK IN ('PRIVATE','SHARED')), created_at†

### WorkspacePromptTemplate

**_workspace_id_**† → Workspace (ON DELETE CASCADE), **_template_id_**† → PromptTemplate (ON DELETE CASCADE), shared_at†
— **PK = (workspace_id, template_id)**
— Trigger: `PromptTemplate.visibility` must equal 'SHARED'.

### SupportAgent

**agent_id**†, name†, email† (UNIQUE), specialty†, hired_date†

### SupportTicket

**ticket_id**†, _user_id_† → User (ON DELETE RESTRICT), _agent_id_ → SupportAgent (ON DELETE SET NULL), topic_name† (CHECK IN ('Billing','Model Error','Account','Feature Request','Other')), opened_time†, closed_time, status† (CHECK IN ('Open','In Progress','Resolved','Escalated'))
— CHECK: `(status IN ('Resolved','Escalated')) = (closed_time IS NOT NULL)`

---

# 3. Normalization Analysis

## 3.1 General Argument

Every relation below is in **Boyce–Codd Normal Form**, which strictly implies 3NF (our required target). The universal test applied: for every non-trivial FD _X → A_ in each relation, _X_ is a superkey — the stricter BCNF condition. Where a relation has a natural-key alternative candidate key (e.g. `email` on User), both candidate keys determine all other attributes, so both LHSes are superkeys.

For each relation, the table below lists all non-trivial FDs, candidate keys, and a short justification.

## 3.2 Per-Table FDs and Justification

### Tier

**FDs**

- tier_id → tier_name, cost, message_limit, pro_access
- tier_name → tier_id, cost, message_limit, pro_access

**Candidate keys**: {tier_id}, {tier_name}. Both LHSes are superkeys. The descriptive attributes (cost, limits, flag) are independent facts of the tier — cost does not determine message_limit, nor vice versa. **3NF ✓ (BCNF ✓)**

### User

**FDs**

- user_id → tier_id, name, email, creation_date, language
- email → user_id, tier_id, name, creation_date, language

**Candidate keys**: {user_id}, {email}. `tier_id` does not determine any other User attribute — two users can share a tier yet differ in every other column. **3NF ✓ (BCNF ✓)**

### BillingRecord

**FDs**

- user_id → payment_method, billing_address

**Candidate key**: {user_id}. Single FD with PK as LHS. **3NF ✓ (BCNF ✓)**

### Invoice

**FDs**

- invoice_id → user_id, tier_id, invoice_date, amount, payment_status

**Candidate key**: {invoice_id}. Critically, `tier_id` does NOT determine `amount` in this relation: the amount is historical (a specific bill that may be prorated, discounted, or reflect an old tier fee), while `tier_id` is a reference. Likewise, `invoice_date` does not determine `payment_status`. No non-key FDs exist. **3NF ✓ (BCNF ✓)**

### Workspace

**FDs**

- workspace_id → owner_id, name, visibility, created_at

**Candidate key**: {workspace_id}. `name` is intentionally not unique (different owners may have workspaces of the same name). **3NF ✓ (BCNF ✓)**

### WorkspaceMember

**FDs**

- {workspace_id, user_id} → role, joined_at

**Candidate key**: {workspace_id, user_id}. The only non-trivial FD has the composite PK as LHS. **3NF ✓ (BCNF ✓)**

### Persona

**FDs**

- persona_id → owner_user_id, name, instructions, created_at
- {owner_user_id, name} → persona_id, instructions, created_at

**Candidate keys**: {persona_id}, {owner_user_id, name} (enforced by UNIQUE). Both are superkeys. **3NF ✓ (BCNF ✓)**

### Conversation

**FDs**

- conversation_id → user_id, workspace_id, persona_id, title, start_time, status, persona_name_snapshot, persona_instr_snapshot

**Candidate key**: {conversation_id}.

_Critical subtlety — why `persona_id → persona_name_snapshot` is NOT a valid FD in this relation:_ the snapshot columns record the persona's state _at the moment of attachment_. Because personas can be updated after attachment, two conversations with the same `persona_id` attached at different times may legitimately carry different snapshot values. An FD must hold across all valid database states, and this one does not. The snapshot fields are thus genuine point-in-time attributes of the conversation (analogous to `price_at_purchase` in an Order table), not redundant storage. **3NF ✓ (BCNF ✓)**

### Message

**FDs**

- message_id → conversation_id, sender_role, time_sent, content

**Candidate key**: {message_id}. **3NF ✓ (BCNF ✓)**

### MessageFeedback

**FDs**

- feedback_id → message_id, rating, comment_text, submitted_at
- message_id → feedback_id, rating, comment_text, submitted_at

**Candidate keys**: {feedback_id}, {message_id} (the UNIQUE constraint on `message_id` makes it a second candidate key). Both LHSes are superkeys. **3NF ✓ (BCNF ✓)**

### MessageBookmark

**FDs**

- {user_id, message_id} → bookmarked_time

**Candidate key**: {user_id, message_id}. **3NF ✓ (BCNF ✓)**

### PromptTemplate

**FDs**

- template_id → owner_user_id, name, text, category, visibility, created_at

**Candidate key**: {template_id}. `category` is a free-form user label, not a determinant of anything else. `visibility` is a simple flag. **3NF ✓ (BCNF ✓)**

### WorkspacePromptTemplate

**FDs**

- {workspace_id, template_id} → shared_at

**Candidate key**: {workspace_id, template_id}. **3NF ✓ (BCNF ✓)**

### SupportAgent

**FDs**

- agent_id → name, email, specialty, hired_date
- email → agent_id, name, specialty, hired_date

**Candidate keys**: {agent_id}, {email}. **3NF ✓ (BCNF ✓)**

### SupportTicket

**FDs**

- ticket_id → user_id, agent_id, topic_name, opened_time, closed_time, status

**Candidate key**: {ticket_id}. No redundant derived attributes (duration is computed at query time rather than stored, explicitly to avoid the {opened_time, closed_time} → duration FD that would violate 3NF — neither is the LHS a superkey nor the RHS a prime attribute). `status` does not determine `closed_time` as an FD: two 'Resolved' tickets have different closing timestamps. The CHECK constraint relating `status` and `closed_time IS NULL` is a tuple-level constraint, not an FD. **3NF ✓ (BCNF ✓)**

## 3.3 Summary

| Relation                | # Candidate Keys | 3NF | BCNF |
| ----------------------- | ---------------- | --- | ---- |
| Tier                    | 2                | ✓   | ✓    |
| User                    | 2                | ✓   | ✓    |
| BillingRecord           | 1                | ✓   | ✓    |
| Invoice                 | 1                | ✓   | ✓    |
| Workspace               | 1                | ✓   | ✓    |
| WorkspaceMember         | 1                | ✓   | ✓    |
| Persona                 | 2                | ✓   | ✓    |
| Conversation            | 1                | ✓   | ✓    |
| Message                 | 1                | ✓   | ✓    |
| MessageFeedback         | 2                | ✓   | ✓    |
| MessageBookmark         | 1                | ✓   | ✓    |
| PromptTemplate          | 1                | ✓   | ✓    |
| WorkspacePromptTemplate | 1                | ✓   | ✓    |
| SupportAgent            | 2                | ✓   | ✓    |
| SupportTicket           | 1                | ✓   | ✓    |

All fifteen relations satisfy 3NF. Every relation in fact satisfies the stricter BCNF standard — there is no relation in which BCNF required decomposition at the cost of dependency preservation, so the two notions coincide across the schema.

---

# 4. Self-Designed Query Description

## 4.1 The Question

> **Given a tier name supplied by the end user, identify the top three Personas that perform best for users of that tier — where "best" means the highest percentage of "Thumbs Up" feedback on AI responses in conversations started by users of that tier. Return the persona name, its owner's email, the number of distinct users of the specified tier who have used it, and its thumbs-up percentage.**

## 4.2 SQL Sketch

```sql
WITH tier_users AS (
  SELECT u.user_id
  FROM   "User" u
  JOIN   Tier t ON u.tier_id = t.tier_id
  WHERE  t.tier_name = ?          -- bound from user input
),
persona_ratings AS (
  SELECT p.persona_id,
         p.name AS persona_name,
         o.email AS owner_email,
         COUNT(DISTINCT c.user_id) AS tier_user_count,
         SUM(CASE WHEN f.rating = 'Thumbs Up' THEN 1 ELSE 0 END) AS up_count,
         COUNT(f.feedback_id) AS total_rated
  FROM   Persona p
  JOIN   "User" o         ON p.owner_user_id = o.user_id
  JOIN   Conversation c   ON c.persona_id = p.persona_id
  JOIN   tier_users tu    ON tu.user_id = c.user_id
  JOIN   Message m        ON m.conversation_id = c.conversation_id
                         AND m.sender_role = 'AI'
  JOIN   MessageFeedback f ON f.message_id = m.message_id
  GROUP  BY p.persona_id, p.name, o.email
  HAVING COUNT(f.feedback_id) > 0
)
SELECT persona_name,
       owner_email,
       tier_user_count,
       ROUND(100 * up_count / total_rated, 2) AS thumbs_up_pct
FROM   persona_ratings
ORDER  BY thumbs_up_pct DESC, tier_user_count DESC
FETCH FIRST 3 ROWS ONLY;
```

## 4.3 Why It Satisfies the Requirements

- **More than two relations**: six are joined — `Tier`, `User` (twice — once filtering tier members, once identifying persona owners), `Persona`, `Conversation`, `Message`, `MessageFeedback`.
- **User input**: the tier name is a bind parameter driving the filter.

## 4.4 Utility

This query directly addresses the _"data-driven insights"_ stakeholder requirement from the assignment spec. It fuses three signals — **who** is using the product (tier segment), **how** they are personalizing it (persona), and **how well** it works for them (feedback rating) — and produces segment-specific recommendations. Three concrete uses:

1. **Product marketing**: surface the top persona for Plus users as a soft upsell when a Free-tier user starts a conversation ("Users on Plus love the _Senior Architect_ persona").
2. **Quality bar**: detect personas that score well for Enterprise but poorly for Free — usually a signal that the persona assumes domain expertise the Free segment doesn't have, and a candidate for tier-specific tuning.
3. **Growth / community**: spot community-authored personas (visible via `owner_email`) that outperform in-house defaults — candidates for a curated marketplace or featured gallery.

Unlike a global "most helpful persona" leaderboard (Query #3 in the spec), this segmented variant reveals that different user populations benefit from different personalization — a far more actionable insight for the product team than a single ranked list.
