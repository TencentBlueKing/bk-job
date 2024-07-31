FROM hub.bktencent.com/blueking/apigw-manager:3.0.4

COPY ./* /data/

RUN chmod +x /data/bin/sync-apigateway.sh
