/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIpHostLocator;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WhiteIpHostValidator {

    private static final String FIELD_HOST_LIST = "host_list";
    private static final String REASON_HOST_IDENTITY = "whiteip.illegal.host.identity";
    private static final String REASON_HOST_NOT_UNIQUE = "whiteip.illegal.host.not.unique";
    private static final String REASON_HOST_ID = "whiteip.illegal.host.id";
    private static final String REASON_HOST_IPV4 = "whiteip.illegal.host.ipv4";
    private static final String REASON_CLOUD_AREA = "whiteip.illegal.cloud.area";
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    private final CurrentTenantHostDAO currentTenantHostDAO;
    private final IBizCmdbClient bizCmdbClient;
    private final MessageI18nService i18nService;

    @Autowired
    public WhiteIpHostValidator(CurrentTenantHostDAO currentTenantHostDAO,
                                IBizCmdbClient bizCmdbClient,
                                MessageI18nService i18nService) {
        this.currentTenantHostDAO = currentTenantHostDAO;
        this.bizCmdbClient = bizCmdbClient;
        this.i18nService = i18nService;
    }

    public List<HostIdWithMeta> resolveWhiteIpHosts(List<WhiteIpHostLocator> locators) {
        if (CollectionUtils.isEmpty(locators)) {
            return Collections.emptyList();
        }
        LinkedHashMap<Long, HostIdWithMeta> hosts = new LinkedHashMap<>();
        for (WhiteIpHostLocator locator : locators) {
            Long hostId = resolveOneWhiteIpHost(locator);
            hosts.putIfAbsent(hostId, new HostIdWithMeta(hostId, null));
        }
        return new ArrayList<>(hosts.values());
    }

    public Map<Long, ApplicationHostDTO> validateAndLoadHosts(List<Long> hostIdList) {
        if (CollectionUtils.isEmpty(hostIdList)) {
            return Collections.emptyMap();
        }
        // 从本地DB查询
        List<ApplicationHostDTO> hostDTOList = currentTenantHostDAO.listHostInfoByHostIds(hostIdList);
        Map<Long, ApplicationHostDTO> hostMap = new HashMap<>();
        hostDTOList.forEach(hostDTO -> hostMap.put(hostDTO.getHostId(), hostDTO));

        // 本地DB不存在的HostId从CMDB查询hostId
        List<Long> notInDbHostIdList = hostIdList.stream()
            .filter(hostId -> !hostMap.containsKey(hostId))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notInDbHostIdList)) {
            String tenantId = JobContextUtil.getTenantId();
            List<ApplicationHostDTO> cmdbHostList = bizCmdbClient.listHostsByHostIds(tenantId, notInDbHostIdList);
            if (CollectionUtils.isNotEmpty(cmdbHostList)) {
                cmdbHostList.forEach(hostDTO -> hostMap.put(hostDTO.getHostId(), hostDTO));
            }
            // CMDB中也查不到的IP需要记录
            notInDbHostIdList.removeIf(hostMap::containsKey);
            if (!notInDbHostIdList.isEmpty()) {
                log.warn("Cannot find hostIds in db/cmdb:{}", notInDbHostIdList);
                throw new InvalidParamException(
                    ErrorCode.HOST_ID_NOT_EXIST_IN_CMDB,
                    StringUtil.concatCollection(notInDbHostIdList)
                );
            }
        }
        return hostMap;
    }

    /**
     * 主机 ID 优先。只给管控区域和 IP 时，本地缓存与 CMDB 按 hostId 去重后必须是同一台主机。
     * 本地唯一命中不能代替 CMDB 校验。
     */
    private Long resolveOneWhiteIpHost(WhiteIpHostLocator locator) {
        if (locator == null) {
            throw illegalHostParam(REASON_HOST_IDENTITY);
        }
        if (locator.getHostId() != null) {
            if (locator.getHostId() < 1L) {
                throw illegalHostParam(REASON_HOST_ID);
            }
            return locator.getHostId();
        }
        if (locator.getCloudAreaId() == null || isBlank(locator.getIp())) {
            throw illegalHostParam(REASON_HOST_IDENTITY);
        }
        if (locator.getCloudAreaId() < 0L) {
            throw illegalHostParam(REASON_CLOUD_AREA);
        }
        String ip = locator.getIp().trim();
        if (!IPV4_PATTERN.matcher(ip).matches()) {
            throw illegalHostParam(REASON_HOST_IPV4);
        }
        return resolveHostIdByCloudIp(locator.getCloudAreaId(), ip);
    }

    private Long resolveHostIdByCloudIp(Long cloudAreaId, String ip) {
        List<String> cloudIps = Collections.singletonList(cloudAreaId + ":" + ip);
        List<ApplicationHostDTO> localHosts = currentTenantHostDAO.listHostInfoByCloudIps(cloudIps);
        List<ApplicationHostDTO> cmdbHosts = bizCmdbClient.listHostsByCloudIps(
            JobContextUtil.getTenantId(),
            cloudIps
        );
        Set<Long> localHostIds = distinctHostIds(localHosts);
        Set<Long> cmdbHostIds = distinctHostIds(cmdbHosts);
        Set<Long> unionHostIds = new LinkedHashSet<>();
        unionHostIds.addAll(localHostIds);
        unionHostIds.addAll(cmdbHostIds);
        if (cmdbHostIds.size() != 1 || unionHostIds.size() != 1) {
            throw illegalHostParam(REASON_HOST_NOT_UNIQUE);
        }
        return cmdbHostIds.iterator().next();
    }

    private Set<Long> distinctHostIds(List<ApplicationHostDTO> hosts) {
        Set<Long> hostIds = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(hosts)) {
            return hostIds;
        }
        for (ApplicationHostDTO host : hosts) {
            if (host != null && host.getHostId() != null) {
                hostIds.add(host.getHostId());
            }
        }
        return hostIds;
    }

    private InvalidParamException illegalHostParam(String reasonKey) {
        return new InvalidParamException(
            ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
            new Object[]{FIELD_HOST_LIST, i18nService.getI18n(reasonKey)}
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
