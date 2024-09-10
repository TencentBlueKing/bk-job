#!/bin/bash
set -e

# 加载 apigw-manager 原始镜像中的通用函数
source /apigw-manager/bin/functions.sh

# 待同步网关名，需修改为实际网关名；
# - 如在下面指令的参数中，指定了参数 --gateway-name=${gateway_name}，则使用该参数指定的网关名
# - 如在下面指令的参数中，未指定参数 --gateway-name，则使用 Django settings BK_APIGW_NAME
gateway_name=$BK_APIGW_NAME
# 自动发布资源，create_version_and_release_apigw命令中指定参数 --no-pub只生成版本不发布资源，不指定生成版本且发布资源
gateway_auto_publish=$BK_APIGW_AUTO_PUBLISH

title "do something before migrate"
# 网关维护人员解析成array格式
BK_APIGW_MAINTAINERS_ARRAY='["'${BK_APIGW_MAINTAINERS//,/\",\"}'"]'
export BK_APIGW_MAINTAINERS="$BK_APIGW_MAINTAINERS_ARRAY"

# 待同步网关、资源定义文件
definition_file="/data/definition.yaml"
resources_file="/data/resources.yaml"

title "begin to db migrate"
call_command_or_warning migrate apigw

title "syncing apigateway"
call_definition_command_or_exit sync_apigw_config "${definition_file}" --gateway-name=${gateway_name}
call_definition_command_or_exit sync_apigw_stage "${definition_file}" --gateway-name=${gateway_name}
call_definition_command_or_exit sync_apigw_resources "${resources_file}" --gateway-name=${gateway_name} --delete
call_definition_command_or_exit sync_resource_docs_by_archive "${definition_file}" --gateway-name=${gateway_name} --safe-mode
call_definition_command_or_exit grant_apigw_permissions "${definition_file}" --gateway-name=${gateway_name}

title "fetch apigateway public key"
apigw-manager.sh fetch_apigw_public_key --gateway-name=${gateway_name} --print > "apigateway.pub"

title "releasing"
if [ "$gateway_auto_publish" = "true" ]; then
  echo "Generate versions and automatically publish resources to blueking api gateway"
  call_definition_command_or_exit create_version_and_release_apigw "${definition_file}" --gateway-name=${gateway_name}
else
  echo "Only generate version, manually publish after confirmation on the blueking api gateway management page"
  call_definition_command_or_exit create_version_and_release_apigw "${definition_file}" --gateway-name=${gateway_name} --no-pub
fi

title "done"
