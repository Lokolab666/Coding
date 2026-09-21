# Database Scripting #

## Table of Contents ##
1. [Overview of schema setup](#overview-of-schema-setup)
1. [Script directory structure](#script-directory-structure)
1. [File Naming Conventions](#file-naming-conventions)
   1. [Driver Script](#driver-script)
1. [Object Naming Conventions](#object-naming-conventions)
1. [Synonyms](#synonyms)
1. [Grants](#grants)
1. [Index and Data Tablespaces](#index-and-data-tablespaces)
1. [Use of Indexes](#use-of-indexes)
1. [Use of Sequences](#use-of-sequences)
1. [Examples](#examples)
   * [Table example](#table-example)
   * [Sequence example](#sequence-example)
   * [View example](#view-example)
1. Database Environments and Promotions

## Overview of schema setup {#overview-of-schema-setup}
Each project is setup with a unique schema inside of a database.  On some occasions multiple applications will leverage the same schema, but this should be avoided unless there is a specific requirement to use an existing schema.

Each schema has a short name or acrynonm associated with it.  In this document we will use **APP** as the short name associated with the project and schema.  The following Oracle users are setup as part of this schema.  Technically, each user has a unique schema but by convention we keep these three users associated with the project's schema.

* **APP_MAIN** - this is the schema owner and generally what we consider the name of the schema.  This user has permission to create/update/remove database objects in the schema.  The only time to connect to the database as this user is to make database changes using scripts in the source code control system.  An application should **never** connect to the database using this user.

* **APP_UPDATE** - this user has select/insert/update/delete to the tables in the schema.  This permission comes from role assignements and the conventions in the scripts, thus it is important to always follow these conventions.  This is usually the user that the application uses to connect to the database.

* **APP_VIEW** - this user has read-only access to the schema.  This user is used if the application does not need to make updates and/or for a report/export generation process.  This should be used when debugging in production when you just want to view the data.

There are two **roles** setup with the schema in the **WEB** database (this includes the **WEB12** database):
* **WEB_APP_UPDATE** this is used for grants in scripts and the user APP_UPATE has the role assigned
* **WEB_APP_VIEW** this is used for grants in scripts and the user APP_VIEW has the role assigned

There are also two tablespaces set up with the schema:
* **APP_DS** is used for storing the data in a tables
* **APP_IS** is used for storing indexes in the schema
Having separate tablespaces for these makes the database more efficient as the storage for these can be separated allowing more parallel access.

## Script Directory Structure ##
The scripts to create tables and other database objects are stored in a directory named **scripts** in the root of the project directory.  Inside the **scripts** directory there is a subdirectory with a two digit number holding all scripts for a particular release, i.e., **01, 02, 03**, etc.  So the initial release will have mostly 'create' scripts in the 01 directory.  Scripts needed for the second release will go in the 02 directory.  Note that scripts in the 01 are not changed to add additional columns for a subsequent release, instead an 'alter' script is create in the new subdirectory which adds the necessary columns.  The purpose of this is to prevent deleting and recreating an existing production table - for the obvious reasons.

Here is an example of what a basic structure would look like:
- ProjectDirectory
   - scripts
      - 01
	     - 01_cr_table_APP_PRODUCT.sql
		 - 01_cr_table_APP_USER.sql
		 - 01_APP_driver.sql
	  - 02
	     - 02_al_table_APP_PRODUCT.sql
	  - 03
	     - 03_al_table_APP_PRODUCT.sql
		 - 03_al_table_APP_USER.sql

## File Naming Conventions ##
The file naming convention for the database scripts follows the format:  release_verb_objectType_objectName.sql

* **release**
   * matches the diretory name (01, 02, etc.)
* **verb** is two letter shortcut
   * cr:  create the object
   * al:  alter the object
   * ld:  load data
   * dr:  drop the object
* Common **objectType** to use
   * table
   * view
   * seq:  sequence
   * idx:  index
   * mat_view: materialized views
   * proc:  procedure
   * fct:  function
* **objectName** follows the naming conventions below

Example:  01_cr_table_APP_PRODUCT.sql

### Driver Script ###
In a release with multiple scripts that need to executed, a __driver__ script should be created.  This script will call the other scripts in the appropriate order.  The driver script is used when preparing an environment for the release.

An example driver would look something like this:
```sql
@01_cr_seq_APP_COMMON_SEQ.sql
@01_cr_table_APP_USER.sql
@01_cr_table_APP_PRODUCT.sql
@01_ld_table_APP_PRODUCT.sql
```

## Object Naming Conventions ##
All objects should start with the short name for the project - **APP** in this document.  Follow this with an underscore.  This ensures objects will not be created with names that conflict with objects in other schemas.

Objects should end with the followng suffix based upon the type:
* VW:  used for views
* SEQ: used for sequences
* MV:  used for materialized views
* IDX: used for indexes
* PK:  used for primary keys
* SP:  used for stored procedures
* TV:  used for "table views" - these are actually tables but loaded by an external process

Other conventions:
- In between the project prefix and the object type suffix, there should be a meaningful name that uses underscores to separate words.
- Table name size is limited to 30 characters in Oracle, so abbreviate where necessary but limit if it impacts the clarity of the name.
- Avoid plural labels.  For example use APP_USER and not --APP_USERS--.
- For join tables, use the main name from both tables.  For example ```APP_USER_PRODUCT``` would be a join table for ```APP_USER``` and ```APP_PRODUCT```.

## Synonyms ##
Each object that is created is owned by the APP_MAIN user, so public synonyms need to be created that allow other uses to access these objects without including the schema owner in the name.  In other words, we want the APP_UPDATE and APP_VIEW uses to be able to run a query like ```select count(*) from APP_PRODUCT``` without having to use APP_MAIN.APP_PRODUCT.

Public synonyms are created using this syntax in the create scripts:
```sql
CREATE PUBLIC SYNONYM APP_PRODUCT FOR APP_MAIN.APP_PRODUCT;
```
## Grants ##
Again, as each object is owned by the APP_MAIN user, grants need to be created to allow access from the other users.  Specifically, grants are done to __roles__ that have users associated with them.  This is an important step to maintain the conventions for an APP_UPDATE user to be able to update data and the APP_VIEW user to select the data.

Code similar to the following should be included with each object created:
```sql
GRANT DELETE, INSERT, SELECT, UPDATE ON APP_MAIN.APP_PRODUCT TO WEB_APP_UPDATE;

GRANT SELECT ON APP_MAIN.APP_PRODUCT TO WEB_APP_VIEW;
```

Note that multiple actions are granted to the WEB_APP_UPDATE role while only __select__ is granted to the WEB_APP_VIEW role.  This means that the APP_UPDATE user (assigned the WEB_APP_UPDATE role) will be able to make changes to table while the APP_VIEW user (assigned the WEB_APP_VIEW role) can only read the table.

For sequences, no grants are needed for the WEB_APP_VIEW role.
For views, no delete, insert or update actions are need for the WEB_APP_UPDATE role.

## Index and Data Tablespaces ##
In the script to create a table, ensure that the 'data' tablespace is defined:  ```TABLESPACE APP_DS;```
In the scripts to create index or primary keys, ensure that the 'index' tablespace is defined:  ```TABLESPACE APP_IS;```

## Use of Indexes ##

## Use of Sequences ##
Unless there is a specific need to have a sequencial id for a table, it is recommended to create one common index instead of one for each table.  In other words, create one sequence shared by all the tables for unique id values instead of many sequences as the value of the unique id generally provides little value when not sequencial.

If a table does require sequencial id values, then care must be taken to define the sequence such that it only provides one nextval at a time - otherwise clustered servers could reserve blocks of values which would impact the sequencing.

## Examples ##

### Table example ###

```sql
ALTER TABLE APP_MAIN.APP_SESSION
 DROP PRIMARY KEY CASCADE;

Prompt Dropping table APP_SESSION and its indexes
Prompt

DROP TABLE APP_MAIN.APP_SESSION CASCADE CONSTRAINTS;

Prompt Dropping public synonym APP_SESSION for APP_SESSION
Prompt

DROP PUBLIC SYNONYM APP_SESSION;

Prompt Creating table APP_SESSION
Prompt

CREATE TABLE APP_MAIN.APP_SESSION
(
  SESSION_ID					VARCHAR2(256 CHAR) NOT NULL,
  VALID_SESSION					VARCHAR2(1 CHAR) NOT NULL,
  MAX_INACTIVE					NUMBER(10) NOT NULL,
  LAST_ACCESS					NUMBER(19),
  APP_NAME						VARCHAR2(255 CHAR),
  SESSION_DATA					BLOB
)
TABLESPACE TTT_DS;

Prompt Creating unique index APP_SESSION for APP_SESSION
Prompt

CREATE UNIQUE INDEX APP_MAIN.APP_SESSION_PK ON APP_MAIN.APP_SESSION
    (SESSION_ID)
    TABLESPACE TTT_IS;

Prompt adding primary_key constraint to  APP_SESSION
Prompt

ALTER TABLE APP_MAIN.APP_SESSION ADD (
    CONSTRAINT APP_SESSION_PK
        PRIMARY KEY
            (SESSION_ID)
            USING INDEX APP_MAIN.APP_SESSION_PK
            ENABLE VALIDATE);

Prompt Creating public synonym APP_SESSION for APP_SESSION
Prompt

CREATE PUBLIC SYNONYM APP_SESSION FOR APP_MAIN.APP_SESSION;

Prompt Granting APP_SESSION Access to Roles
Prompt

GRANT DELETE, INSERT, SELECT, UPDATE ON APP_MAIN.APP_SESSION TO WEB_APP_UPDATE;

GRANT SELECT ON APP_MAIN.APP_SESSION TO WEB_APP_VIEW;

Prompt creation of table APP_SESSION completed normally
Prompt
```

### Sequence example ###
```sql
DROP SEQUENCE APP_MAIN.APP_COMMON_SEQ;

CREATE SEQUENCE APP_MAIN.APP_COMMON_SEQ INCREMENT BY 1 START WITH 1;

DROP PUBLIC SYNONYM APP_COMMON_SEQ;

CREATE PUBLIC SYNONYM APP_COMMON_SEQ FOR APP_MAIN.APP_COMMON_SEQ;

GRANT SELECT ON  APP_MAIN.APP_COMMON_SEQ TO WEB_APP_UPDATE;
```

### View example ###
```sql
Prompt Dropping view APP_PROJECT_VW and its indexes
Prompt

DROP VIEW APP_MAIN.APP_PROJECT_VW CASCADE CONSTRAINTS;

Prompt Dropping public synonym APP_PROJECT_VW for APP_PROJECT_VW
Prompt

DROP PUBLIC SYNONYM APP_PROJECT_VW;

Prompt Creating view APP_PROJECT_VW
Prompt

CREATE OR REPLACE FORCE VIEW APP_MAIN.APP_PROJECT_VW
(
    SAP_WBS_NUMBER,
    SAP_TITLE,
    PROJECT_OWNER_USERID,
    PROJECT_OWNER_NAME,
    ALT_PJT_OWNER_USERID,
    ALT_PJT_OWNER_NAME,
    STATUS,
    IS_MANAGED_SVC,
    SAP_START_DATE,
    ADJUSTED_END_DATE,
    SAP_CC_RESOURCE,
    SAP_CC_CHARGEBACK
)
AS
select P.SAP_WBS_NUMBER,
       TV.SAP_TITLE,
       DIR1.USERID,
       decode(dir1.mdtuniqueness, null, dir1.sn || ', ' || dir1.givenname, dir1.sn || ', ' || dir1.givenname || ' [' || dir1.mdtuniqueness || ']'),
       DIR2.USERID,
       decode(dir2.mdtuniqueness, null, dir2.sn || ', ' || dir2.givenname, dir2.sn || ', ' || dir2.givenname || ' [' || dir2.mdtuniqueness || ']'),
       P.STATUS,
       P.IS_MANAGED_SVC,
       TV.SAP_START_DATE,
       P.ADJUSTED_END_DATE,
       TV.SAP_CC_RESOURCE,
       TV.SAP_CC_CHARGEBACK
    from ttt_project p, ttt_sap_project_tv tv, restricted_ent_dir_vw dir1, restricted_ent_dir_vw dir2
    where P.PROJECT_OWNER = DIR1.MDTDIRECTORYKEYI
    and P.ALT_PROJECT_OWNER = DIR2.MDTDIRECTORYKEYI
    and P.SAP_WBS_NUMBER = TV.SAP_WBS_NUMBER;

Prompt Creating public synonym APP_PROJECT_VW for APP_PROJECT_VW
Prompt

CREATE PUBLIC SYNONYM APP_PROJECT_VW FOR APP_MAIN.APP_PROJECT_VW;

Prompt Granting APP_PROJECT_VW Access to Roles
Prompt

GRANT SELECT ON APP_MAIN.APP_PROJECT_VW TO web_TTT_UPDATE;

GRANT SELECT ON APP_MAIN.APP_PROJECT_VW TO web_TTT_VIEW;

Prompt creation of view APP_PROJECT_VW completed normally
Prompt

```

### Load table example ###
```sql
REM INSERTING into PHY_LOC_COUNTRY
SET DEFINE OFF;
Insert into PHY_LOC_COUNTRY (ID,CODE,CODE_THREE,NAME,USE_METRIC_MEASUREMENTS) values (1,'US','USA','United States',null);
Insert into PHY_LOC_COUNTRY (ID,CODE,CODE_THREE,NAME,USE_METRIC_MEASUREMENTS) values (2,'AU','AUS','Australia',null);
Insert into PHY_LOC_COUNTRY (ID,CODE,CODE_THREE,NAME,USE_METRIC_MEASUREMENTS) values (3,'NZ','NZL','New Zealand',null);
```
