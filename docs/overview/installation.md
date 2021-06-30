# 安装部署

## 1. 部署目录说明

部署的目录遵循蓝鲸运营规范，这里举例以/data/bkee 作为主目录，用户可以自行更换，具体如下：

```
|- /data/bkee  # 蓝鲸根目录
  |- job      # job部署程序目录
  |- etc      # 蓝鲸配置文件总目录
    |- job    # job配置文件目录
```

具体说明下以下章节。

### 1.1 job部署目录

```
|- /data/bkee/job        # 程序主目录
  |- frontend            # 存放前端程序的静态资源目录
    |- index.html        # 前端首页文件
    |- __init__          # 初始化文件目录
    |- js                # 静态JS目录
    |- static            # 其他静态文件目录
  |- support-files       # 资源文件
  |- backend             # 存放后台微服务及文件源接入点程序
    |- job-manage        # job-manage微服务相关jar包与运维脚本，共有11个目录，不再一一列举
      |- job-manage.jar  # job-manage微服务的SpringBoot.jar
      |- bin  
        |- job-manage.sh # job-manage微服务的运维脚本
    |- job-xxx
```

### 1.2 job配置文件目录

```
|- /data/bkee/etc   # 蓝鲸配置文件总目录
  |- job 		    # job配置文件目录
    |- api.cert     # job调用GSE接口使用的证书文件
    |- application-manage.yml  # job-manage的基础配置，每个微服务各有1个基础配置yaml文件，如有增加微服务也放此处
    |- job-manage              # job-manage的扩展配置，每个微服务各有1个对应目录存放扩展配置文件，如有增加微服务须增加对应目录
      |- job-manage.properties # job-manage扩展配置properties文件
```

## 2. 基础环境部署

### 2.1 系统要求

- CentOS 7.X
- jdk: 1.8
- gradle: 6.3
- redis: 4.0.14
- mysql 5.7
- rabbitmq: 3.7.14  
- mongoDB: 4.2.2  
- nginx: 1.16.1  
- Consul: 1.0+

### 2.2 数据库初始化

将support-files/sql/* 目录下按文件序号顺序执行。


## 3 程序部署

### 3.1 support-files/template配置文件初始化

涉及到配置文件里面有双"_"下划线定义的变量需要根据实际数据（如MySQL账号密码等）做占位符号替换，完成替换后需将配置文件移动至正确的配置文件目录下，供配置中心微服务启动后读取。

### 3.2 后端微服务部署

- [后端服务部署](../install/backend.md)

### 3.3 前端部署

首先，将编译生成的所有前端静态文件放置于正确的前端目录下，可参考1.1节部署目录。
其次，对前端部署目录中的index.html文件做变量替换：
文件位置：/data/bkee/job/frontend/index.html
将其中的{{JOB_API_GATEWAY_URL}}替换为为Job后台API地址(通常为分配的独立API域名)。
前端部署准备工作至此完成，后续通过配置Nginx作为前端静态服务即可。

### 3.4 Nginx配置与部署
Job使用Nginx作为前端静态资源服务器并转发后端请求至job-gateway微服务，其配置可参考如下：
```shell script
# 前端静态资源服务server配置
server {
    listen                  80;
    server_name             {{JOB_FRONTEND_HOST}};

    gzip on;
    client_max_body_size 150M;
    root /data/bkee/job/frontend;
    index index.html;

    location / {
        add_header Cache-Control no-cache;
        expires 0;
        try_files $uri $uri/ @rewrites;
    }

    location @rewrites {
        rewrite ^(.+)$ /index.html last;
    }
}
# 后台API转发server配置
upstream job_gateway_servers {
   server {{JOB_GATEWAY_IP0}}:19802;
   server {{JOB_GATEWAY_IP1}}:19802;
}
server {
    listen                  80;
    server_name             {{JOB_API_HOST}};
    location / {
        proxy_pass          http://job_gateway_servers;
        proxy_set_header    Host                $host;
        proxy_set_header    X-Real-IP           $remote_addr;
        proxy_set_header    X-Forwarded-Proto   $scheme;
        proxy_set_header    X-Forwarded-For     $proxy_add_x_forwarded_for;
        proxy_set_header Host $http_host;
    }
    access_log /data/bkee/logs/nginx-access.log json_combined;
    client_max_body_size 2048M;
    error_log /data/bkee/logs/nginx-error.log;
    error_page 404 403 500 502 503 504 /job_error.html;
    location = /job_error.html {
    root /data/html;
    }
}
```
配置完成后reload nginx使其生效：
```shell script
nginx -s reload -c /path/to/nginx.conf
```

### 3.5 访问Job首页
通过{{JOB_FRONTEND_HOST}}访问Job首页，即可开始体验Job功能。
