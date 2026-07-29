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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiScopeHostV4Resource;
import com.tencent.bk.job.manage.model.esb.v4.req.V4GetBizHostTopoTreeRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4SearchScopeHostRequest;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4HostTopoNodeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4ScopeHostDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4SearchScopeHostResult;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.ScopeTopoHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class OpenApiScopeHostV4ResourceImpl implements OpenApiScopeHostV4Resource {

    private final AppScopeMappingService appScopeMappingService;
    private final ScopeTopoHostService scopeTopoHostService;
    private final ScopeHostService scopeHostService;
    private final HostDetailService hostDetailService;

    @Autowired
    public OpenApiScopeHostV4ResourceImpl(AppScopeMappingService appScopeMappingService,
                                          ScopeTopoHostService scopeTopoHostService,
                                          ScopeHostService scopeHostService,
                                          HostDetailService hostDetailService) {
        this.appScopeMappingService = appScopeMappingService;
        this.scopeTopoHostService = scopeTopoHostService;
        this.scopeHostService = scopeHostService;
        this.hostDetailService = hostDetailService;
    }

    @Override
    @AuditEntry(actionId = ActionId.ACCESS_BUSINESS)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_biz_host_topo_tree"})
    public EsbV4Response<V4HostTopoNodeDTO> getBizHostTopoTree(String username,
                                                               String appCode,
                                                               V4GetBizHostTopoTreeRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        AppResourceScope appResourceScope = request.getAppResourceScope();
        // 该接口仅支持业务(biz)，业务集/租户集属于客户端传入不受支持的资源范围类型，
        // 按参数类错误(4xx)拒绝，而非服务端内部错误(500)。
        assertOnlyBizSupported(appResourceScope);

        CcTopologyNodeVO topologyTree =
            scopeTopoHostService.listAppTopologyHostCountTree(username, appResourceScope);
        return EsbV4Response.success(OpenApiV4ScopeHostConverter.toHostTopoNodeDTO(topologyTree));
    }

    @Override
    @AuditEntry(actionId = ActionId.ACCESS_BUSINESS)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_search_scope_host"})
    public EsbV4Response<V4SearchScopeHostResult> searchScopeHost(String username,
                                                                  String appCode,
                                                                  V4SearchScopeHostRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        AppResourceScope appResourceScope = request.getAppResourceScope();

        int offset = request.getOffset() == null ? 0 : request.getOffset();
        int length = request.getLength() == null ? 10 : request.getLength();

        List<BizTopoNode> nodeList = OpenApiV4ScopeHostConverter.toBizTopoNodeList(request.getTopoNodeList());
        // 业务(biz)作用域下未指定拓扑节点时，语义等价于查询整个业务，默认以业务根节点检索，
        // 避免底层按空模块集合过滤命中 0 台主机（业务集/租户集不按拓扑节点过滤，无需兜底）。
        if (CollectionUtils.isEmpty(nodeList) && appResourceScope.isBiz()) {
            nodeList = Collections.singletonList(buildBizRootNode(appResourceScope));
        }
        PageData<ApplicationHostDTO> pageData = scopeHostService.searchHost(
            appResourceScope,
            nodeList,
            request.getAlive(),
            null,
            request.getCleanIpv4KeyList(),
            request.getCleanIpv6KeyList(),
            request.getCleanHostNameKeyList(),
            request.getCleanOsNameKeyList(),
            (long) offset,
            (long) length
        );

        List<ApplicationHostDTO> hosts = pageData.getData();
        if (hosts == null) {
            hosts = Collections.emptyList();
        }
        // 填充云区域名称、操作系统类型名称等详情信息
        hostDetailService.fillDetailForApplicationHosts(JobContextUtil.getTenantId(), hosts);

        V4SearchScopeHostResult result = new V4SearchScopeHostResult();
        result.setTotal(pageData.getTotal());
        result.setOffset(offset);
        result.setLength(length);
        result.setData(
            hosts.stream()
                .map(OpenApiV4ScopeHostConverter::toScopeHostDTO)
                .collect(Collectors.toList())
        );
        return EsbV4Response.success(result);
    }

    /**
     * 断言资源范围为业务(biz)，否则按参数类错误(4xx)拒绝。
     *
     * <p>与 {@code ScopeFeatureUtil.assertOnlyBizSupported} 复用同一错误码
     * {@link ErrorCode#NOT_SUPPORT_FEATURE_FOR_RESOURCE_SCOPE}，但抛出
     * {@link InvalidParamException}，使 OpenAPI V4 返回 400 INVALID_ARGUMENT 而非 500 INTERNAL。</p>
     *
     * @param appResourceScope 资源范围
     */
    private void assertOnlyBizSupported(AppResourceScope appResourceScope) {
        if (!appResourceScope.isBiz()) {
            throw new InvalidParamException(
                ErrorCode.NOT_SUPPORT_FEATURE_FOR_RESOURCE_SCOPE,
                new Object[]{
                    ResourceScopeTypeEnum.BIZ.name(),
                    appResourceScope.getType().name()
                }
            );
        }
    }

    /**
     * 构造业务根节点（object_id=biz, instance_id=业务ID），用于业务作用域未指定拓扑节点时的默认全量检索。
     *
     * @param appResourceScope 业务资源范围
     * @return 业务根拓扑节点
     */
    private BizTopoNode buildBizRootNode(AppResourceScope appResourceScope) {
        BizTopoNode bizRootNode = new BizTopoNode();
        bizRootNode.setObjectId(CcNodeTypeEnum.BIZ.getType());
        bizRootNode.setInstanceId(Long.parseLong(appResourceScope.getId()));
        return bizRootNode;
    }
}
