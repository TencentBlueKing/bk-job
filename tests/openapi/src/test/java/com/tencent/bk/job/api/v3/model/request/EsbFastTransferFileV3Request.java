package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.EsbFileSourceV3DTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分发文件请求报文
 */
@Getter
@Setter
public class EsbFastTransferFileV3Request extends EsbAppScopeReq {

    /**
     * 文件分发任务名称
     */
    @JsonProperty("task_name")
    private String name;
    /**
     * 源文件
     */
    @JsonProperty("file_source_list")
    private List<EsbFileSourceV3DTO> fileSources;

    /**
     * 目标路径
     */
    @JsonProperty("file_target_path")
    private String targetPath;

    /**
     * 目标文件/目录名
     */
    @JsonProperty("file_target_name")
    private String targetName;

    /**
     * 目标服务器账户别名
     */
    @JsonProperty("account_alias")
    private String accountAlias;

    /**
     * 目标服务器账号ID
     */
    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("target_server")
    private EsbServerV3DTO targetServer;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    /**
     * 下载限速，单位MB
     */
    @JsonProperty("download_speed_limit")
    private Integer downloadSpeedLimit;

    /**
     * 上传限速，单位MB
     */
    @JsonProperty("upload_speed_limit")
    private Integer uploadSpeedLimit;

    /**
     * 超时时间，单位秒
     */
    @JsonProperty("timeout")
    private Integer timeout;

    /**
     * 传输模式。1-严谨模式，2-强制模式，3-安全模式
     */
    private Integer transferMode;

    public void trimIps() {
        if (this.targetServer != null) {
            trimIps(this.targetServer.getIps());
        }
        if (this.fileSources != null && this.fileSources.size() > 0) {
            this.fileSources.forEach(fileSource -> {
                if (fileSource.getServer() != null) {
                    trimIps(fileSource.getServer().getIps());
                }
            });
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


