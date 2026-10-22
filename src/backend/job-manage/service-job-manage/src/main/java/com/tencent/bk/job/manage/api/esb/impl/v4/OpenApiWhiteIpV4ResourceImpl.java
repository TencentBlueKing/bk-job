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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiWhiteIpV4Resource;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIpHostLocator;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateWhiteIpRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4ListWhiteIpRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4WhiteIpHostReq;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4WhiteIpHostDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4WhiteIpPageDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4WhiteIpRecordDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4WhiteIpScopeDTO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPHostVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import com.tencent.bk.job.manage.service.WhiteIPService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class OpenApiWhiteIpV4ResourceImpl implements OpenApiWhiteIpV4Resource {

    private final WhiteIPService whiteIPService;
    private final NoResourceScopeAuthService noResourceScopeAuthService;
    private final ActionScopeDAO actionScopeDAO;
    private final OpenApiWhiteIpV4Validator validator;

    @Autowired
    public OpenApiWhiteIpV4ResourceImpl(WhiteIPService whiteIPService,
                                        NoResourceScopeAuthService noResourceScopeAuthService,
                                        ActionScopeDAO actionScopeDAO,
                                        OpenApiWhiteIpV4Validator validator) {
        this.whiteIPService = whiteIPService;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
        this.actionScopeDAO = actionScopeDAO;
        this.validator = validator;
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_WHITELIST)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_create_white_ip"})
    public EsbV4Response<OpenApiV4WhiteIpRecordDTO> createWhiteIp(
        String username,
        String appCode,
        @AuditRequestBody V4CreateWhiteIpRequest request) {
        AuthResult authResult = noResourceScopeAuthService.authCreateWhiteList(JobContextUtil.getUser());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        List<Long> actionScopeIds = validator.requireActionScopeIds(request.getActionScopeList());
        List<ResourceScope> scopes = null;
        if (!request.isAllScope()) {
            scopes = validator.requireResourceScopes(request.getScopeList());
        }
        List<HostIdWithMeta> hosts = whiteIPService.resolveWhiteIpHosts(toLocators(request.getHostList()));

        WhiteIPRecordCreateUpdateReq createReq = new WhiteIPRecordCreateUpdateReq();
        createReq.setAllScope(request.isAllScope());
        createReq.setRemark(request.getRemark());
        createReq.setHostList(hosts);
        createReq.setActionScopeIdList(actionScopeIds);
        createReq.setScopeList(scopes);
        WhiteIPRecordDTO record = whiteIPService.createWhiteIP(username, createReq);
        return EsbV4Response.success(toRecord(record));
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_WHITELIST)
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_WHITELIST,
        content = EventContentConstants.VIEW_WHITE_LIST
    )
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_list_white_ip"})
    public EsbV4Response<OpenApiV4WhiteIpPageDTO> listWhiteIp(
        String username,
        String appCode,
        @AuditRequestBody V4ListWhiteIpRequest request) {
        AuthResult authResult = noResourceScopeAuthService.authManageWhiteList(JobContextUtil.getUser());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        int start = validator.normalizeStart(request.getStart());
        int length = validator.normalizeLength(request.getLength());
        String daoOrderField = validator.normalizeOrderField(request.getOrderField());
        Integer order = validator.normalizeOrder(daoOrderField, request.getOrder());
        String appIdStr = validator.toAppIdStr(request.getScopeList());
        String actionScopeIdStr = joinIds(validator.optionalActionScopeIds(request.getActionScopeList()));

        PageData<WhiteIPRecordVO> pageData = whiteIPService.listWhiteIPRecord(
            username,
            blankToNull(request.getIp()),
            appIdStr,
            null,
            actionScopeIdStr,
            request.getCreator(),
            request.getLastModifier(),
            start,
            length,
            daoOrderField,
            order
        );
        OpenApiV4WhiteIpPageDTO page = new OpenApiV4WhiteIpPageDTO();
        page.setStart(start);
        page.setLength(length);
        page.setTotal(pageData.getTotal() == null ? 0L : pageData.getTotal());
        page.setData(toRecords(pageData.getData()));
        return EsbV4Response.success(page);
    }

    private List<WhiteIpHostLocator> toLocators(List<V4WhiteIpHostReq> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return Collections.emptyList();
        }
        List<WhiteIpHostLocator> locators = new ArrayList<>();
        for (V4WhiteIpHostReq host : hostList) {
            if (host == null) {
                locators.add(new WhiteIpHostLocator());
                continue;
            }
            locators.add(new WhiteIpHostLocator(host.getHostId(), host.getCloudAreaId(), host.getIp()));
        }
        return locators;
    }

    private String joinIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<OpenApiV4WhiteIpRecordDTO> toRecords(List<WhiteIPRecordVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<OpenApiV4WhiteIpRecordDTO> result = new ArrayList<>();
        for (WhiteIPRecordVO record : records) {
            result.add(toRecord(record));
        }
        return result;
    }

    private OpenApiV4WhiteIpRecordDTO toRecord(WhiteIPRecordDTO record) {
        OpenApiV4WhiteIpRecordDTO dto = new OpenApiV4WhiteIpRecordDTO();
        dto.setId(record.getId());
        dto.setRemark(record.getRemark());
        dto.setCreator(record.getCreator());
        dto.setCreateTime(record.getCreateTime());
        dto.setLastModifier(record.getLastModifier());
        dto.setLastModifyTime(record.getLastModifyTime());
        List<OpenApiV4WhiteIpHostDTO> hosts = toHostsFromDto(record.getIpList());
        dto.setHostList(hosts);
        if (CollectionUtils.isNotEmpty(record.getIpList())) {
            dto.setCloudAreaId(record.getIpList().get(0).getCloudAreaId());
        }
        boolean allScope = CollectionUtils.isNotEmpty(record.getAppIdList())
            && record.getAppIdList().contains(JobConstants.PUBLIC_APP_ID);
        dto.setAllScope(allScope);
        dto.setScopeList(allScope ? Collections.emptyList() : toScopesFromApps(record.getAppList()));
        dto.setActionScopeList(toActionScopeCodes(extractActionScopeIds(record.getActionScopeList())));
        return dto;
    }

    private OpenApiV4WhiteIpRecordDTO toRecord(WhiteIPRecordVO record) {
        OpenApiV4WhiteIpRecordDTO dto = new OpenApiV4WhiteIpRecordDTO();
        dto.setId(record.getId());
        dto.setCloudAreaId(record.getCloudAreaId());
        dto.setRemark(record.getRemark());
        dto.setCreator(record.getCreator());
        dto.setCreateTime(record.getCreateTime());
        dto.setLastModifier(record.getLastModifier());
        dto.setLastModifyTime(record.getLastModifyTime());
        dto.setHostList(toHostsFromVo(record.getHostList()));
        dto.setAllScope(record.isAllScope());
        dto.setScopeList(record.isAllScope() ? Collections.emptyList() : toScopesFromVo(record.getScopeList()));
        dto.setActionScopeList(toActionScopeCodes(extractActionScopeIdsFromVo(record.getActionScopeList())));
        return dto;
    }

    private List<OpenApiV4WhiteIpHostDTO> toHostsFromDto(List<WhiteIPIPDTO> ipList) {
        if (CollectionUtils.isEmpty(ipList)) {
            return Collections.emptyList();
        }
        List<OpenApiV4WhiteIpHostDTO> hosts = new ArrayList<>();
        for (WhiteIPIPDTO ip : ipList) {
            hosts.add(toHost(ip.getHostId(), ip.getCloudAreaId(), ip.getIp(), ip.getIpv6()));
        }
        return hosts;
    }

    private List<OpenApiV4WhiteIpHostDTO> toHostsFromVo(List<WhiteIPHostVO> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return Collections.emptyList();
        }
        List<OpenApiV4WhiteIpHostDTO> hosts = new ArrayList<>();
        for (WhiteIPHostVO host : hostList) {
            hosts.add(toHost(host.getHostId(), host.getCloudAreaId(), host.getIp(), host.getIpv6()));
        }
        return hosts;
    }

    private OpenApiV4WhiteIpHostDTO toHost(Long hostId, Long cloudAreaId, String ip, String ipv6) {
        OpenApiV4WhiteIpHostDTO host = new OpenApiV4WhiteIpHostDTO();
        host.setHostId(hostId);
        host.setCloudAreaId(cloudAreaId);
        if (!isBlank(ip)) {
            host.setIp(ip);
        }
        if (!isBlank(ipv6)) {
            host.setIpv6(ipv6);
        }
        return host;
    }

    private List<OpenApiV4WhiteIpScopeDTO> toScopesFromApps(List<ApplicationDTO> appList) {
        if (CollectionUtils.isEmpty(appList)) {
            return Collections.emptyList();
        }
        List<OpenApiV4WhiteIpScopeDTO> scopes = new ArrayList<>();
        for (ApplicationDTO app : appList) {
            if (app == null) {
                continue;
            }
            OpenApiV4WhiteIpScopeDTO scope = new OpenApiV4WhiteIpScopeDTO();
            scope.setName(app.getName());
            if (app.getScope() != null && app.getScope().getType() != null) {
                scope.setScopeType(app.getScope().getType().getValue());
                scope.setScopeId(app.getScope().getId());
            }
            scopes.add(scope);
        }
        return scopes;
    }

    private List<OpenApiV4WhiteIpScopeDTO> toScopesFromVo(List<ScopeVO> scopeList) {
        if (CollectionUtils.isEmpty(scopeList)) {
            return Collections.emptyList();
        }
        List<OpenApiV4WhiteIpScopeDTO> scopes = new ArrayList<>();
        for (ScopeVO scope : scopeList) {
            if (scope == null) {
                continue;
            }
            OpenApiV4WhiteIpScopeDTO dto = new OpenApiV4WhiteIpScopeDTO();
            dto.setName(scope.getName());
            if (!isBlank(scope.getScopeType())) {
                dto.setScopeType(scope.getScopeType());
            }
            if (!isBlank(scope.getScopeId())) {
                dto.setScopeId(scope.getScopeId());
            }
            scopes.add(dto);
        }
        return scopes;
    }

    private List<Long> extractActionScopeIds(List<WhiteIPActionScopeDTO> actionScopes) {
        if (CollectionUtils.isEmpty(actionScopes)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (WhiteIPActionScopeDTO actionScope : actionScopes) {
            if (actionScope == null || actionScope.getActionScopeId() == null) {
                log.error("white ip action scope id is missing on create result");
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            ids.add(actionScope.getActionScopeId());
        }
        return ids;
    }

    private List<Long> extractActionScopeIdsFromVo(List<ActionScopeVO> actionScopes) {
        if (CollectionUtils.isEmpty(actionScopes)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (ActionScopeVO actionScope : actionScopes) {
            if (actionScope == null || actionScope.getId() == null) {
                log.error("white ip action scope id is missing on list result");
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            ids.add(actionScope.getId());
        }
        return ids;
    }

    private List<String> toActionScopeCodes(List<Long> actionScopeIds) {
        if (CollectionUtils.isEmpty(actionScopeIds)) {
            return Collections.emptyList();
        }
        List<ActionScopeDTO> actionScopes = actionScopeDAO.getActionScopeByIds(actionScopeIds);
        Map<Long, String> codeById = new HashMap<>();
        if (actionScopes != null) {
            for (ActionScopeDTO actionScope : actionScopes) {
                if (actionScope != null && actionScope.getId() != null && !isBlank(actionScope.getCode())) {
                    codeById.put(actionScope.getId(), actionScope.getCode());
                }
            }
        }
        List<String> codes = new ArrayList<>();
        for (Long actionScopeId : actionScopeIds) {
            String code = codeById.get(actionScopeId);
            if (code == null) {
                log.error("cannot find action scope code by id {}, please check init data", actionScopeId);
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            if (!codes.contains(code)) {
                codes.add(code);
            }
        }
        return codes;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
