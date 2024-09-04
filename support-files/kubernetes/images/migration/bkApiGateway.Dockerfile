FROM mirrors.tencent.com/bk-apigateway/apigw-manager:3.1.1-alpha

COPY bin /data/bin
COPY apidocs /data/apidocs
COPY definition.yaml /data/definition.yaml
COPY resources.yaml /data/resources.yaml

RUN chmod +x /data/bin/sync-bkapigateway.sh
