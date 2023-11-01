package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分发配置文件请求
 */
@Getter
@Setter
public class EsbPushConfigFileV3Request extends BaseEsbReq {
    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

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

    @Setter
    @Getter
    public static class EsbConfigFileDTO {
        public EsbConfigFileDTO(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
        }

        @JsonProperty("file_name")
        private String fileName;
        /**
         * 文件内容Base64
         */
        private String content;
    }

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
