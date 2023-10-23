package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.gse.service.BizHostInfoQueryService;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.manage.api.inner.ServiceHostResource;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostsReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("jobExecuteBizHostInfoQueryService")
public class BizHostInfoQueryServiceImpl implements BizHostInfoQueryService {

    private final ServiceHostResource hostResource;

    @Autowired
    public BizHostInfoQueryServiceImpl(ServiceHostResource hostResource) {
        this.hostResource = hostResource;
    }

    @Override
    public Map<Long, Long> queryBizIdsByHostId(Collection<Long> hostIds) {
        List<ServiceHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ServiceHostDTO::getHostId, ServiceHostDTO::getBizId);
    }

    @Override
    public Map<Long, String> queryAgentIdsByHostId(Collection<Long> hostIds) {
        List<ServiceHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ServiceHostDTO::getHostId, ServiceHostDTO::getAgentId);
    }

    @Override
    public Map<Long, String> queryCloudIpsByHostId(Collection<Long> hostIds) {
        List<ServiceHostDTO> hostList = queryHosts(hostIds);
        return CollectionUtil.convertToMap(hostList, ServiceHostDTO::getHostId, ServiceHostDTO::getCloudIp);
    }

    private List<ServiceHostDTO> queryHosts(Collection<Long> hostIds) {
        List<HostDTO> hostDTOList = hostIds.stream().map(HostDTO::fromHostId).collect(Collectors.toList());
        return hostResource.batchGetHosts(new ServiceBatchGetHostsReq(hostDTOList)).getData();
    }
}
