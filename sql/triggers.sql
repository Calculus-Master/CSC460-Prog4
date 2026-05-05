-- CSc 460 Program 4 -- Triggers

-- Ensure feedback is only submitted on AI-generated messages
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
            'Feedback can only be given on AI messages.');
    END IF;
END;
/

-- Prevent adding private templates to a workspace
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
