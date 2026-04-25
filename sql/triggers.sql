-- CSc 460 Program 4 — Triggers

-- Trigger 1: MessageFeedback may only be placed on AI messages
CREATE OR REPLACE TRIGGER trg_feedback_ai_only
BEFORE INSERT OR UPDATE ON MessageFeedback
FOR EACH ROW
DECLARE
    v_role Message.sender_role%TYPE;
BEGIN
    SELECT sender_role INTO v_role
    FROM Message
    WHERE message_id = :NEW.message_id;

    IF v_role <> 'AI' THEN
        RAISE_APPLICATION_ERROR(-20001,
            'Feedback can only be given on AI messages (sender_role = AI).');
    END IF;
END;
/

-- Trigger 2: WorkspacePromptTemplate requires template visibility = SHARED
CREATE OR REPLACE TRIGGER trg_wpt_shared_only
BEFORE INSERT ON WorkspacePromptTemplate
FOR EACH ROW
DECLARE
    v_vis PromptTemplate.visibility%TYPE;
BEGIN
    SELECT visibility INTO v_vis
    FROM PromptTemplate
    WHERE template_id = :NEW.template_id;

    IF v_vis <> 'SHARED' THEN
        RAISE_APPLICATION_ERROR(-20002,
            'Only SHARED templates can be added to a workspace.');
    END IF;
END;
/
