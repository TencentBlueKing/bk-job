# Installation Setup

English | [简体中文](installation.md)

## 1. About Setup Directory

The directory should conform to Blueking rules of operation. We're taking /data/bkee as the primary directory. Users can feel free to change them. The details are shown as follows:

```
|- /data/bkee  # Blueking Root Directory
  |- job      # job Setup Program Directory
  |- etc      # Blueking Config File Catalog
    |- job      # job Config File Menu
```

Detailed explanation of the following chapters:

### 1.1 job Setup Directory

```
|- /data/bkee/job        # Home Directory
  |- frontend            # Static Resource Directory for Frontend Programs
    |- index.html        # Frontend Home Page File
    |- __init__          # Initialization File Directory
    |- js                # Static JS Directory
    |- static            # Other Static File Directory
  |- support-files       # Resource Files
  |- backend             # The program that stores backend microservices and file source access points
    |- job-manage        # job-manage Microservice Jar package and ops script. There are 11 directories in total, which have already been introduced above.
      |- job-manage.jar  # job-manage Microservice's SpringBoot.jar
      |- bin  
        |- job-manage.sh # job-manage Microservice's ops script
    |- job-xxx
```

### 1.2 job Config File Directory

```
|- /data/bkee/etc   # Blueking Config File Catalog
  |- job 		    # job Config File Menu
    |- job.env      # Specified environment variables when rendering job configuration files through template
    |- application-manage.yml  # job-manage's basic configuration. Each microservice has 1 yaml file. If there are new microservices, please put them here.
    |- job-manage              # job-manage's extended configuration. Each microservice has 1 corresponding directory for the extended configuration files. If there are new microservices, new directories should be added accordingly.
      |- job-manage.properties # job-manage Extended configuration properties file
```

## 2. Basic Environment Setup

### 2.1 System Requirement

- CentOS 7.X
- jdk: 1.8
- gradle: 6.3
- redis: 4.0.14
- mysql 5.7
- rabbitmq: 3.7.14  
- mongoDB: 4.2.2  
- nginx: 1.16.1  
- Consul: 1.0+

### 2.2 Database Initialization

Run the files under support-files/sql/* by the file sequence.


## 3 Program Setup

### 3.1 support-files/template Initialize configuration file

The underlined variables in the configuration file should be replaced with real data (e.g. MySQL account and password.) When the placement is made, the configuration file should be moved to the correct file directory to be read by the Configuration Center Microservice.

### 3.2 Backend Microservice Setup

- [Backend Service Setup] (../install/backend.md)

### 3.3 Frontend Setup

First, place all compiled static frontend files under the correct frontend directory. Refer to chapter 1.1 - Directory Setup.
Second, replace the variables of index.html in the frontend setup directory:
Location: /data/bkee/job/frontend/index.html
Change {{JOB_API_GATEWAY_URL}} into backend job API address (usually an independent API domain.)
And that's all the preparations for frontend setup. Later, you can set Nginx as frontend static service.

### 3.4 Nginx Configuration and Setup
Job uses Nginx as the frontend static resource server, and forwards backend requests to the job-gateway microservice. The configuration is listed as follows:
```shell script
# Server Configuration of Frontend Static Resources
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
# Backend API Forwards Server Configuration
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
When the configuration is completed, reload nginx to make the settings effective:
```shell script
nginx -s reload -c /path/to/nginx.conf
```

### 3.5 Access Job's Home Page
Access Job's home page via {{JOB_FRONTEND_HOST}} to use Job features.
