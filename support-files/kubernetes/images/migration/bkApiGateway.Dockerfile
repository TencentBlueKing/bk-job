FROM hub.bktencent.com/blueking/apigw-manager:3.0.4

COPY bin /data/bin
COPY apidocs /data/apidocs
COPY definition.yaml /data/definition.yaml
COPY resources.yaml /data/resources.yaml

RUN chmod +x /data/bin/sync-bkapigateway.sh
