![LOGO](docs/resource/img/bk-job.png)
---
[![license](https://img.shields.io/badge/license-mit-brightgreen.svg?style=flat)](https://github.com/Tencent/bk-job/blob/master/LICENSE.txt) [![Release Version](https://img.shields.io/github/v/release/Tencent/bk-job?include_prereleases)](https://github.com/Tencent/bk-job/releases)

English | [简体中文](README.md)

> **Notice**: During the process of development, the 'master' branch might be in an *unstable state or unavailable state*.
Please access the stable binary file via [releases](https://github.com/tencent/bk-job/releases) instead of 'master'.

The BK-JOB is a ops script management and execution system with the capability of dealing with multiple tasks simultaneously. In addition to script execution, file distribution, cron jobs, and other basic operation environments, it is also capable of putting together all the individual tasks into an automated workflow. All tasks, as individual nodes, can be offered to upper-level or peripheral systems/platforms, making it an automated cross-system dispatching system.

## Benefits

### Dependable Dangerous Command Detection Ability

As an underlying atomic operation platform made for server OS, the verification of user command's validity and safety is of critical importance! The platform allows for regular expression detection rules for dangerous commands in various script languages, and provides a log for intercepted operations. Even when a command is made by a peripheral system via API, it'll be intercepted immediately, making the server more secure!

### Comprehensive Script Version Management

Cloud management of script versions, which is accordant with the modern concept of collaboration. Aided by the convenient platform and version management feature, BK-JOB users can share script resources, control the releasing status of various versions, and reduce loss if there's a security vulnerability.

### Arrange Jobs for All Scenarios

When an operation scenario requires multiple interconnected steps, clicking them one by one is too inefficient! Besides, it is inconvenient for later usage and maintenance.

The task management feature offers an optimal solution. Users can set up the necessary steps on the "Job Template" before making up an "Executable Plan" as required by the scenario; As such, job templates and its instances are set apart rather than welded together, which is also beneficial to later management and maintenance.

### The Authentic Cron Jobs

Linux's original crontab job feature is preserved, allowing the maintenance team to access them easily. The friendly monitoring feature allows you to easily control and assess the situation of scheduled tasks.

### Scalable File Source Management

When it comes to file distribution, we not only use remote server and local files as source of transmission, we also need to retrieve files from different file systems/services such as object storage, FTP, and Samba.

To meet the diversified demands, we have enabled the file source plug-in feature, which allows developers to develop plugins for the file management modules on the platform according to their own file system. As such, they can distribute files from various file systems.


BK-JOB provides quick execution, task arrangement, cron job, and a wide range of core services, meeting the demands of all environments:
- **Quick Execution**: Offers a temporary, versatile, quick access.
- **Task Arrangement**: Transforms repetitive operations into "jobs" using the arrangement feature to make them easy to use and manage.
- **Scheduled Execution**: Allows users to make up periodic or one-off execution plans according to the logical needs of their business.
- **Script Management**: Manage scripts in a cloud-based mode, which further facilitates job arrangement and the flexibility of peripheral system scheduling.
- **Account Management**: The execution account of that manages server OS, such as Linux root or Windows administrator, etc.
- **Notification**: Allows businesses to send notifications on the execution result for various task statuses.
- **File Source Management**: Enables file source plugins, allowing it to retrieve and transfer files from various file systems.
- **Operation Analysis**: Displays platform operation statistics, which allows the administrator to have an all-around insight into the operation of the platform.
- **Platform Management**: A versatile platform management tool whose features include, but not limited to, information modification, message channel settings, rules for detecting dangerous commands, feature limitation settings, public script management, background service status display, etc.

## Overview

- [Architecture Design](docs/overview/architecture.en.md)
- [Code Directory](docs/overview/code_framework.en.md)
- [Design Philosophy](docs/overview/design.en.md)

## Features

For more information, please check Blueking's official website [Platform Product Overview](https://bk.tencent.com/docs/document/6.0/125/5748)

## Getting started
- [Download and Compile](docs/overview/source_compile.en.md)
- [Installation Setup](docs/overview/installation.en.md)

## Support
1. [GitHub Community](https://github.com/Tencent/bk-job/discussions)

## BlueKing Community
- [BK-BCS](https://github.com/Tencent/bk-bcs): Blueking Container Service is a container-based basic service platform that provides management service to microservice businesses.
- [BK-CI](https://github.com/Tencent/bk-ci): Blueking Continuous Integration platform is a free, open source CI service, which allows developers to automatically create - test - release workflow, and continuously, efficiently deliver their high-quality products.
- [BK-CMDB](https://github.com/Tencent/bk-cmdb): BlueKing CMDB is an enterprise-level management platform designed for assets and applications.
- [BK-PaaS](https://github.com/Tencent/bk-PaaS): Blueking PaaS is an open development platform that allows developers to efficiently create, develop, set up, and manage SaaS apps.
- [BK-SOPS](https://github.com/Tencent/bk-sops): Blueking SOPS is a system that features workflow arrangement and execution using a graphical interface. It's a lightweight task scheduling and arrangement SaaS product of the Blueking system.

## Contributing
- For more information about bk-job's fork management, issue and PR rules, please check [Contributing](CONTRIBUTING.md).
- [Tencent Open Source Incentive Plan](https://opensource.tencent.com/contribution) encourages developers to engage and contribute in the community. Join us now!


## License
BK-JOB is based on the MIT agreement. For more information, please check [LICENSE](LICENSE.txt).

