# Blueking (BK-JOB) Architecture Design

English | [简体中文](architecture.md)

![Architecture](../resource/img/architecture.png)

The BlueKing Platform (aka **bk-job** ) is written in multiple languages including java/js/html/python/shell/gradle, featuring separation of frontend and backend, with highly-accessible, scalabe, service-oriented architecture:

- **Frontend (FrontEnd):**

  Features ES6 grammar, MVVM architecture vue.js, and webpack4 package.

- **Backend (BackEnd):** 

Written in Java, featuring microservice architecture of Spring Cloud. Here is a brief rundown on each of the microservice modules:

  - **job-config:** Configuration Center: Manages and coordinates the configuration information of all microservices.
  - **job-gateway:** Microservice Gateway: In charge of authentication, traffic throttling, routing requests, etc.
  - **job-manage:** Job Management Microservice: In charge of managing the resources on the platform, including script, account, job template, execution plans, notification, and general settings.
  - **job-execute:** Job Execution Microservice: Serves Blueking GSE by submitting file distribution/script execution tasks, retrieving task logs, and converting task statuses.
  - **job-logsvr:** Log Management Microservice: Connects to the underlying MongoDB. In charge of storing the logs generated from script execution, file distribution, and source file downloading.
  - **job-crontab:** Scheduled Task Microservice. Task scheduling and management of BK-job based on the Quartz engine.
  - **job-backup:** Backup Management Microservice: Imports and exports jobs in bk-job and archives job execution history on a regular basis.
  - **job-analysis:** Statistical Analysis Microservice. Provides backend access to job error message on the main page and operation analysis module; dispatches timed tasks; acquires and analyzes the metadata from other microservices; generates analysis result and statistics; provides data for the operation of bk-job; improves accessibility of the platform.
  - **job-ticket:** Credential Management Microservice: Provides a variety of credentials for the third party file source connected to the file gateway; stores, encrypts and decrypts credentials.
  - **job-file-gateway:** File Gateway Microservice: Connects with various third party file sources via FileWorker (object-based storage, file system storage, etc.); schedules the file downloading tasks from file source; works with the execution engine to distribute files from third party file source.
  - **job-file-worker:** An access point, an expandable module independent of other bk-job backend microservice; multiple instances can be set up; communicate with the file gateway to connect to various types of third party file sources; executes file downloading tasks.

- **Resource Service (Resource):** Provides the necessary basic middleware for storage.
    - **Consul:** Used as a service discovery server; You need to build a Consul Server and install Consul on the machine where bk-job microservices are deployed, and run it as Agent.
    - **RabbitMQ:** Core Message Queue Service: bk-job's task execution engine uses RabbitMQ to deliver the message of task status changes.
    - **MySQL:** bk-job's primary database. Uses mysql 5.7.2 to store the relational data of all microservices listed above.
    - **Redis:** Core Service Cache 4.0.14: Provides host information for distributed locks and cache service.
    - **MongoDB:**  BK-job's log database. MongoDB 4.2.2. Used to store the log data generated from script execution/file distribution.
    - **NFS:** Used to store user-uploaded local files in local file distribution scenario and store temporary files generated from job import and export.

