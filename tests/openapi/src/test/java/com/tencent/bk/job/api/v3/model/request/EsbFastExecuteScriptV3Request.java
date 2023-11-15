package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.EsbRollingConfigDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本执行请求
 */
@Getter
@Setter
public class EsbFastExecuteScriptV3Request extends EsbAppScopeReq {

    /**
     * 脚本执行任务名称
     */
    @JsonProperty("task_name")
    private String name;

    /**
     * "脚本内容，BASE64编码
     */
    @JsonProperty("script_content")
    private String content;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_alias")
    private String accountAlias;

    /**
     * 执行账号别名
     */
    @JsonProperty("account_id")
    private Long accountId;

    /**
     * 脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell
     */
    @JsonProperty("script_language")
    private Integer scriptLanguage;

    /**
     * 脚本参数， BASE64编码
     */
    @JsonProperty("script_param")
    private String scriptParam;

    /**
     * 脚本ID
     */
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 脚本版本ID
     */
    @JsonProperty("script_version_id")
    private Long scriptVersionId;

    /**
     * 是否敏感参数
     */
    @JsonProperty("is_param_sensitive")
    private Integer isParamSensitive = 0;

    /**
     * 执行超时时间,单位秒
     */
    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("target_server")
    private EsbServerV3DTO targetServer;

    @JsonProperty("target_servers")
    private List<EsbServerV3DTO> targetServers = new ArrayList<>();

    /**
     * 滚动配置
     */
    @JsonProperty("rolling_config")
    private EsbRollingConfigDTO rollingConfig;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    public void trimIps() {
        if (this.targetServer != null) {
            trimIps(this.targetServer.getIps());
        }
    }

    private void trimIps(List<HostDTO> ips) {
        if (ips != null && ips.size() > 0) {
            ips.forEach(host -> {
                host.setIp(host.getIp().trim());
            });
        }
    }

}
