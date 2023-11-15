package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.gse.service.BizHostInfoQueryService;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.manage.service.host.HostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("jobManageBizHostInfoQueryService")
public class BizHostInfoQueryServiceImpl implements BizHostInfoQueryService {

    private final HostService hostService;

    @Autowired
    public BizHostInfoQueryServiceImpl(HostService hostService) {
        this.hostService = hostService;
    }

    @Override
    public Map<Long, Long> queryBizIdsByHostId(Collection<Long> hostIds) {
        List<ApplicationHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ApplicationHostDTO::getHostId, ApplicationHostDTO::getBizId);
    }

    @Override
    public Map<Long, String> queryAgentIdsByHostId(Collection<Long> hostIds) {
        List<ApplicationHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ApplicationHostDTO::getHostId, ApplicationHostDTO::getAgentId);
    }

    @Override
    public Map<Long, String> queryCloudIpsByHostId(Collection<Long> hostIds) {
        List<ApplicationHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ApplicationHostDTO::getHostId, ApplicationHostDTO::getCloudIp);
    }

    private List<ApplicationHostDTO> queryHosts(Collection<Long> hostIds) {
        Set<HostDTO> hostDTOSet = hostIds.stream().map(HostDTO::fromHostId).collect(Collectors.toSet());
        return hostService.listHosts(hostDTOSet);
    }

}
