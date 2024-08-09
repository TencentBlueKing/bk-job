job采用chart + 自定义镜像方式同步网关.

接口文档与映射关系，主要包括以下几部分：
```
v3
├── definition.yaml         # 维护网关、环境、资源文档路径、主动授权、发布等配置
├── resources.yaml          # job与API Gateway的接口映射关系
├── bin
│   └── sync-apigateway.sh  # 同步脚本，用于注册网关
└── apidocs                 
    ├── zh                  # 中文文档目录
    └── en                  # 英文文档目录
```

[接入文档](https://github.com/TencentBlueKing/bkpaas-python-sdk/tree/master/sdks/apigw-manager)

[同步方式](https://github.com/TencentBlueKing/bkpaas-python-sdk/blob/master/sdks/apigw-manager/docs/sync-apigateway-with-docker.md)

[API资源文档规范](https://bk.tencent.com/docs/markdown/ZH/APIGateway/1.10/UserGuide/apigateway/reference/api-doc-specification.md)

