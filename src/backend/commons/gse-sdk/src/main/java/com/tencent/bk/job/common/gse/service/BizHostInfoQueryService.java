package com.tencent.bk.job.common.gse.service;

import java.util.Collection;
import java.util.Map;

public interface BizHostInfoQueryService {

    /**
     * 根据主机ID批量查询CMDB业务ID
     *
     * @param hostIds 主机ID集合
     * @return Map<主机ID, CMDB业务ID>
     */
    Map<Long, Long> queryBizIdsByHostId(Collection<Long> hostIds);

    /**
     * 根据主机ID批量查询AgentID
     *
     * @param hostIds 主机ID集合
     * @return Map<主机ID, AgentID>
     */
    Map<Long, String> queryAgentIdsByHostId(Collection<Long> hostIds);

    /**
     * 根据主机ID批量查询云区域IP
     *
     * @param hostIds 主机ID集合
     * @return Map<主机ID, 云区域IP>
     */
    Map<Long, String> queryCloudIpsByHostId(Collection<Long> hostIds);

}
