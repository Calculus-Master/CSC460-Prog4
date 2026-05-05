Program #4 — LLM Platform Management System
CSc 460 — Database Design, Spring 2026
Team: Ojas Sanghi, Saptarshi Mallick


=== COMPILATION ===

Run from the prog4/ directory on lectura:

    bash run.sh

Or manually:

    javac -cp "/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar" -d . src/*.java


=== EXECUTION ===

After compiling, run:

    java -cp ".:/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar" Main

The program will prompt for Oracle credentials (username and password).
Press Enter at both prompts to use the default credentials for our schema
on aloe.cs.arizona.edu (mallicksap / a6515).

The program must be run from lectura.cs.arizona.edu, which has network
access to aloe.cs.arizona.edu and the Oracle JDBC driver installed at
/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar.


=== DATABASE SETUP ===

The schema and sample data are already loaded on aloe.cs.arizona.edu under
the mallicksap account. If you need to reload from scratch, connect via
SQL*Plus and run the scripts in order:

    1. sql/create_tables.sql   -- drops and recreates all tables, sequences, triggers
    2. sql/triggers.sql        -- creates application-logic triggers
    3. sql/populate.sql        -- inserts sample data for testing

From lectura:
    sqlplus mallicksap@oracle.aloe / (password: a6515)

Or using the connect.txt shortcut in src/:
    sqlpl mallicksap@oracle.aloe


=== EXERCISING THE REQUIRED FUNCTIONALITIES ===

The main menu has 8 functional submenus plus a query menu:

  1. User Account Management
     - Add User: creates a user and an associated BillingRecord
     - Update User: change tier, name, email, or language
     - Delete User: blocked if unpaid invoices or open/in-progress tickets exist
     - View All Users

  2. Conversations & Messages
     - Start Conversation: optionally attach a persona and/or workspace
     - Add Message: USER or AI role; enforces tier daily message rate limit for USER messages
     - Update Message Feedback: thumbs up/down with optional comment (AI messages only)
     - Bookmark Message
     - View Conversation Messages
     - Archive Conversation

  3. Workspace Organization
     - Create Workspace: auto-adds creator as OWNER member
     - Modify Workspace: name or visibility
     - Add Member: adds a user as MEMBER
     - Move Conversation to Workspace: verifies conversation owner is a workspace member first
     - View Workspace Members

  4. Persona Management
     - Create Persona
     - Update Persona: name or instructions
     - Delete Persona: blocked if more than 5 active conversations reference it
     - View My Personas

  5. Prompt Library
     - Add Template
     - Update Template: name, text, category, or visibility
     - Share Template with Workspace: only SHARED-visibility templates allowed
     - View Templates for User

  6. Billing & Subscriptions
     - Generate Invoice: snapshots current tier and cost; auto-marks free-tier as Paid
     - Mark Invoice Paid
     - View User Invoices

  7. Support Tickets
     - Create Ticket: choose from 5 topic categories
     - Assign to Agent: lists available agents; sets status to In Progress
     - Resolve / Escalate Ticket: sets closed_time and terminal status
     - View Open Tickets

  8. Database Queries
     - Q1: Bookmarked messages for a given user (with conversation title and timestamp)
     - Q2: All users with unpaid invoices (email, total owed, last conversation date)
     - Q3: Most helpful persona (highest thumbs-up percentage across all linked conversations)
     - Q4: For a given user, all persona-linked conversations with AI message counts


=== WORKLOAD DISTRIBUTION ===

Ojas Sanghi:
  - Database schema design
  - SQL code and implementation
  - design.pdf and readme.txt

Saptarshi Mallick:
  - Application logic
  - Input handling, JDBC connection
