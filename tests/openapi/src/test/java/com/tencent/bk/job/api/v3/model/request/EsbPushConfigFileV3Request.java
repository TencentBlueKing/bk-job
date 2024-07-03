package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分发配置文件请求
 */
@Getter
@Setter
public class EsbPushConfigFileV3Request extends EsbAppScopeReq {

    /**
     * 用户自定义任务名称
     */
    @JsonProperty("task_name")
    private String name;


    /**
     * 目标路径
     */
    @JsonProperty("file_target_path")
    private String targetPath;

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
     * 目标服务器
     */
    @JsonProperty("target_server")
    private EsbServerV3DTO targetServer;

    @JsonProperty("file_list")
    private List<EsbConfigFileDTO> fileList;

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

    private void trimIps(List<EsbIpDTO> ips) {
        if (ips != null && ips.size() > 0) {
            ips.forEach(host -> {
                host.setIp(host.getIp().trim());
            });
        }
    }

    @Setter
    @Getter
    public static class EsbConfigFileDTO {

        @JsonProperty("file_name")
        private String fileName;
        /**
         * 文件内容Base64
         */
        private String content;
    }


}
