
**user** (user_id, _tier_id_, name, email, creation_date, language)
**tier** (tier_id, tier_name, cost, message_limit, pro_access)

**workspace** (workspace_id, owner_id, name, visibility)
**workspace_member** (workspace_id, user_id, role)

**persona** (persona_id, name, instructions)
**conversation** (conversation_id, user_id, workspace_id, persona_id, title, start_time, status)

**message** (message_id, conversation_id, sender_role, time_sent, content)
**message_feedback** (feedback_id, message_id, rating, comment_text)
**message_bookmark** (user_id, message_id, bookmarked_time)

**prompt_template** (template_id, owner_user_id, name, text, visibility)
**workspace_prompt_template** (workspace_id, template_id)

**billing_record** (billing_id, user_id, payment_method, billing_address)
**invoice** (invoice_id, user_id, tier_id, invoice_date, amount, payment_status)

**support_agent** (agent_id, name)
**support_ticket** (ticket_id, user_id, agent_id, topic_name, opened_time, closed_time, resolution_duration, outcome)`
