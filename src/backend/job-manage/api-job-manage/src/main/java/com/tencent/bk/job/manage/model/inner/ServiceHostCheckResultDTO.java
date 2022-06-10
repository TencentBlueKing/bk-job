package com.tencent.bk.job.manage.model.inner;

import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 主机检查结果
 */
@Data
public class ServiceHostCheckResultDTO {
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
    /**
     * 主机白名单
     */
    private Map<HostDTO, List<String>> whiteHosts;

}
