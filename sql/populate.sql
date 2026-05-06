-- Sample data

-- Tier
INSERT INTO Tier (tier_name, cost, message_limit, pro_access) VALUES ('Free', 0, 100, 'N');
INSERT INTO Tier (tier_name, cost, message_limit, pro_access) VALUES ('Plus', 20, 1000, 'Y');
INSERT INTO Tier (tier_name, cost, message_limit, pro_access) VALUES ('Enterprise', 100, 99999, 'Y');

-- SupportAgent
INSERT INTO SupportAgent (name, email, specialty, hired_date) VALUES ('Alice', 'alice@support.llm', 'Billing', DATE '2023-01-15');
INSERT INTO SupportAgent (name, email, specialty, hired_date) VALUES ('Bob', 'bob@support.llm', 'Account', DATE '2023-06-01');

-- LLMUser
INSERT INTO LLMUser (tier_id, name, email, creation_date, language) VALUES (1, 'Carol', 'carol@example.com', DATE '2024-01-10', 'English');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language) VALUES (2, 'Dave', 'dave@example.com', DATE '2024-02-20', 'English');
INSERT INTO LLMUser (tier_id, name, email, creation_date, language) VALUES (3, 'Eva', 'eva@example.com', DATE '2024-03-05', 'German');

-- BillingRecord
INSERT INTO BillingRecord (user_id, payment_method, billing_address) VALUES (1, 'Credit Card', 'UofA, Tucson AZ');
INSERT INTO BillingRecord (user_id, payment_method, billing_address) VALUES (2, 'PayPal', 'ASU, Tempe AZ');
INSERT INTO BillingRecord (user_id, payment_method, billing_address) VALUES (3, 'Credit Card', 'Parliament, UK');

-- Invoice
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status) VALUES (2, 2, DATE '2024-03-01', 20.00, 'Paid');
INSERT INTO Invoice (user_id, tier_id, invoice_date, amount, payment_status) VALUES (3, 3, DATE '2024-04-01', 100.00, 'Unpaid');

-- Workspace
INSERT INTO Workspace (owner_id, name, visibility, created_at) VALUES (3, 'google', 'SHARED', TIMESTAMP '2024-03-10 09:00:00');

-- WorkspaceMember
INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at) VALUES (1, 3, 'OWNER', TIMESTAMP '2024-03-10 09:00:00');
INSERT INTO WorkspaceMember (workspace_id, user_id, role, joined_at) VALUES (1, 2, 'MEMBER', TIMESTAMP '2024-03-15 11:00:00');

-- Persona
INSERT INTO Persona (owner_user_id, name, instructions, created_at) VALUES (3, 'SWE', 'you are software architect', TIMESTAMP '2024-03-12 10:00:00');
INSERT INTO Persona (owner_user_id, name, instructions, created_at) VALUES (3, 'writer', 'you are writing coach', TIMESTAMP '2024-03-20 14:00:00');

-- Conversation
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status, persona_name_snapshot, persona_instr_snapshot)
    VALUES (3, 1, 1, 'Microservices Discussion', TIMESTAMP '2024-04-01 10:00:00', 'ACTIVE', 'SWE', 'you are software architect');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status, persona_name_snapshot, persona_instr_snapshot)
    VALUES (1, NULL, 2, 'Short Story Help', TIMESTAMP '2024-06-25 14:00:00', 'ACTIVE', 'writer', 'you are writing coach');
INSERT INTO Conversation (user_id, workspace_id, persona_id, title, start_time, status, persona_name_snapshot, persona_instr_snapshot)
    VALUES (2, NULL, NULL, 'General Chat', TIMESTAMP '2024-07-01 12:00:00', 'ARCHIVED', NULL, NULL);

-- Message
INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (1, 'USER', TIMESTAMP '2024-04-01 10:01:00', 'pros and cons of iphone?');
INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (1, 'AI', TIMESTAMP '2024-04-01 10:01:30', 'pros: cool. cons: expensive');
INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (2, 'USER', TIMESTAMP '2024-06-25 14:01:00', 'opening line for a sci-fi.');
INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (2, 'AI', TIMESTAMP '2024-06-25 14:01:25', 'spaceballs');
INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (3, 'USER', TIMESTAMP '2024-07-01 12:01:00', 'hi');
INSERT INTO Message (conversation_id, sender_role, time_sent, content) VALUES (3, 'AI', TIMESTAMP '2024-07-01 12:01:10', 'hello');

-- MessageFeedback
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at) VALUES (2, 'Thumbs Up', 'not wrong', TIMESTAMP '2024-04-01 10:05:00');
INSERT INTO MessageFeedback (message_id, rating, comment_text, submitted_at) VALUES (4, 'Thumbs Down', 'nede more', TIMESTAMP '2024-06-25 14:05:00');

-- MessageBookmark
INSERT INTO MessageBookmark (user_id, message_id, bookmarked_time) VALUES (1, 2, TIMESTAMP '2024-04-02 08:00:00');

-- PromptTemplate
INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at) VALUES (3, 'code review', 'review code for correctness', 'engineering', 'SHARED',  TIMESTAMP '2024-04-10 09:00:00');
INSERT INTO PromptTemplate (owner_user_id, name, text, category, visibility, created_at) VALUES (2, 'summary', 'summarize this short:', 'personal', 'PRIVATE', TIMESTAMP '2024-05-20 11:00:00');

-- WorkspacePromptTemplate
INSERT INTO WorkspacePromptTemplate (workspace_id, template_id, shared_at) VALUES (1, 1, TIMESTAMP '2024-04-11 08:00:00');

-- SupportTicket
INSERT INTO SupportTicket (user_id, agent_id, topic_name, opened_time, closed_time, status) VALUES (1, NULL, 'Billing', TIMESTAMP '2024-07-10 10:00:00', NULL, 'Open');
INSERT INTO SupportTicket (user_id, agent_id, topic_name, opened_time, closed_time, status) VALUES (3, 1, 'Account', TIMESTAMP '2024-06-20 08:00:00', TIMESTAMP '2024-06-21 10:00:00', 'Resolved');

COMMIT;
