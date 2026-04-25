# Database Schema

## User
| Column         | Key | References       |
|----------------|-----|------------------|
| user_id        | PK  |                  |
| tier_id        | FK  | Tier.tier_id     |
| name           |     |                  |
| email          |     |                  |
| creation_date  |     |                  |
| language       |     |                  |

## Tier
| Column         | Key | References |
|----------------|-----|------------|
| tier_id        | PK  |            |
| tier_name      |     |            |
| cost           |     |            |
| message_limit  |     |            |
| pro_access     |     |            |


## Workspace
| Column         | Key | References       |
|----------------|-----|------------------|
| workspace_id   | PK  |                  |
| owner_id       | FK  | User.user_id     |
| name           |     |                  |
| visibility     |     |                  |

## WorkspaceMember
| Column         | Key     | References                 |
|----------------|---------|----------------------------|
| workspace_id   | PK, FK  | Workspace.workspace_id     |
| user_id        | PK, FK  | User.user_id               |
| role           |         |                            |


## Persona
| Column         | Key | References |
|----------------|-----|------------|
| persona_id     | PK  |            |
| name           |     |            |
| instructions   |     |            |

## Conversation
| Column           | Key | References                   |
|------------------|-----|------------------------------|
| conversation_id  | PK  |                              |
| user_id          | FK  | User.user_id                 |
| workspace_id     | FK  | Workspace.workspace_id       |
| persona_id       | FK  | Persona.persona_id           |
| title            |     |                              |
| start_time       |     |                              |
| status           |     |                              |


## Message
| Column           | Key | References                      |
|------------------|-----|---------------------------------|
| message_id       | PK  |                                 |
| conversation_id  | FK  | Conversation.conversation_id    |
| sender_role      |     |                                 |
| time_sent        |     |                                 |
| content          |     |                                 |

## MessageFeedback
| Column         | Key | References             |
|----------------|-----|------------------------|
| feedback_id    | PK  |                        |
| message_id     | FK  | Message.message_id     |
| rating         |     |                        |
| comment_text   |     |                        |

## MessageBookmark
| Column           | Key     | References             |
|------------------|---------|------------------------|
| user_id          | PK, FK  | User.user_id           |
| message_id       | PK, FK  | Message.message_id     |
| bookmarked_time  |         |                        |


## PromptTemplate
| Column           | Key | References         |
|------------------|-----|--------------------|
| template_id      | PK  |                    |
| owner_user_id    | FK  | User.user_id       |
| name             |     |                    |
| text             |     |                    |
| visibility       |     |                    |

## WorkspacePromptTemplate
| Column         | Key     | References                       |
|----------------|---------|----------------------------------|
| workspace_id   | PK, FK  | Workspace.workspace_id           |
| template_id    | PK, FK  | PromptTemplate.template_id       |


## BillingRecord
| Column           | Key | References       |
|------------------|-----|------------------|
| billing_id       | PK  |                  |
| user_id          | FK  | User.user_id     |
| payment_method   |     |                  |
| billing_address  |     |                  |

## Invoice
| Column           | Key | References       |
|------------------|-----|------------------|
| invoice_id       | PK  |                  |
| user_id          | FK  | User.user_id     |
| tier_id          | FK  | Tier.tier_id     |
| invoice_date     |     |                  |
| amount           |     |                  |
| payment_status   |     |                  |


## SupportAgent
| Column     | Key | References |
|------------|-----|------------|
| agent_id   | PK  |            |
| name       |     |            |

## SupportTicket
| Column               | Key | References                 |
|----------------------|-----|----------------------------|
| ticket_id            | PK  |                            |
| user_id              | FK  | User.user_id               |
| agent_id             | FK  | SupportAgent.agent_id      |
| topic_name           |     |                            |
| opened_time          |     |                            |
| closed_time          |     |                            |
| resolution_duration  |     |                            |
| outcome              |     |                            |