# About Database Script

English | [简体中文](README.md)

## Script Writing Requirements:

- All changes are to be synced to the ddl script
- Requirements: All scripts need to be executed repeatedly without discrepancy (such as power):
  - When inserting data, use INSERT IGNORE INTO to prevent users' data from being overwritten.
  - When it comes to the system data that requires forced refresh, use the updated command INSERT INTO xxx ON DUPLICATE KEY UPDATE or  REPLACE INTO  xxx
  - It is not allowed to use command that deletes then recreates a table or a database. Creation must be made after judgement: CREATE TABLE IF NOT EXISTS "Table Name"
  - To alter the index field, save the process using updated script. When modifying the field, first make a judgement of whether it exists.
- It is not allowed to run the program when it is not updated:
  - Add/change fields. It is not allowed to set it as not null without default value.
  - It is not allowed to change name field.
  - To delete a table or field, simply update ddl. Do not submit the updated script in case of users' misoperation.


## Naming Conventions

{Execution No.}_{db Name}_{Creation Time}-{Script No.}_{Version No.}_{db type}.sql

**About Parameters:**
**Execution No.**: 4 Digits: Start from 0001 to specify execution sequence. If its a repeated number, use the creation time and script no. to determine the execution sequence.
**db Name**:  Name of the database connected to all microservice modules, such as job_manage and job_execute. Use underlines as separators.
**Creation Time**: Creation time in yyyyMMdd format.
**Script No.**: A sequence number starting from 1000, used to distinguish multiple SQL with same creation time in the same library.
**Version No.**: Starts with the letter V, followed by 3/4-digit version number. Prioritize the 4-digit version.
**db Type**: mysql
