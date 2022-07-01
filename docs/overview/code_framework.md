# 蓝鲸作业平台(BK-JOB)的代码结构

[English](code_framework.en.md) | 简体中文

```shell script
|- bk-job
  |- docs
  |- scripts
  |- src
    |- backend
    |- frontend
  |- support-files
  |- versionLogs
```
## 工程源码(src)
工程混合了vue/java/shell等几种语言，按功能划分为前端、后端、支撑文件、版本日志、运维脚本等子目录。
### 前端代码(frontend)
```shell script
|- bk-job/src
  |- frontend/src
    |- lib   # 依赖的第三方库源码
    |- src
      |- common     # 公共模块
      |- components # 公共交互组件
      |- css        # 全局css
      |- domain   
        |- model            # 业务模型
        |- service          # 后端api服务
        |- source           # 后端api配置
        |- variable-object  # 服务于前端逻辑的变量对象
      |- i18n       # 全局公用的国际化
      |- images     # 静态资源图片
      |- router     # 路由配置
      |- store      # 状态管理
      |- utils      # 公共方法
      |- views      # 系统模块
        |- account-manage          # 账号管理
          |- index.vue                # 模块入口
          |- local.js                 # 模块国际化
          |- routes.js                # 模块路由表配置
        |- cron-job                   # 定时任务
        |- dangerous-rule-manage   # 高危语句配置
        |- dashboard               # 运营分析
        |- detect-records          # 高危语句拦截记录
        |- executive-history       # 任务执行历史
        |- fast-execution          # 快速执行
        |- file-manage             # 文件管理
        |- home                    # 业务概览
        |- notify-manage           # 消息通知
        |- plan-manage             # 执行方案管理
        |- public-script-manage    # 公共脚本管理
        |- script-manage           # 脚本管理
        |- script-template         # 脚本模板
        |- service-state           # 服务状态
        |- setting                 # 全局设置
        |- task-manage             # 作业管理
        |- ticket-manage           # 凭证管理
        |- white-ip                # IP白名单
        |- 404.vue                 # 路由404页面
        |- business-permission.vue # 无业务权限页面
        |- index.vue               # 系统模块入口文件
      |- App.vue            # 页面渲染入口
      |- iframe-app.vue     # 通过iframe访问时页面渲染入口
      |- layout-new.vue     # 导航布局
      |- main.js            # 前端入口文件
    |- index-dev.html    # dev本地开发服务入口
    |- index.html        # build服务入口
    |- webpack.config.js # webpack配置
```

### 后端微服务代码(backend)
```shell script
|- bk-job/src
  |- backend
    |- buildSrc     # 自定义Gradle Task，实现打包过程中的特殊操作
    |- commons      # 公共模块
      |- cmdb-sdk   # 对接蓝鲸配置平台(CMDB)公共代码
      |- cmdb-sdk-ext  # 在CMDB-SDK基础上加入Redis限流器
      |- common        # 通用常量、异常等公共代码
      |- common-i18n   # 国际化
      |- common-iam    # 对接蓝鲸权限中心
      |- common-redis  # Redis操作
      |- common-security   # 安全控制
      |- common-spring-ext # 通用Spring扩展(自定义listener、processor等)
      |- common-statistics # 通用统计相关
      |- common-utils  # 通用工具类
      |- common-web    # Web工具(filter、interceptor等)
      |- esb-sdk    # 对接蓝鲸ESB
      |- gse-sdk    # 对接蓝鲸GSE
      |- paas-sdk   # 对接蓝鲸PaaS平台
    |- job-analysis # 统计分析微服务
      |- api-job-analysis     # API定义抽象层
      |- boot-job-analysis    # 启动类及相关配置
      |- model-job-analysis   # JOOQ自动生成的表结构模型类存放目录
      |- service-job-analysis # 业务逻辑实现层
    |- job-backup   # 备份管理微服务
    |- job-config   # 配置中心微服务
    |- job-crontab  # 定时任务微服务
    |- job-execute  # 作业执行微服务
    |- job-file-gateway     # 文件网关微服务
    |- job-file-worker      # 文件源接入点实现
    |- job-file-worker-sdk  # 文件源接入点公共逻辑SDK
    |- job-gateway  # 微服务网关
    |- job-logsvr   # 日志管理微服务
    |- job-manage   # 作业管理微服务
    |- job-ticket   # 凭据管理微服务
    |- upgrader     # 版本间升级辅助工具
```

### 支撑文件(support-files)
```shell script
|- bk-job/support-files
  |- bkiam     # 存放记录权限模型变更的迁移文件
  |- sql       # 存放记录MySQL数据库表结构变更的迁移文件
  |- templates # 存放各微服务部署时需要用环境变量替换的配置模板文件
```

### 版本日志(versionLogs)
存放中英文两种语言的版本日志与对应的前端资源生成脚本。

### 运维脚本(scripts)
存放后台各微服务的启动/停止/重启等操作的运维脚本。
