-- CSc 460 Program 4 — Sample Data
-- Enough rows to exercise all 4 required queries

-- Tiers
INSERT INTO Tier (tier_name, cost, message_limit, pro_access)
VALUES ('Free', 0, 100, 'N');
INSERT INTO Tier (tier_name, cost, message_limit, pro_access)
VALUES ('Plus', 20, 1000, 'Y');
INSERT INTO Tier (tier_name, cost, message_limit, pro_access)
VALUES ('Enterprise', 100, 99999, 'Y');

-- SupportAgents
INSERT INTO SupportAgent (name, email, specialty, hired_date)
VALUES ('Alice Tran', 'alice@support.llm', 'Billing', DATE '2023-01-15');
INSERT INTO SupportAgent (name, email, specialty, hired_date)
VALUES ('Bob Nguyen', 'bob@support.llm', 'Model Error', DATE '2023-06-01');

-- Users (tier_id: 1=Free,2=Plus,3=Enterprise)
INSERT INTO LLMUser (tier_id, name, email, creation_date, language)
VALUES (1, 'Carol Smith',   'carol@example.com',   DATE '2024-01-10', 'English');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language)
VALUES (1, 'Dave Jones',    'dave@example.com',    DATE '2024-02-20', 'English');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language)
VALUES (2, 'Eva Muller',    'eva@example.com',     DATE '2024-03-05', 'German');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language)
VALUES (2, 'Frank Lee',     'frank@example.com',   DATE '2024-04-12', 'English');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language)
VALUES (3, 'Grace Park',    'grace@example.com',   DATE '2024-05-01', 'Korean');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language)
VALUES (3, 'Hiro Tanaka',   'hiro@example.com',    DATE '2024-06-15', 'Japanese');

-- BillingRecords (one per user)
INSERT INTO BillingRecord (user_id, payment_method, billing_address)
VALUES (1, 'Credit Card', '100 Maple St, Tucson AZ');
INSERT INTO BillingRecord (user_id, payment_method, billing_address)
VALUES (2, 'PayPal',      '200 Oak Ave, Tucson AZ');
INSERT INTO BillingRecord (user_id, payment_method, billing_address)
VALUES (3, 'Credit Card', '10 Elm Rd, Berlin');
INSERT INTO BillingRecord (user_id, payment_method, billing_address)
VALUES (4, 'Bank Transfer','300 Pine St, Seattle WA');
INSERT INTO BillingRecord (user_id, payment_method, billing_address)
VALUES (5, 'Credit Card', '50 Han River Dr, Seoul');
INSERT INTO BillingRecord (user_id, payment_method, billing_address)
VALUES (6, 'Credit Card', '1-1 Shibuya, Tokyo');

-- Invoices
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status)
VALUES (3, 2, DATE '2024-03-01', 20.00, 'Paid');
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status)
VALUES (3, 2, DATE '2024-04-01', 20.00, 'Unpaid');   -- Eva has unpaid
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status)
VALUES (4, 2, DATE '2024-04-01', 20.00, 'Paid');
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status)
VALUES (5, 3, DATE '2024-05-01', 100.00,'Unpaid');   -- Grace has unpaid
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status)
VALUES (6, 3, DATE '2024-06-01', 100.00,'Paid');

-- Workspaces
INSERT INTO Workspace (owner_id, name, visibility, created_at)
VALUES (3, 'Eva Research Hub', 'SHARED', TIMESTAMP '2024-03-10 09:00:00');
INSERT INTO Workspace (owner_id, name, visibility, created_at)
VALUES (5, 'Grace Enterprise WS', 'PRIVATE', TIMESTAMP '2024-05-05 10:00:00');

-- WorkspaceMembers
INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at)
VALUES (1, 3, 'OWNER', TIMESTAMP '2024-03-10 09:00:00');
INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at)
VALUES (1, 4, 'MEMBER', TIMESTAMP '2024-03-15 11:00:00');
INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at)
VALUES (2, 5, 'OWNER', TIMESTAMP '2024-05-05 10:00:00');
INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at)
VALUES (2, 6, 'MEMBER', TIMESTAMP '2024-05-06 08:00:00');

-- Personas
INSERT INTO Persona (owner_user_id, name, instructions, created_at)
VALUES (3, 'Senior Architect',
    'You are an experienced software architect. Speak concisely and cite trade-offs.',
    TIMESTAMP '2024-03-12 10:00:00');
INSERT INTO Persona (owner_user_id, name, instructions, created_at)
VALUES (5, 'Data Analyst',
    'You are a data analyst expert in SQL and Python. Provide code examples.',
    TIMESTAMP '2024-05-10 14:00:00');
INSERT INTO Persona (owner_user_id, name, instructions, created_at)
VALUES (6, 'Creative Writer',
    'You are a creative writing coach. Be imaginative and inspiring.',
    TIMESTAMP '2024-06-20 16:00:00');

-- Conversations (some with personas for query 3 & 4)
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status,
    persona_name_snapshot, persona_instr_snapshot)
VALUES (3, 1, 1, 'Microservices Discussion', TIMESTAMP '2024-04-01 10:00:00', 'ACTIVE',
    'Senior Architect', 'You are an experienced software architect. Speak concisely and cite trade-offs.');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status,
    persona_name_snapshot, persona_instr_snapshot)
VALUES (4, 1, 1, 'System Design Interview', TIMESTAMP '2024-04-05 11:00:00', 'ACTIVE',
    'Senior Architect', 'You are an experienced software architect. Speak concisely and cite trade-offs.');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status,
    persona_name_snapshot, persona_instr_snapshot)
VALUES (5, 2, 2, 'Sales Dashboard Query', TIMESTAMP '2024-05-15 09:00:00', 'ACTIVE',
    'Data Analyst', 'You are a data analyst expert in SQL and Python. Provide code examples.');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status,
    persona_name_snapshot, persona_instr_snapshot)
VALUES (6, 2, 2, 'Python Data Pipeline', TIMESTAMP '2024-06-01 08:00:00', 'ACTIVE',
    'Data Analyst', 'You are a data analyst expert in SQL and Python. Provide code examples.');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status,
    persona_name_snapshot, persona_instr_snapshot)
VALUES (1, NULL, 3, 'Short Story Help', TIMESTAMP '2024-06-25 14:00:00', 'ACTIVE',
    'Creative Writer', 'You are a creative writing coach. Be imaginative and inspiring.');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status,
    persona_name_snapshot, persona_instr_snapshot)
VALUES (2, NULL, NULL, 'General Chat', TIMESTAMP '2024-07-01 12:00:00', 'ARCHIVED',
    NULL, NULL);

-- Messages for conversation 1 (microservices, user 3 + AI persona 1)
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (1, 'USER', TIMESTAMP '2024-04-01 10:01:00', 'What are the pros and cons of microservices?');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (1, 'AI',   TIMESTAMP '2024-04-01 10:01:30',
    'Pros: independent deployability, fault isolation, tech diversity. Cons: network latency, distributed tracing complexity, operational overhead.');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (1, 'USER', TIMESTAMP '2024-04-01 10:03:00', 'How do you handle service discovery?');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (1, 'AI',   TIMESTAMP '2024-04-01 10:03:30',
    'Use a service registry (Consul, Eureka) or leverage Kubernetes DNS-based discovery.');

-- Messages for conversation 2 (system design, user 4 + AI persona 1)
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (2, 'USER', TIMESTAMP '2024-04-05 11:01:00', 'Design a URL shortener.');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (2, 'AI',   TIMESTAMP '2024-04-05 11:01:45',
    'Use a hash (MD5/base62) for short codes, store in Redis for fast lookups, persist to DB for durability.');

-- Messages for conversation 3 (sales dashboard, user 5 + AI persona 2)
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (3, 'USER', TIMESTAMP '2024-05-15 09:01:00', 'Write a SQL query for monthly revenue.');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (3, 'AI',   TIMESTAMP '2024-05-15 09:01:40',
    'SELECT TO_CHAR(sale_date,''YYYY-MM'') AS month, SUM(amount) FROM sales GROUP BY TO_CHAR(sale_date,''YYYY-MM'') ORDER BY 1;');

-- Messages for conversation 4 (pipeline, user 6 + AI persona 2)
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (4, 'USER', TIMESTAMP '2024-06-01 08:01:00', 'How to read CSVs in pandas?');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (4, 'AI',   TIMESTAMP '2024-06-01 08:01:30',
    'Use pd.read_csv(''file.csv'') — pass dtype={} to avoid type inference surprises.');

-- Messages for conversation 5 (creative, user 1 + AI persona 3)
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (5, 'USER', TIMESTAMP '2024-06-25 14:01:00', 'Give me an opening line for a sci-fi story.');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (5, 'AI',   TIMESTAMP '2024-06-25 14:01:25',
    'The last star blinked out on a Tuesday, and nobody noticed until Wednesday.');

-- Messages for conversation 6 (general, user 2, no persona)
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (6, 'USER', TIMESTAMP '2024-07-01 12:01:00', 'Hello');
INSERT INTO Message (conversation_id, sender_role, time_sent, content)
VALUES (6, 'AI',   TIMESTAMP '2024-07-01 12:01:10', 'Hello! How can I help you today?');

-- MessageFeedback (only on AI messages: msg 2,4,6,8,10,12,14)
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at)
VALUES (2, 'Thumbs Up', 'Very clear trade-off summary.', TIMESTAMP '2024-04-01 10:05:00');
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at)
VALUES (4, 'Thumbs Up', 'Good pointer on Kubernetes.', TIMESTAMP '2024-04-01 10:07:00');
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at)
VALUES (6, 'Thumbs Up', 'Concise and accurate.', TIMESTAMP '2024-04-05 11:05:00');
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at)
VALUES (8, 'Thumbs Up', 'Exactly what I needed.', TIMESTAMP '2024-05-15 09:05:00');
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at)
VALUES (10, 'Thumbs Down', 'Could have more context.', TIMESTAMP '2024-06-01 08:05:00');
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at)
VALUES (12, 'Thumbs Up', 'Love this opening!', TIMESTAMP '2024-06-25 14:03:00');

-- MessageBookmarks (user 1 bookmarks messages from conv 1 and conv 5)
INSERT INTO MessageBookmark (user_id, message_id, bookmarked_time)
VALUES (1, 2, TIMESTAMP '2024-04-02 08:00:00');
INSERT INTO MessageBookmark (user_id, message_id, bookmarked_time)
VALUES (1, 12, TIMESTAMP '2024-06-26 09:00:00');
-- user 3 bookmarks from conv 2
INSERT INTO MessageBookmark (user_id, message_id, bookmarked_time)
VALUES (3, 6, TIMESTAMP '2024-04-06 10:00:00');

-- PromptTemplates
INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at)
VALUES (3, 'Code Review Checklist',
    'Review this code for: correctness, performance, security, readability.',
    'Engineering', 'SHARED', TIMESTAMP '2024-04-10 09:00:00');
INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at)
VALUES (5, 'Private Note Template',
    'Summarize the following in 3 bullets:',
    'Personal', 'PRIVATE', TIMESTAMP '2024-05-20 11:00:00');
INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at)
VALUES (6, 'Meeting Summary',
    'Summarize this meeting transcript: action items, decisions, owners.',
    'Productivity', 'SHARED', TIMESTAMP '2024-06-22 14:00:00');

-- WorkspacePromptTemplate (only SHARED templates)
INSERT INTO WorkspacePromptTemplate (workspace_id, template_id, shared_at)
VALUES (1, 1, TIMESTAMP '2024-04-11 08:00:00');
INSERT INTO WorkspacePromptTemplate (workspace_id, template_id, shared_at)
VALUES (2, 3, TIMESTAMP '2024-06-23 09:00:00');

-- SupportTickets
INSERT INTO SupportTicket (user_id, agent_id, topic_name, opened_time, closed_time, status)
VALUES (1, NULL, 'Billing', TIMESTAMP '2024-07-10 10:00:00', NULL, 'Open');
INSERT INTO SupportTicket (user_id, agent_id, topic_name, opened_time, closed_time, status)
VALUES (2, 1, 'Account', TIMESTAMP '2024-07-05 09:00:00', NULL, 'In Progress');
INSERT INTO SupportTicket (user_id, agent_id, topic_name, opened_time, closed_time, status)
VALUES (3, 1, 'Model Error', TIMESTAMP '2024-06-20 08:00:00',
    TIMESTAMP '2024-06-21 10:00:00', 'Resolved');
INSERT INTO SupportTicket (user_id, agent_id, topic_name, opened_time, closed_time, status)
VALUES (4, 2, 'Feature Request', TIMESTAMP '2024-07-01 11:00:00',
    TIMESTAMP '2024-07-03 14:00:00', 'Escalated');

COMMIT;
