Program #4, CSC 460 Database Design
Team: Ojas Sanghi, Saptarshi Mallick

--- COMPILATION ---

Run `run.sh`.

Or, manually: `javac -cp "/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar" -d . src/*.java`

--- EXECUTION ---

After compiling, run:
`java -cp ".:/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar" Main`

The program will prompt for Oracle credentials (username and password).

Press Enter at both prompts to use the default credentials for our schema on aloe.cs.arizona.edu (mallicksap / a6515).

The program must be run from lectura.cs.arizona.edu, which has network access to aloe.cs.arizona.edu and the Oracle JDBC driver installed at /usr/lib/oracle/19.8/client64/lib/ojdbc8.jar.

--- DATABASE SETUP ---

The schema and sample data are already loaded on aloe.cs.arizona.edu under
the mallicksap account. To set up your SQL tables, run the following:

First, login: sqlplus mallicksap@oracle.aloe / (password: a6515)

Then, run these scripts:

@ sql/create_tables.sql -- drops and recreates all tables, sequences, and auto-increment triggers
@ sql/populate.sql -- populate with placeholder data

--- WORKLOAD DISTRIBUTION ---

Ojas Sanghi:
  - Database schema design
  - SQL code and implementation
  - design.pdf and readme.txt

Saptarshi Mallick:
  - Application logic
  - Input handling, JDBC connection
  - SQL code and implementation

Both members agree that the workload was evenly distributed.