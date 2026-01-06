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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.model.container.KubeNodeDTO;
import com.tencent.bk.job.common.cc.model.container.KubeTopologyDTO;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.manage.api.web.WebContainerResource;
import com.tencent.bk.job.manage.model.mapper.ContainerMapper;
import com.tencent.bk.job.manage.model.query.ContainerQuery;
import com.tencent.bk.job.manage.model.web.request.chooser.ListTopologyTreesReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerCheckReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerDetailReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ListContainerByTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.chooser.container.ContainerTopologyNodeVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.ContainerService;
import com.tencent.bk.job.manage.service.host.CurrentTenantHostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebContainerResourceImpl implements WebContainerResource {

    private final ContainerService containerService;
    private final ApplicationService applicationService;
    private final MessageI18nService i18nService;
    private final CurrentTenantHostService currentTenantHostService;

    @Autowired
    public WebContainerResourceImpl(ContainerService containerService,
                                    ApplicationService applicationService,
                                    MessageI18nService i18nService,
                                    CurrentTenantHostService currentTenantHostService) {
        this.containerService = containerService;
        this.applicationService = applicationService;
        this.i18nService = i18nService;
        this.currentTenantHostService = currentTenantHostService;
    }

    @Override
    public Response<List<ContainerTopologyNodeVO>> listTopologyTrees(String username,
                                                                     AppResourceScope appResourceScope,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     ListTopologyTreesReq req) {
        if (appResourceScope.isBizSet()) {
            // 业务集暂时不支持容器拓扑
            return Response.buildSuccessResp(buildEmptyTopoTree(scopeType, scopeId));
        }
        if (!isContainerExecuteSupport(appResourceScope)) {
            // 未开启"容器执行"特性的灰度，返回空的容器拓扑
            return Response.buildSuccessResp(buildEmptyTopoTree(scopeType, scopeId));
        }

        KubeTopologyDTO topo = containerService.getBizKubeCacheTopo(Long.parseLong(scopeId));

        ContainerTopologyNodeVO topoVO = new ContainerTopologyNodeVO();
        topoVO.setInstanceId(topo.getBiz().getId());
        topoVO.setInstanceName(topo.getBiz().getName());
        topoVO.setObjectId("biz");
        topoVO.setObjectName(i18nService.getI18n("cmdb.object.name.biz"));
        topoVO.setCount(topo.getBiz().getCount());

        if (CollectionUtils.isNotEmpty(topo.getNodes())) {
            List<ContainerTopologyNodeVO> clusters = new ArrayList<>();
            for (KubeNodeDTO node : topo.getNodes()) {
                clusters.add(convertToContainerTopologyNodeVO(node));
            }
            topoVO.setChild(clusters);
        }

        return Response.buildSuccessResp(
            Collections.singletonList(
                topoVO
            )
        );
    }

    private boolean isContainerExecuteSupport(AppResourceScope appResourceScope) {
        if (appResourceScope.isBizSet()) {
            return false;
        }
        return FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_CONTAINER_EXECUTE,
            ToggleEvaluateContext.builder().addContextParam(
                ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, appResourceScope));
    }

    private ContainerTopologyNodeVO convertToContainerTopologyNodeVO(KubeNodeDTO node) {
        ContainerTopologyNodeVO nodeVO = new ContainerTopologyNodeVO();
        nodeVO.setObjectId(node.getKind());
        nodeVO.setObjectName(node.getKind());
        nodeVO.setInstanceId(node.getId());
        nodeVO.setInstanceName(node.getName());
        nodeVO.setCount(node.getCount());
        List<ContainerTopologyNodeVO> children = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(node.getNodes())) {
            for (KubeNodeDTO childNode : node.getNodes()) {
                children.add(convertToContainerTopologyNodeVO(childNode));
            }
        }
        nodeVO.setChild(children);
        return nodeVO;
    }

    private List<ContainerTopologyNodeVO> buildEmptyTopoTree(String scopeType, String scopeId) {
        ContainerTopologyNodeVO topo = new ContainerTopologyNodeVO();
        ApplicationDTO bizSetApp = applicationService.getAppByScope(scopeType, scopeId);
        topo.setInstanceId(bizSetApp.getId());
        topo.setInstanceName(bizSetApp.getName());
        if (bizSetApp.isBiz()) {
            topo.setObjectId("biz");
            topo.setObjectName(i18nService.getI18n("cmdb.object.name.biz"));
        } else if (bizSetApp.isBizSet()) {
            topo.setObjectId("biz_set");
            topo.setObjectName(i18nService.getI18n("cmdb.object.name.biz_set"));
        } else if (bizSetApp.isTenantSet()) {
            topo.setObjectId("tenant_set");
            topo.setObjectName(i18nService.getI18n("cmdb.object.name.tenant_set"));
        }
        topo.setCount(0);
        return Collections.singletonList(topo);
    }

    @Override
    public Response<PageData<ContainerVO>> listContainerByTopologyNodes(String username,
                                                                        AppResourceScope appResourceScope,
                                                                        String scopeType,
                                                                        String scopeId,
                                                                        ListContainerByTopologyNodesReq req) {
        if (!isContainerExecuteSupport(appResourceScope)) {
            // 未开启"容器执行"特性的灰度，返回空的容器拓扑
            return Response.buildSuccessResp(PageData.emptyPageData(req.getStart(), req.getPageSize()));
        }
        ContainerQuery containerQuery =
            ContainerQuery.fromListContainerByTopologyNodesReq(Long.parseLong(scopeId), req);

        PageData<ContainerDetailDTO> pageData = containerService.listPageKubeContainerByTopo(containerQuery);

        PageData<ContainerVO> containerVOPageData =
            PageData.from(pageData, ContainerMapper::toContainerVO);
        fillNodesHostInfo(containerVOPageData.getData());

        return Response.buildSuccessResp(containerVOPageData);
    }

    private void fillNodesHostInfo(Collection<ContainerVO> containerVOs) {
        List<Long> hostIds = containerVOs.stream()
            .map(ContainerVO::getNodeHostId).distinct().collect(Collectors.toList());
        Map<Long, ApplicationHostDTO> hostMap = currentTenantHostService.listHostsByHostIds(hostIds);

        containerVOs.forEach(containerVO -> {
            Long nodeHostId = containerVO.getNodeHostId();
            ApplicationHostDTO nodeHost = hostMap.get(nodeHostId);
            if (nodeHost != null) {
                containerVO.setNodeIp(nodeHost.getPrimaryIp());
            }
        });
    }

    @Override
    public Response<PageData<ContainerIdWithMeta>> listContainerIdByTopologyNodes(
        String username,
        AppResourceScope appResourceScope,
        String scopeType,
        String scopeId,
        ListContainerByTopologyNodesReq req
    ) {
        if (!isContainerExecuteSupport(appResourceScope)) {
            // 未开启"容器执行"特性的灰度，返回空的容器拓扑
            return Response.buildSuccessResp(PageData.emptyPageData(req.getStart(), req.getPageSize()));
        }
        ContainerQuery containerQuery =
            ContainerQuery.fromListContainerByTopologyNodesReq(Long.parseLong(scopeId), req);

        PageData<ContainerDetailDTO> pageData;
        if (containerQuery.getPageCondition() != null) {
            pageData = containerService.listPageKubeContainerByTopo(containerQuery);
        } else {
            List<ContainerDetailDTO> containerList = containerService.listKubeContainerByTopo(containerQuery);
            pageData = new PageData<>(0, Integer.MAX_VALUE,
                containerList == null ? 0L : containerList.size(), containerList);
        }

        PageData<ContainerIdWithMeta> containerIdPageData =
            PageData.from(pageData, container -> new ContainerIdWithMeta(container.getContainer().getId()));

        return Response.buildSuccessResp(containerIdPageData);
    }

    @Override
    public Response<List<ContainerVO>> checkContainers(String username,
                                                       AppResourceScope appResourceScope,
                                                       String scopeType,
                                                       String scopeId,
                                                       ContainerCheckReq req) {
        if (!isContainerExecuteSupport(appResourceScope)) {
            // 未开启"容器执行"特性的灰度，返回空的容器拓扑
            return Response.buildSuccessResp(Collections.emptyList());
        }
        List<ContainerDetailDTO> containers =
            containerService.listKubeContainerByUIds(Long.parseLong(scopeId), req.getUidList());

        return Response.buildSuccessResp(toContainerVOS(containers));
    }

    private List<ContainerVO> toContainerVOS(List<ContainerDetailDTO> containers) {
        if (CollectionUtils.isEmpty(containers)) {
            return Collections.emptyList();
        }

        List<ContainerVO> containerVOS = containers.stream()
            .map(ContainerMapper::toContainerVO).collect(Collectors.toList());
        fillNodesHostInfo(containerVOS);

        return containerVOS;
    }

    @Override
    public Response<List<ContainerVO>> getContainerDetails(String username,
                                                           AppResourceScope appResourceScope,
                                                           String scopeType,
                                                           String scopeId,
                                                           ContainerDetailReq req) {
        List<ContainerDetailDTO> containers = containerService.listKubeContainerByIds(
            Long.parseLong(scopeId),
            req.getContainerList().stream()
                .map(ContainerIdWithMeta::getId).collect(Collectors.toList()));

        return Response.buildSuccessResp(toContainerVOS(containers));
    }
}
