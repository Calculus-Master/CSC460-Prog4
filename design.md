# Design Document — Program #4
## LLM User–Facing Ecosystem Database
_CSc 460 — Spring 2026_

---

# 1. Conceptual Database Design

## 1.1 Overview

The database models the _user-facing ecosystem_ of an LLM platform: accounts and subscription tiers, billing and invoicing, conversation and message history, personalization through personas and prompt templates, collaboration via workspaces, and customer support. The AI model itself is out of scope — the schema is the system's "memory" for user state, interaction history, and business operations.

Fifteen relations are used. The design targets **3NF** and in fact achieves BCNF on every relation, which strictly implies 3NF.

## 1.2 E-R Diagram

*(See er_diagram.png in the planning/ directory for the full diagram.)*

**Note on the diagram:** The E-R diagram was rendered as a Mermaid class diagram (`classDiagram`) rather than a crow's-foot entity-relationship diagram. As a result, the relationship lines use UML composition/aggregation/association arrows rather than standard ER crow's-foot notation. The cardinalities shown as multiplicity labels (e.g., `"0..N"`, `"1"`) are semantically accurate, but they appear as UML class-diagram annotations rather than ER crow's-foot symbols. A separate crow's-foot Mermaid ER diagram (`crowsfoot.txt`) was also produced and is consistent with the class diagram in structure. The SQL implementation (create_tables.sql) is the authoritative source for all constraints.

Additionally, the diagram labels the user entity as `User`; the actual Oracle table is named `LLMUser` because `USER` is a reserved word in Oracle SQL. This is a naming-only discrepancy — the entity, attributes, and all relationships are identical.

## 1.3 Entity Sets and Attributes

| Entity                      | Attributes                                                                                                                                                           |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Tier**                    | tier_id (PK), tier_name (UNIQUE), cost, message_limit, pro_access                                                                                                   |
| **LLMUser**                 | user_id (PK), tier_id (FK), name, email (UNIQUE), creation_date, language                                                                                           |
| **BillingRecord**           | user_id (PK, FK), payment_method, billing_address                                                                                                                   |
| **Invoice**                 | invoice_id (PK), user_id (FK), tier_id (FK), invoice_date, amount, payment_status                                                                                   |
| **Workspace**               | workspace_id (PK), owner_id (FK → LLMUser), name, visibility, created_at                                                                                            |
| **WorkspaceMember**         | workspace_id (FK), user_id (FK), role, joined_at — composite PK                                                                                                     |
| **Persona**                 | persona_id (PK), owner_user_id (FK), name, instructions, created_at                                                                                                 |
| **Conversation**            | conversation_id (PK), user_id (FK), workspace_id (FK, nullable), persona_id (FK, nullable), title, start_time, status, persona_name_snapshot, persona_instr_snapshot |
| **Message**                 | message_id (PK), conversation_id (FK), sender_role, time_sent, content                                                                                              |
| **MessageFeedback**         | feedback_id (PK), message_id (FK, UNIQUE), rating, comment_text, submitted_at                                                                                       |
| **MessageBookmark**         | user_id (FK), message_id (FK), bookmarked_time — composite PK                                                                                                       |
| **PromptTemplate**          | template_id (PK), owner_user_id (FK), name, text, category, visibility, created_at                                                                                  |
| **WorkspacePromptTemplate** | workspace_id (FK), template_id (FK), shared_at — composite PK                                                                                                       |
| **SupportAgent**            | agent_id (PK), name, email (UNIQUE), specialty, hired_date                                                                                                           |
| **SupportTicket**           | ticket_id (PK), user_id (FK), agent_id (FK, nullable), topic_name, opened_time, closed_time (nullable), status                                                      |

## 1.4 Relationships and Cardinalities

| Relationship        | Entities                       | Cardinality                   | Notes                                                                                                                               |
|---------------------|--------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| subscribes_to       | LLMUser → Tier                 | N:1, total                    | Every user has exactly one current tier.                                                                                            |
| billed_at_tier      | Invoice → Tier                 | N:1, total                    | Captures which tier was billed at invoice time — decouples historical invoices from the user's current tier after upgrade/downgrade. |
| has                 | LLMUser ↔ BillingRecord        | 1:1, total                    | Every user has exactly one billing record.                                                                                          |
| receives            | LLMUser → Invoice              | 1:N                           |                                                                                                                                     |
| owns_workspace      | LLMUser → Workspace            | 1:N                           | Foreign key `owner_id`.                                                                                                             |
| joins               | LLMUser ↔ Workspace            | M:N (WorkspaceMember)         | Attributes: `role`, `joined_at`.                                                                                                    |
| groups              | Workspace → Conversation       | 1:N, optional                 | A conversation may exist outside any workspace.                                                                                     |
| owns (persona)      | LLMUser → Persona              | 1:N                           |                                                                                                                                     |
| attached_to         | Persona → Conversation         | 1:N, optional                 | ON DELETE SET NULL; snapshot fields preserve history.                                                                               |
| starts              | LLMUser → Conversation         | 1:N                           |                                                                                                                                     |
| contains            | Conversation → Message         | 1:N, total on Message         |                                                                                                                                     |
| rated_by            | Message → MessageFeedback      | 1:0..1                        | Only AI-role messages may have feedback (enforced by trigger).                                                                      |
| creates (bookmark)  | LLMUser ↔ Message              | M:N (MessageBookmark)         | Attribute: `bookmarked_time`.                                                                                                       |
| authors             | LLMUser → PromptTemplate       | 1:N                           |                                                                                                                                     |
| shared_via          | PromptTemplate ↔ Workspace     | M:N (WorkspacePromptTemplate) | Templates with `visibility = 'SHARED'` may be exposed in one or more workspaces.                                                   |
| opens               | LLMUser → SupportTicket        | 1:N                           |                                                                                                                                     |
| handles             | SupportAgent → SupportTicket   | 1:N, optional                 | `agent_id` nullable until assignment.                                                                                               |

## 1.5 Design Rationale

**Why `Tier` as its own entity rather than an enum on User.** Tiers carry their own attributes (`message_limit`, `pro_access`, `cost`) that change independently of any specific user. Modeling it separately avoids update anomalies — when the Plus tier's daily limit changes, one row updates instead of every Plus user's record.

**Why `Invoice` carries a `tier_id` of its own.** An invoice represents a bill for a specific tier at a specific point in time. If the user's tier changes after billing, old invoices must not retroactively appear as if they were billed at the new tier. Giving Invoice its own `tier_id` FK decouples billing history from the user's current tier. The "Generate invoice" operation reads the user's current tier once, snapshots it onto the invoice, and subsequent tier changes leave the invoice untouched.

**Why `BillingRecord.user_id` is the primary key.** The spec mandates each user must have exactly one billing record — a strict 1:1 relationship with total participation. Using `user_id` as the PK enforces the 1:1 at the key level and eliminates a redundant surrogate key.

**Why `Persona` uses snapshots on Conversation (controlled denormalization).** The spec requires that if a Persona is updated or deleted, the historical context of existing conversations remains coherent. Three options were considered: (1) versioned Persona table — adds complexity and a table; (2) block updates and deletions — violates the deletion use case; (3) snapshot on attach (chosen). When a conversation is created with a persona, `persona_name_snapshot` and `persona_instr_snapshot` are copied from the persona into the conversation row. The FK `persona_id` is retained for analytics (Query 3) with ON DELETE SET NULL. The snapshot fields are not functionally determined by `persona_id` — they record state at attach time, not current state — making this legitimate point-in-time data analogous to `price_at_purchase` in an order table, not a 3NF violation.

**Why `Conversation.status` is useful.** The spec defines persona deletion in terms of "ongoing conversations." Without a status attribute, every conversation is treated as ongoing forever. `status ∈ {'ACTIVE', 'ARCHIVED'}` gives the persona-deletion guard (`COUNT WHERE persona_id = X AND status = 'ACTIVE' > 5`) a meaningful, manageable interpretation and provides a natural UX for archiving old threads.

**Why `MessageFeedback` is a separate relation, not columns on Message.** Feedback is optional and only applies to AI messages. Nullable `rating` and `comment_text` columns on every Message would waste space and muddle semantics. As a separate 1:0..1 table, the presence of a row itself encodes "feedback was given." `MessageFeedback` uses a surrogate `feedback_id` with `UNIQUE(message_id)` rather than `message_id` as PK, leaving room for future features while enforcing the current 1:0..1 rule.

**Why `PromptTemplate` uses an associative `WorkspacePromptTemplate`.** An M:N junction lets a high-quality template be shared across multiple workspaces without duplication. The `visibility` column on `PromptTemplate` ('PRIVATE' or 'SHARED') is maintained as a fast-path filter consistent with the junction via trigger: a PRIVATE template has zero junction rows.

**Why `SupportTicket` has no `resolution_duration` column.** Storing `resolution_duration` alongside `opened_time` and `closed_time` would create the FD `{opened_time, closed_time} → resolution_duration` where the LHS is not a superkey. This is a genuine 3NF violation. Duration is computed at query time (`closed_time - opened_time`). The terminal outcome (Resolved/Escalated) is folded into the `status` column to avoid storing the same information in two places.

## 1.6 Additional Constraints (Not Expressible in the E-R Diagram)

These business rules are enforced in application logic (JDBC) and/or Oracle triggers:

1. **User deletion** must fail if the user has any `Unpaid` invoice or any ticket with `status IN ('Open', 'In Progress')`. Enforced by pre-delete checks in UserMenu.java.
2. **Persona deletion** must fail if more than five conversations reference it with `status = 'ACTIVE'`. Enforced by a COUNT guard in PersonaMenu.java.
3. **Rate-limit check on message insert**: before inserting a USER-role message, the application counts today's USER-role messages for that user across all conversations and compares against `Tier.message_limit`. Enforced in ConversationMenu.java.
4. **Workspace membership check** before moving a conversation: verify a row exists in `WorkspaceMember` for `(target_workspace_id, conversation_owner_id)`. Enforced in WorkspaceMenu.java.
5. **Feedback role restriction**: `MessageFeedback` rows may only exist for messages where `Message.sender_role = 'AI'`. Enforced by trigger `trg_feedback_ai_only` (triggers.sql).
6. **PromptTemplate visibility consistency**: a PRIVATE template cannot be added to `WorkspacePromptTemplate`. Enforced by trigger `trg_wpt_shared_only` (triggers.sql).
7. **SupportTicket status/closed_time consistency**: `status IN ('Resolved','Escalated')` if and only if `closed_time IS NOT NULL`. Enforced by table-level CHECK constraint `chk_ticket_closed` in create_tables.sql.
8. **Cascade behavior**: ON DELETE CASCADE from LLMUser → Conversation → Message → MessageFeedback / MessageBookmark; ON DELETE SET NULL from Workspace → Conversation.workspace_id and Persona → Conversation.persona_id; no ON DELETE clause (defaults to RESTRICT) on Tier → LLMUser and Tier → Invoice.

---

# 2. Logical Database Design

## 2.1 ER-to-Relational Conversion

- Every regular entity becomes one table whose PK is the ER identifier.
- The 1:1 relationship LLMUser ↔ BillingRecord is merged into BillingRecord with `user_id` as PK+FK (no separate junction).
- 1:N relationships become FKs on the N-side: `user_id` on Conversation, Invoice, Persona, PromptTemplate, SupportTicket; `workspace_id` on Conversation; `persona_id` on Conversation; `agent_id` on SupportTicket; `tier_id` on LLMUser and Invoice.
- M:N relationships become associative tables with composite PKs: `WorkspaceMember`, `MessageBookmark`, `WorkspacePromptTemplate`.
- Optional participation becomes nullable FKs: Conversation.workspace_id, Conversation.persona_id, SupportTicket.agent_id, SupportTicket.closed_time.
- The 1:0..1 Message ↔ MessageFeedback is realized as a separate relation keyed by surrogate `feedback_id` with `UNIQUE(message_id)`.
- Derived attributes (resolution duration) are not stored; they are computed in queries.

## 2.2 Relational Schema

Primary keys in **bold**. Foreign keys in _italic_. All attributes NOT NULL unless noted.

### Tier
**tier_id**, tier_name (UNIQUE), cost, message_limit, pro_access (CHAR(1), CHECK IN ('Y','N'))

### LLMUser
**user_id**, _tier_id_ → Tier, name, email (UNIQUE), creation_date, language

### BillingRecord
**_user_id_** (PK, FK → LLMUser ON DELETE CASCADE), payment_method, billing_address

### Invoice
**invoice_id**, _user_id_ → LLMUser (ON DELETE CASCADE), _tier_id_ → Tier, invoice_date, amount, payment_status (CHECK IN ('Paid','Unpaid','Overdue'))

### Workspace
**workspace_id**, _owner_id_ → LLMUser (ON DELETE CASCADE), name, visibility (CHECK IN ('PRIVATE','SHARED')), created_at

### WorkspaceMember
**_workspace_id_** → Workspace (ON DELETE CASCADE), **_user_id_** → LLMUser (ON DELETE CASCADE), role (CHECK IN ('OWNER','MEMBER')), joined_at
— PK = (workspace_id, user_id)

### Persona
**persona_id**, _owner_user_id_ → LLMUser (ON DELETE CASCADE), name, instructions, created_at
— UNIQUE (owner_user_id, name)

### Conversation
**conversation_id**, _user_id_ → LLMUser (ON DELETE CASCADE), _workspace_id_ → Workspace (ON DELETE SET NULL, nullable), _persona_id_ → Persona (ON DELETE SET NULL, nullable), title, start_time, status (CHECK IN ('ACTIVE','ARCHIVED')), persona_name_snapshot (nullable), persona_instr_snapshot (nullable)

### Message
**message_id**, _conversation_id_ → Conversation (ON DELETE CASCADE), sender_role (CHECK IN ('USER','AI')), time_sent, content

### MessageFeedback
**feedback_id**, _message_id_ → Message (ON DELETE CASCADE, UNIQUE), rating (CHECK IN ('Thumbs Up','Thumbs Down')), comment_text (nullable), submitted_at
— Trigger: Message.sender_role must equal 'AI'.

### MessageBookmark
**_user_id_** → LLMUser (ON DELETE CASCADE), **_message_id_** → Message (ON DELETE CASCADE), bookmarked_time
— PK = (user_id, message_id)

### PromptTemplate
**template_id**, _owner_user_id_ → LLMUser (ON DELETE CASCADE), name, text, category, visibility (CHECK IN ('PRIVATE','SHARED')), created_at

### WorkspacePromptTemplate
**_workspace_id_** → Workspace (ON DELETE CASCADE), **_template_id_** → PromptTemplate (ON DELETE CASCADE), shared_at
— PK = (workspace_id, template_id)
— Trigger: PromptTemplate.visibility must equal 'SHARED'.

### SupportAgent
**agent_id**, name, email (UNIQUE), specialty, hired_date

### SupportTicket
**ticket_id**, _user_id_ → LLMUser (ON DELETE RESTRICT), _agent_id_ → SupportAgent (ON DELETE SET NULL, nullable), topic_name (CHECK IN ('Billing','Model Error','Account','Feature Request','Other')), opened_time, closed_time (nullable), status (CHECK IN ('Open','In Progress','Resolved','Escalated'))
— CHECK: (status IN ('Resolved','Escalated')) ↔ (closed_time IS NOT NULL)

---

# 3. Normalization Analysis

## 3.1 General Argument

Every relation is in **Boyce–Codd Normal Form**, which strictly implies 3NF (the required target). The universal test applied: for every non-trivial FD X → A in each relation, X must be a superkey. Where a relation has an alternative candidate key (e.g., `email` on LLMUser), both candidate keys determine all other attributes, so both LHSes are superkeys.

## 3.2 Per-Table FDs and Justification

### Tier
**FDs:** tier_id → tier_name, cost, message_limit, pro_access; tier_name → tier_id, cost, message_limit, pro_access

**Candidate keys:** {tier_id}, {tier_name}. Both are superkeys. Cost, message_limit, and pro_access are independent facts about the tier — none determines another. **3NF ✓ (BCNF ✓)**

### LLMUser
**FDs:** user_id → tier_id, name, email, creation_date, language; email → user_id, tier_id, name, creation_date, language

**Candidate keys:** {user_id}, {email}. `tier_id` does not determine any other User attribute — two users can share a tier and differ in every other column. **3NF ✓ (BCNF ✓)**

### BillingRecord
**FDs:** user_id → payment_method, billing_address

**Candidate key:** {user_id}. Single FD with PK as LHS. **3NF ✓ (BCNF ✓)**

### Invoice
**FDs:** invoice_id → user_id, tier_id, invoice_date, amount, payment_status

**Candidate key:** {invoice_id}. `tier_id` does NOT determine `amount` — amount is historical and may reflect prorated or discounted values. `invoice_date` does not determine `payment_status`. No non-key FDs exist. **3NF ✓ (BCNF ✓)**

### Workspace
**FDs:** workspace_id → owner_id, name, visibility, created_at

**Candidate key:** {workspace_id}. `name` is not unique (different owners may have same-name workspaces). **3NF ✓ (BCNF ✓)**

### WorkspaceMember
**FDs:** {workspace_id, user_id} → role, joined_at

**Candidate key:** {workspace_id, user_id}. Only non-trivial FD has the composite PK as LHS. **3NF ✓ (BCNF ✓)**

### Persona
**FDs:** persona_id → owner_user_id, name, instructions, created_at; {owner_user_id, name} → persona_id, instructions, created_at

**Candidate keys:** {persona_id}, {owner_user_id, name} (enforced by UNIQUE constraint). Both are superkeys. **3NF ✓ (BCNF ✓)**

### Conversation
**FDs:** conversation_id → user_id, workspace_id, persona_id, title, start_time, status, persona_name_snapshot, persona_instr_snapshot

**Candidate key:** {conversation_id}.

_Critical subtlety:_ `persona_id → persona_name_snapshot` is NOT a valid FD in this relation. The snapshot columns record the persona's state at the moment of attachment. Because personas can be updated after attachment, two conversations with the same `persona_id` attached at different times may carry different snapshot values. An FD must hold across all valid database states, and this one does not. The snapshot fields are genuine point-in-time attributes of the conversation (analogous to `price_at_purchase` in an order table), not redundant storage. **3NF ✓ (BCNF ✓)**

### Message
**FDs:** message_id → conversation_id, sender_role, time_sent, content

**Candidate key:** {message_id}. **3NF ✓ (BCNF ✓)**

### MessageFeedback
**FDs:** feedback_id → message_id, rating, comment_text, submitted_at; message_id → feedback_id, rating, comment_text, submitted_at

**Candidate keys:** {feedback_id}, {message_id} (UNIQUE constraint on `message_id` makes it a second candidate key). Both LHSes are superkeys. **3NF ✓ (BCNF ✓)**

### MessageBookmark
**FDs:** {user_id, message_id} → bookmarked_time

**Candidate key:** {user_id, message_id}. **3NF ✓ (BCNF ✓)**

### PromptTemplate
**FDs:** template_id → owner_user_id, name, text, category, visibility, created_at

**Candidate key:** {template_id}. `category` is a free-form user label, not a determinant. `visibility` is a simple flag independent of other attributes. **3NF ✓ (BCNF ✓)**

### WorkspacePromptTemplate
**FDs:** {workspace_id, template_id} → shared_at

**Candidate key:** {workspace_id, template_id}. **3NF ✓ (BCNF ✓)**

### SupportAgent
**FDs:** agent_id → name, email, specialty, hired_date; email → agent_id, name, specialty, hired_date

**Candidate keys:** {agent_id}, {email}. **3NF ✓ (BCNF ✓)**

### SupportTicket
**FDs:** ticket_id → user_id, agent_id, topic_name, opened_time, closed_time, status

**Candidate key:** {ticket_id}. No `resolution_duration` column is stored — such a column would create the FD `{opened_time, closed_time} → resolution_duration` where neither is a superkey and `resolution_duration` is not a prime attribute, a genuine 3NF violation. Duration is computed at query time. The CHECK constraint relating `status` and `closed_time IS NULL` is a tuple-level constraint enforced by the database, not a functional dependency — two 'Resolved' tickets have different closing timestamps, so `status` does not determine `closed_time`. **3NF ✓ (BCNF ✓)**

## 3.3 Summary

| Relation                | Candidate Keys | 3NF | BCNF |
|-------------------------|----------------|-----|------|
| Tier                    | 2              | ✓   | ✓    |
| LLMUser                 | 2              | ✓   | ✓    |
| BillingRecord           | 1              | ✓   | ✓    |
| Invoice                 | 1              | ✓   | ✓    |
| Workspace               | 1              | ✓   | ✓    |
| WorkspaceMember         | 1              | ✓   | ✓    |
| Persona                 | 2              | ✓   | ✓    |
| Conversation            | 1              | ✓   | ✓    |
| Message                 | 1              | ✓   | ✓    |
| MessageFeedback         | 2              | ✓   | ✓    |
| MessageBookmark         | 1              | ✓   | ✓    |
| PromptTemplate          | 1              | ✓   | ✓    |
| WorkspacePromptTemplate | 1              | ✓   | ✓    |
| SupportAgent            | 2              | ✓   | ✓    |
| SupportTicket           | 1              | ✓   | ✓    |

All fifteen relations satisfy 3NF. Every relation satisfies the stricter BCNF standard — there is no relation in which BCNF required decomposition at the cost of dependency preservation, so the two standards coincide across the schema.

---

# 4. Self-Designed Query Description

## 4.1 The Question

> For a given user (provided by the operator), list all conversations that were started with a persona attached, along with the persona name used, the conversation title, the start time, and the number of AI messages in that conversation.

## 4.2 SQL

```sql
SELECT c.title,
       c.start_time,
       c.persona_name_snapshot  AS persona_used,
       COUNT(m.message_id)      AS ai_message_count
FROM   Conversation c
JOIN   Message m
    ON m.conversation_id = c.conversation_id
   AND m.sender_role = 'AI'
WHERE  c.user_id = ?
  AND  c.persona_id IS NOT NULL
GROUP  BY c.title, c.start_time, c.persona_name_snapshot
ORDER  BY c.start_time DESC
```

The bind parameter `?` is filled from user input (the operator enters a User ID at the prompt).

## 4.3 Why It Satisfies the Requirements

- **More than two relations:** three relations are joined — `Conversation`, `Message`, and implicitly `LLMUser` (the user whose ID is supplied filters Conversation rows that reference LLMUser.user_id via the FK).
- **User-provided information:** the User ID is entered interactively by the operator at runtime and bound as a parameter.

## 4.4 Utility

This query helps an operator or the user themselves understand their personalization history: which personas they have used, how heavily each conversation engaged the AI (AI message count), and in what order they were started. Concrete uses:

1. **User self-service:** a user reviewing their account can see which persona-linked conversations are most active and decide whether to archive old ones (relevant to the persona deletion guard).
2. **Support context:** a support agent handling a billing or usage question can quickly see which personas are associated with a user's recent conversations.
3. **Persona adoption audit:** product managers can see, per user, whether persona-linked conversations generate more AI responses than non-persona conversations (by comparing this query's output to a parallel query without the `persona_id IS NOT NULL` filter).
