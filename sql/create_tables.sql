-- CSc 460 Program 4 — LLM Platform Database
-- create_tables.sql: DDL for all 15 tables in dependency order
-- Run on aloe.cs.arizona.edu via SQL*Plus

-- Drop in reverse dependency order (ignore errors if tables don't exist)
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS';
    END LOOP;
END;
/

-- 1. Tier
CREATE TABLE Tier (
    tier_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tier_name    VARCHAR2(50)  NOT NULL UNIQUE,
    cost         NUMBER(10,2)  NOT NULL,
    message_limit NUMBER       NOT NULL,
    pro_access   CHAR(1)       NOT NULL CHECK (pro_access IN ('Y','N'))
);

-- 2. SupportAgent
CREATE TABLE SupportAgent (
    agent_id    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR2(100) NOT NULL,
    email       VARCHAR2(150) NOT NULL UNIQUE,
    specialty   VARCHAR2(100) NOT NULL,
    hired_date  DATE          NOT NULL
);

-- 3. "User" (quoted — Oracle reserved word)
CREATE TABLE "User" (
    user_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tier_id       NUMBER        NOT NULL REFERENCES Tier(tier_id),
    name          VARCHAR2(100) NOT NULL,
    email         VARCHAR2(150) NOT NULL UNIQUE,
    creation_date DATE          NOT NULL,
    language      VARCHAR2(50)  NOT NULL
);

-- 4. BillingRecord
CREATE TABLE BillingRecord (
    user_id         NUMBER        PRIMARY KEY REFERENCES "User"(user_id) ON DELETE CASCADE,
    payment_method  VARCHAR2(50)  NOT NULL,
    billing_address VARCHAR2(300) NOT NULL
);

-- 5. Invoice
CREATE TABLE Invoice (
    invoice_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        NUMBER        NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    tier_id        NUMBER        NOT NULL REFERENCES Tier(tier_id),
    invoice_date   DATE          NOT NULL,
    amount         NUMBER(10,2)  NOT NULL,
    payment_status VARCHAR2(20)  NOT NULL CHECK (payment_status IN ('Paid','Unpaid','Overdue'))
);

-- 6. Workspace
CREATE TABLE Workspace (
    workspace_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id     NUMBER        NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    name         VARCHAR2(150) NOT NULL,
    visibility   VARCHAR2(20)  NOT NULL CHECK (visibility IN ('PRIVATE','SHARED')),
    created_at   TIMESTAMP     NOT NULL
);

-- 7. WorkspaceMember
CREATE TABLE WorkspaceMember (
    workspace_id NUMBER    NOT NULL REFERENCES Workspace(workspace_id) ON DELETE CASCADE,
    user_id      NUMBER    NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    role         VARCHAR2(20) NOT NULL CHECK (role IN ('OWNER','MEMBER')),
    joined_at    TIMESTAMP NOT NULL,
    PRIMARY KEY (workspace_id, user_id)
);

-- 8. Persona
CREATE TABLE Persona (
    persona_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_user_id  NUMBER        NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    name           VARCHAR2(150) NOT NULL,
    instructions   CLOB          NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    UNIQUE (owner_user_id, name)
);

-- 9. Conversation
CREATE TABLE Conversation (
    conversation_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 NUMBER        NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    workspace_id            NUMBER        REFERENCES Workspace(workspace_id) ON DELETE SET NULL,
    persona_id              NUMBER        REFERENCES Persona(persona_id) ON DELETE SET NULL,
    title                   VARCHAR2(300) NOT NULL,
    start_time              TIMESTAMP     NOT NULL,
    status                  VARCHAR2(20)  NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
    persona_name_snapshot   VARCHAR2(150),
    persona_instr_snapshot  CLOB
);

-- 10. Message
CREATE TABLE Message (
    message_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id NUMBER       NOT NULL REFERENCES Conversation(conversation_id) ON DELETE CASCADE,
    sender_role     VARCHAR2(10) NOT NULL CHECK (sender_role IN ('USER','AI')),
    time_sent       TIMESTAMP    NOT NULL,
    content         CLOB         NOT NULL
);

-- 11. MessageFeedback
CREATE TABLE MessageFeedback (
    feedback_id  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message_id   NUMBER       NOT NULL REFERENCES Message(message_id) ON DELETE CASCADE UNIQUE,
    rating       VARCHAR2(20) NOT NULL CHECK (rating IN ('Thumbs Up','Thumbs Down')),
    comment_text CLOB,
    submitted_at TIMESTAMP    NOT NULL
);

-- 12. MessageBookmark
CREATE TABLE MessageBookmark (
    user_id         NUMBER    NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    message_id      NUMBER    NOT NULL REFERENCES Message(message_id) ON DELETE CASCADE,
    bookmarked_time TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, message_id)
);

-- 13. PromptTemplate
CREATE TABLE PromptTemplate (
    template_id    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_user_id  NUMBER        NOT NULL REFERENCES "User"(user_id) ON DELETE CASCADE,
    name           VARCHAR2(150) NOT NULL,
    text           CLOB          NOT NULL,
    category       VARCHAR2(100) NOT NULL,
    visibility     VARCHAR2(20)  NOT NULL CHECK (visibility IN ('PRIVATE','SHARED')),
    created_at     TIMESTAMP     NOT NULL
);

-- 14. WorkspacePromptTemplate
CREATE TABLE WorkspacePromptTemplate (
    workspace_id NUMBER    NOT NULL REFERENCES Workspace(workspace_id) ON DELETE CASCADE,
    template_id  NUMBER    NOT NULL REFERENCES PromptTemplate(template_id) ON DELETE CASCADE,
    shared_at    TIMESTAMP NOT NULL,
    PRIMARY KEY (workspace_id, template_id)
);

-- 15. SupportTicket
CREATE TABLE SupportTicket (
    ticket_id    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      NUMBER       NOT NULL REFERENCES "User"(user_id),
    agent_id     NUMBER       REFERENCES SupportAgent(agent_id) ON DELETE SET NULL,
    topic_name   VARCHAR2(50) NOT NULL CHECK (topic_name IN ('Billing','Model Error','Account','Feature Request','Other')),
    opened_time  TIMESTAMP    NOT NULL,
    closed_time  TIMESTAMP,
    status       VARCHAR2(20) NOT NULL CHECK (status IN ('Open','In Progress','Resolved','Escalated')),
    CONSTRAINT chk_ticket_closed CHECK (
        (status IN ('Resolved','Escalated') AND closed_time IS NOT NULL) OR
        (status NOT IN ('Resolved','Escalated') AND closed_time IS NULL)
    )
);
