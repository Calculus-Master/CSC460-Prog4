-- Drop triggers before tables to avoid constraint conflicts
BEGIN
    FOR tr IN (SELECT trigger_name FROM user_triggers) LOOP
        EXECUTE IMMEDIATE 'DROP TRIGGER "' || tr.trigger_name || '"';
    END LOOP;
END;
/


-- Drop tables; cascade handles FK dependencies
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS';
    END LOOP;
END;
/


-- Drop sequences
BEGIN
    FOR s IN (
        SELECT sequence_name
        FROM user_sequences
        WHERE sequence_name IN (
            'TIER_ID_SEQ', 'SUPPORTAGENT_ID_SEQ', 'USER_ID_SEQ', 'INVOICE_ID_SEQ',
            'WORKSPACE_ID_SEQ', 'PERSONA_ID_SEQ', 'CONVERSATION_ID_SEQ', 'MESSAGE_ID_SEQ',
            'MESSAGEFEEDBACK_ID_SEQ', 'PROMPTTEMPLATE_ID_SEQ', 'SUPPORTTICKET_ID_SEQ'
        )
    ) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
END;
/


-- 1. Tier
CREATE TABLE Tier (
    tier_id       NUMBER        PRIMARY KEY,
    tier_name     VARCHAR2(50)  NOT NULL UNIQUE,
    cost          NUMBER(10,2)  NOT NULL,
    message_limit NUMBER        NOT NULL,
    pro_access    CHAR(1)       NOT NULL CHECK (pro_access IN ('Y','N'))
);

CREATE SEQUENCE tier_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER tier_bi
BEFORE INSERT ON Tier
FOR EACH ROW
WHEN (NEW.tier_id IS NULL)
BEGIN
    :NEW.tier_id := tier_id_seq.NEXTVAL;
END;
/


-- 2. SupportAgent
CREATE TABLE SupportAgent (
    agent_id    NUMBER        PRIMARY KEY,
    name        VARCHAR2(100) NOT NULL,
    email       VARCHAR2(150) NOT NULL UNIQUE,
    specialty   VARCHAR2(100) NOT NULL,
    hired_date  DATE          NOT NULL
);

CREATE SEQUENCE supportagent_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER supportagent_bi
BEFORE INSERT ON SupportAgent
FOR EACH ROW
WHEN (NEW.agent_id IS NULL)
BEGIN
    :NEW.agent_id := supportagent_id_seq.NEXTVAL;
END;
/


-- 3. LLMUser
CREATE TABLE LLMUser (
    user_id        NUMBER        PRIMARY KEY,
    tier_id        NUMBER        NOT NULL REFERENCES Tier(tier_id),
    name           VARCHAR2(100) NOT NULL,
    email          VARCHAR2(150) NOT NULL UNIQUE,
    creation_date  DATE          NOT NULL,
    language       VARCHAR2(50)  NOT NULL
);

CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER user_bi
BEFORE INSERT ON LLMUser
FOR EACH ROW
WHEN (NEW.user_id IS NULL)
BEGIN
    :NEW.user_id := user_id_seq.NEXTVAL;
END;
/


-- 4. BillingRecord
CREATE TABLE BillingRecord (
    user_id         NUMBER        PRIMARY KEY REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    payment_method  VARCHAR2(50)  NOT NULL,
    billing_address VARCHAR2(300) NOT NULL
);


-- 5. Invoice
CREATE TABLE Invoice (
    invoice_id      NUMBER        PRIMARY KEY,
    user_id         NUMBER        NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    tier_id         NUMBER        NOT NULL REFERENCES Tier(tier_id),
    invoice_date    DATE          NOT NULL,
    amount          NUMBER(10,2)  NOT NULL,
    payment_status  VARCHAR2(20)  NOT NULL CHECK (payment_status IN ('Paid','Unpaid','Overdue'))
);

CREATE SEQUENCE invoice_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER invoice_bi
BEFORE INSERT ON Invoice
FOR EACH ROW
WHEN (NEW.invoice_id IS NULL)
BEGIN
    :NEW.invoice_id := invoice_id_seq.NEXTVAL;
END;
/


-- 6. Workspace
CREATE TABLE Workspace (
    workspace_id NUMBER        PRIMARY KEY,
    owner_id     NUMBER        NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    name         VARCHAR2(150) NOT NULL,
    visibility   VARCHAR2(20)  NOT NULL CHECK (visibility IN ('PRIVATE','SHARED')),
    created_at   TIMESTAMP     NOT NULL
);

CREATE SEQUENCE workspace_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER workspace_bi
BEFORE INSERT ON Workspace
FOR EACH ROW
WHEN (NEW.workspace_id IS NULL)
BEGIN
    :NEW.workspace_id := workspace_id_seq.NEXTVAL;
END;
/


-- 7. WorkspaceMember
CREATE TABLE WorkspaceMember (
    workspace_id NUMBER       NOT NULL REFERENCES Workspace(workspace_id) ON DELETE CASCADE,
    user_id      NUMBER       NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    role         VARCHAR2(20) NOT NULL CHECK (role IN ('OWNER','MEMBER')),
    joined_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (workspace_id, user_id)
);


-- 8. Persona
CREATE TABLE Persona (
    persona_id      NUMBER        PRIMARY KEY,
    owner_user_id   NUMBER        NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    name            VARCHAR2(150) NOT NULL,
    instructions    CLOB          NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    UNIQUE (owner_user_id, name)
);

CREATE SEQUENCE persona_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER persona_bi
BEFORE INSERT ON Persona
FOR EACH ROW
WHEN (NEW.persona_id IS NULL)
BEGIN
    :NEW.persona_id := persona_id_seq.NEXTVAL;
END;
/


-- 9. Conversation
CREATE TABLE Conversation (
    conversation_id        NUMBER        PRIMARY KEY,
    user_id                NUMBER        NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    workspace_id           NUMBER        REFERENCES Workspace(workspace_id) ON DELETE SET NULL,
    persona_id             NUMBER        REFERENCES Persona(persona_id) ON DELETE SET NULL,
    title                  VARCHAR2(300) NOT NULL,
    start_time             TIMESTAMP     NOT NULL,
    status                 VARCHAR2(20)  NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
    persona_name_snapshot  VARCHAR2(150),
    persona_instr_snapshot CLOB
);

CREATE SEQUENCE conversation_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER conversation_bi
BEFORE INSERT ON Conversation
FOR EACH ROW
WHEN (NEW.conversation_id IS NULL)
BEGIN
    :NEW.conversation_id := conversation_id_seq.NEXTVAL;
END;
/


-- 10. Message
CREATE TABLE Message (
    message_id       NUMBER        PRIMARY KEY,
    conversation_id  NUMBER        NOT NULL REFERENCES Conversation(conversation_id) ON DELETE CASCADE,
    sender_role      VARCHAR2(10)  NOT NULL CHECK (sender_role IN ('USER','AI')),
    time_sent        TIMESTAMP     NOT NULL,
    content          CLOB          NOT NULL
);

CREATE SEQUENCE message_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER message_bi
BEFORE INSERT ON Message
FOR EACH ROW
WHEN (NEW.message_id IS NULL)
BEGIN
    :NEW.message_id := message_id_seq.NEXTVAL;
END;
/


-- 11. MessageFeedback
CREATE TABLE MessageFeedback (
    feedback_id   NUMBER        PRIMARY KEY,
    message_id    NUMBER        NOT NULL REFERENCES Message(message_id) ON DELETE CASCADE UNIQUE,
    rating        VARCHAR2(20)  NOT NULL CHECK (rating IN ('Thumbs Up','Thumbs Down')),
    comment_text  CLOB,
    submitted_at  TIMESTAMP     NOT NULL
);

CREATE SEQUENCE messagefeedback_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER messagefeedback_bi
BEFORE INSERT ON MessageFeedback
FOR EACH ROW
WHEN (NEW.feedback_id IS NULL)
BEGIN
    :NEW.feedback_id := messagefeedback_id_seq.NEXTVAL;
END;
/


-- 12. MessageBookmark
CREATE TABLE MessageBookmark (
    user_id          NUMBER        NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    message_id       NUMBER        NOT NULL REFERENCES Message(message_id) ON DELETE CASCADE,
    bookmarked_time  TIMESTAMP     NOT NULL,
    PRIMARY KEY (user_id, message_id)
);


-- 13. PromptTemplate
CREATE TABLE PromptTemplate (
    template_id     NUMBER        PRIMARY KEY,
    owner_user_id   NUMBER        NOT NULL REFERENCES LLMUser(user_id) ON DELETE CASCADE,
    name            VARCHAR2(150) NOT NULL,
    text            CLOB          NOT NULL,
    category        VARCHAR2(100) NOT NULL,
    visibility      VARCHAR2(20)  NOT NULL CHECK (visibility IN ('PRIVATE','SHARED')),
    created_at      TIMESTAMP     NOT NULL
);

CREATE SEQUENCE prompttemplate_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER prompttemplate_bi
BEFORE INSERT ON PromptTemplate
FOR EACH ROW
WHEN (NEW.template_id IS NULL)
BEGIN
    :NEW.template_id := prompttemplate_id_seq.NEXTVAL;
END;
/


-- 14. WorkspacePromptTemplate
CREATE TABLE WorkspacePromptTemplate (
    workspace_id NUMBER    NOT NULL REFERENCES Workspace(workspace_id) ON DELETE CASCADE,
    template_id  NUMBER    NOT NULL REFERENCES PromptTemplate(template_id) ON DELETE CASCADE,
    shared_at    TIMESTAMP NOT NULL,
    PRIMARY KEY (workspace_id, template_id)
);


-- 15. SupportTicket
CREATE TABLE SupportTicket (
    ticket_id     NUMBER        PRIMARY KEY,
    user_id       NUMBER        NOT NULL REFERENCES LLMUser(user_id),
    agent_id      NUMBER        REFERENCES SupportAgent(agent_id) ON DELETE SET NULL,
    topic_name    VARCHAR2(50)  NOT NULL CHECK (topic_name IN ('Billing','Model Error','Account','Feature Request','Other')),
    opened_time   TIMESTAMP     NOT NULL,
    closed_time   TIMESTAMP,
    status        VARCHAR2(20)  NOT NULL CHECK (status IN ('Open','In Progress','Resolved','Escalated')),
    CONSTRAINT chk_ticket_closed CHECK (
        (status IN ('Resolved','Escalated') AND closed_time IS NOT NULL) OR
        (status NOT IN ('Resolved','Escalated') AND closed_time IS NULL)
    )
);

CREATE SEQUENCE supportticket_id_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER supportticket_bi
BEFORE INSERT ON SupportTicket
FOR EACH ROW
WHEN (NEW.ticket_id IS NULL)
BEGIN
    :NEW.ticket_id := supportticket_id_seq.NEXTVAL;
END;
/
