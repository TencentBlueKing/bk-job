job
===

## 使用

### 本地开发

- 1，执行命令（本地启动http服务）

``` bash
npm run dev
```
- 需要在 /frontend 目录下面创建 .env.development 文件
```
// 配置 api 域名
AJAX_URL_PREFIX = "http://api.xxx.yyy.com"
```

- 2，执行命令（本地启动https服务）

``` bash
npm run dev-external
```
- 需要在 /frontend 目录下面创建 .env.external 文件
```
// 配置 api 域名
AJAX_URL_PREFIX = "https://api.xxx.yyy.com"
```


### 生产环境构建

``` bash

npm run build

```

