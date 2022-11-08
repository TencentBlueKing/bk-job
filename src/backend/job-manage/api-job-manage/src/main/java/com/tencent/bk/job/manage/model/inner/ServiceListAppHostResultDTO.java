package com.tencent.bk.job.manage.model.inner;

import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Data;

import java.util.List;

/**
 * 主机查询结果
 */
@Data
public class ServiceListAppHostResultDTO {
    /**
     * 合法的主机（在当前业务下)
     */
    private List<HostDTO> validHosts;
    /**
     * 不存在的主机
     */
    private List<HostDTO> notExistHosts;
    /**
     * 在其他业务下的主机
     */
    private List<HostDTO> notInAppHosts;
}
