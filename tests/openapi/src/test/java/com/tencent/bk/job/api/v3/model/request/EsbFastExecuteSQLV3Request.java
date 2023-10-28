package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * SQL执行请求
 */
@Getter
@Setter
public class EsbFastExecuteSQLV3Request extends BaseEsbReq {
    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

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
     * 执行账号/别名
     */
    @JsonProperty("db_account_id")
    private Long dbAccountId;

    /**
     * 脚本版本ID
     */
    @JsonProperty("script_version_id")
    private Long scriptVersionId;

    /**
     * 脚本ID
     */
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 执行超时时间,单位秒
     */
    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("target_server")
    private EsbServerV3DTO targetServer;

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
