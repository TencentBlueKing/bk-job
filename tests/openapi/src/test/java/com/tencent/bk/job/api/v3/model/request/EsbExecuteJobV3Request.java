package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.BaseEsbReq;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.EsbGlobalVarV3DTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 作业执行请求
 */
@Getter
@Setter
public class EsbExecuteJobV3Request extends BaseEsbReq {
    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 执行方案 ID
     */
    @JsonProperty("job_plan_id")
    private Long taskId;

    @JsonProperty("global_var_list")
    private List<EsbGlobalVarV3DTO> globalVars;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    public void trimIps() {
        if (globalVars != null && globalVars.size() > 0) {
            globalVars.forEach(globalVar -> {
                if (globalVar.getServer() != null) {
                    trimIps(globalVar.getServer().getIps());
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
