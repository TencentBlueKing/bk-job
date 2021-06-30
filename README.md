![LOGO](docs/resource/img/bk-job.png)
---
[![license](https://img.shields.io/badge/license-mit-brightgreen.svg?style=flat)](https://github.com/Tencent/bk-job/blob/master/LICENSE.txt) [![Release Version](https://img.shields.io/github/v/release/Tencent/bk-job?include_prereleases)](https://github.com/Tencent/bk-job/releases)


> **重要提示**: `master` 分支在开发过程中可能处于 *不稳定或者不可用状态* 。
请通过[releases](https://github.com/tencent/bk-job/releases) 而非 `master` 去获取稳定的二进制文件。

蓝鲸作业平台(Job)是一套运维脚本管理系统，具备海量任务并发处理能力。除了支持脚本执行、文件分发、定时任务等一系列基础运维场景以外，还支持通过流程调度能力将零碎的单个任务组装成一个自动化作业流程；而每个作业都可做为一个原子节点，提供给上层或周边系统/平台使用，实现跨系统调度自动化。

## Benefits

### 安全可靠的高危命令检测能力

作为底层面向服务器OS的原子操作平台，对用户操作指令是否合规、安全的检测至关重要！作业平台支持通过正则表达式设置各种不同脚本语言的高危命令语句检测规则，并且提供被阻拦的操作日志；即便是周边系统通过 API 形式调度执行，也能够被实时检测拦截，让服务器操作更安全！

### 完善的脚本版本管理

云化脚本版本管理模式，贴合现代化开放协同的理念，协作者之间借助平台便捷的共享脚本资源；利用版本管理功能，您可以很好的控制版本的上/下线状态，并能够在出安全漏洞时快速禁用、及时止损！

### 作业编排，一切皆场景

当一个操作场景需要多个步骤串联执行时，如果手工一个个去点击执行，那么效率实在太低了！并且，也没办法很好的沉淀下来，方便后续持续使用和维护。

作业平台的作业管理功能很好的解决了这个问题，用户可以在「作业模板」中配置好相应的执行步骤，然后再根据需求场景衍生对应的「执行方案」；如此，即清晰的区分开作业模板和实例的关系，避免强耦合关系，也便于后续对使用场景的管理和维护。

### 原汁原味的 Cron 定时任务

保留了 Linux 原生的 Crontab 定时任务使用习惯，让运维同仁能够更平滑、快速的上手；更有贴心的监测功能助您发现及时掌握定时任务的动向和执行情况。

### 高扩展性的文件源管理能力

在文件分发的需求场景中，我们除了从远程服务器、本地文件作为传输源以外，还可能需要从对象存储、FTP、Samba等不同的文件系统/服务获取文件；

为了满足这种多元化的文件源对接诉求，我们开放了文件源插件的能力，支持开发者根据自己的文件系统类型开发插件对接作业平台的文件源管理模块，从而实现从不同文件系统分发的能力。


bk-job 提供了快速执行、任务编排、定时执行等核心服务，多重组合，满足企业不同场景的需求：
- **快速执行**：提供临时性且多变的快速一次性操作入口，用完即走
- **任务编排**：对于重复性的操作组合，可以通过编排功能将其沉淀为“作业”，方便管理和使用
- **定时执行**：支持用户按业务逻辑诉求设置周期性或一次性的定期执行计划
- **脚本管理**：将脚本以云化模式统一管理，更好的支持作业编排和周边系统调度的灵活度
- **账号管理**：管理服务器OS的执行账户，如Linux的 root，Windows的 administrator 等等
- **消息通知**：满足业务按管理需求设置任务不同状态的执行结果消息通知
- **文件源管理**：开放文件源对接插件能力，满足从不同文件系统类型拉取文件并传输的诉求
- **运营分析**：提供平台的运营统计数据展示，助力管理员更全方位的了解平台的运行情况
- **平台管理**：丰富的平台管理员工具，包括但不仅限于信息更改、消息渠道设置、高危语句检测规则、功能限制设置、公共脚本管理、后台服务状态展示等等

## Overview

- [架构设计](docs/overview/architecture.md)
- [代码目录](docs/overview/code_framework.md)
- [设计理念](docs/overview/design.md)

## Features

详情可见蓝鲸官网[作业平台产品白皮书](https://bk.tencent.com/docs/document/6.0/125/5748)

## Getting started
- [下载与编译](docs/overview/source_compile.md)
- [安装部署](docs/overview/installation.md)

## Support
1. [GitHub讨论区](https://github.com/Tencent/bk-job/discussions)

## BlueKing Community
- [BK-BCS](https://github.com/Tencent/bk-bcs)：蓝鲸容器管理平台是以容器技术为基础，为微服务业务提供编排管理的基础服务平台。
- [BK-BCS-SaaS](https://github.com/Tencent/bk-bcs-saas)：蓝鲸容器管理平台SaaS基于原生Kubernetes和Mesos自研的两种模式，提供给用户高度可扩展、灵活易用的容器产品服务。
- [BK-CI](https://github.com/Tencent/bk-ci)：蓝鲸持续集成平台是一个免费并开源的CI服务，让开发者可以自动化构建-测试-发布工作流，持续、快速、高质量地交付产品。
- [BK-CMDB](https://github.com/Tencent/bk-cmdb)：蓝鲸配置平台（蓝鲸CMDB）是一个面向资产及应用的企业级配置管理平台。
- [BK-PaaS](https://github.com/Tencent/bk-PaaS)：蓝鲸PaaS平台是一个开放式的开发平台，让开发者可以方便快捷地创建、开发、部署和管理SaaS应用。
- [BK-SOPS](https://github.com/Tencent/bk-sops)：蓝鲸标准运维（SOPS）是通过可视化的图形界面进行任务流程编排和执行的系统，是蓝鲸体系中一款轻量级的调度编排类SaaS产品。

## Contributing
- 关于 bk-job 分支管理、issue 以及 pr 规范，请阅读 [Contributing](CONTRIBUTING.md)
- [腾讯开源激励计划](https://opensource.tencent.com/contribution) 鼓励开发者的参与和贡献，期待你的加入


## License
BK-JOB 是基于 MIT 协议， 详细请参考 [LICENSE](LICENSE.txt)

