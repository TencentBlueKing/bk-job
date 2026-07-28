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

package com.tencent.bk.job.manage.api.esb.v4.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiScopeHostV4ResourceImpl;
import com.tencent.bk.job.manage.model.esb.v4.req.V4GetBizHostTopoTreeRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4SearchScopeHostRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4TopoNodeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4HostTopoNodeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4ScopeHostDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4SearchScopeHostResult;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import com.tencent.bk.job.manage.service.host.ScopeHostService;
import com.tencent.bk.job.manage.service.host.ScopeTopoHostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OpenApiScopeHostV4ResourceImpl} 单元测试。
 */
class OpenApiScopeHostV4ResourceImplTest {

    private static final String USERNAME = "tester";
    private static final String APP_CODE = "bk_job";

    private AppScopeMappingService appScopeMappingService;
    private ScopeTopoHostService scopeTopoHostService;
    private ScopeHostService scopeHostService;
    private HostDetailService hostDetailService;
    private OpenApiScopeHostV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        appScopeMappingService = mock(AppScopeMappingService.class);
        scopeTopoHostService = mock(ScopeTopoHostService.class);
        scopeHostService = mock(ScopeHostService.class);
        hostDetailService = mock(HostDetailService.class);
        resource = new OpenApiScopeHostV4ResourceImpl(
            appScopeMappingService, scopeTopoHostService, scopeHostService, hostDetailService);
    }

    // ---------------------- getBizHostTopoTree ----------------------

    @Test
    @DisplayName("拓扑树：biz 正常返回多层结构并正确映射 host_count/child，无 lazy 字段")
    void getBizHostTopoTree_bizNormal() {
        V4GetBizHostTopoTreeRequest request = new V4GetBizHostTopoTreeRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(1L);

        CcTopologyNodeVO module = buildNode("module", "模块", 100L, "db", 3, null);
        CcTopologyNodeVO set = buildNode("set", "集群", 10L, "set-a", 3,
            Collections.singletonList(module));
        CcTopologyNodeVO biz = buildNode("biz", "业务", 2L, "biz-a", 3,
            Collections.singletonList(set));
        when(scopeTopoHostService.listAppTopologyHostCountTree(eq(USERNAME), any(AppResourceScope.class)))
            .thenReturn(biz);

        EsbV4Response<V4HostTopoNodeDTO> response =
            resource.getBizHostTopoTree(USERNAME, APP_CODE, request);

        V4HostTopoNodeDTO root = response.getData();
        assertThat(root.getObjectId()).isEqualTo("biz");
        assertThat(root.getObjectName()).isEqualTo("业务");
        assertThat(root.getInstanceId()).isEqualTo(2L);
        assertThat(root.getInstanceName()).isEqualTo("biz-a");
        assertThat(root.getHostCount()).isEqualTo(3);
        assertThat(root.getChild()).hasSize(1);

        V4HostTopoNodeDTO setNode = root.getChild().get(0);
        assertThat(setNode.getObjectId()).isEqualTo("set");
        assertThat(setNode.getChild()).hasSize(1);

        V4HostTopoNodeDTO moduleNode = setNode.getChild().get(0);
        assertThat(moduleNode.getObjectId()).isEqualTo("module");
        assertThat(moduleNode.getHostCount()).isEqualTo(3);
        assertThat(moduleNode.getChild()).isNull();
    }

    @Test
    @DisplayName("拓扑树：单节点空树 host_count=0、child 为空")
    void getBizHostTopoTree_singleEmptyNode() {
        V4GetBizHostTopoTreeRequest request = new V4GetBizHostTopoTreeRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(1L);
        when(scopeTopoHostService.listAppTopologyHostCountTree(eq(USERNAME), any(AppResourceScope.class)))
            .thenReturn(buildNode("biz", "业务", 2L, "biz-a", 0, null));

        EsbV4Response<V4HostTopoNodeDTO> response =
            resource.getBizHostTopoTree(USERNAME, APP_CODE, request);

        assertThat(response.getData().getHostCount()).isEqualTo(0);
        assertThat(response.getData().getChild()).isNull();
    }

    @Test
    @DisplayName("拓扑树：biz_set 抛参数错误（仅业务支持）")
    void getBizHostTopoTree_bizSetRejected() {
        V4GetBizHostTopoTreeRequest request = new V4GetBizHostTopoTreeRequest();
        request.setScopeType("biz_set");
        request.setScopeId("9991001");
        when(appScopeMappingService.getAppIdByScope("biz_set", "9991001")).thenReturn(100L);

        assertThatThrownBy(() -> resource.getBizHostTopoTree(USERNAME, APP_CODE, request))
            .isInstanceOf(NotImplementedException.class);
    }

    // ---------------------- searchScopeHost ----------------------

    @Test
    @DisplayName("搜索：biz + 拓扑节点 + 各关键字，参数正确透传，offset/length 映射到 start/pageSize")
    void searchScopeHost_bizWithConditions() {
        V4SearchScopeHostRequest request = new V4SearchScopeHostRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setAlive(1);
        request.setIpv4KeyList(Arrays.asList("127.0.0.1", " ", null));
        request.setIpv6KeyList(Collections.singletonList("::1"));
        request.setHostNameKeyList(Collections.singletonList("host"));
        request.setOsNameKeyList(Collections.singletonList("linux"));
        request.setOffset(5);
        request.setLength(20);
        V4TopoNodeDTO node = new V4TopoNodeDTO();
        node.setObjectId("module");
        node.setInstanceId(100L);
        request.setTopoNodeList(Collections.singletonList(node));
        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(1L);

        when(scopeHostService.searchHost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(buildHostPageData(1L, Collections.singletonList(buildHost())));

        EsbV4Response<V4SearchScopeHostResult> response =
            resource.searchScopeHost(USERNAME, APP_CODE, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BizTopoNode>> nodeCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Integer> aliveCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> startCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> pageSizeCaptor = ArgumentCaptor.forClass(Long.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> ipv4Captor = ArgumentCaptor.forClass(List.class);
        verify(scopeHostService).searchHost(
            any(AppResourceScope.class),
            nodeCaptor.capture(),
            aliveCaptor.capture(),
            isNull(),
            ipv4Captor.capture(),
            anyList(),
            anyList(),
            anyList(),
            startCaptor.capture(),
            pageSizeCaptor.capture()
        );

        assertThat(aliveCaptor.getValue()).isEqualTo(1);
        assertThat(startCaptor.getValue()).isEqualTo(5L);
        assertThat(pageSizeCaptor.getValue()).isEqualTo(20L);
        // 拓扑节点正确映射到内部 BizTopoNode
        assertThat(nodeCaptor.getValue()).hasSize(1);
        assertThat(nodeCaptor.getValue().get(0).getObjectId()).isEqualTo("module");
        assertThat(nodeCaptor.getValue().get(0).getInstanceId()).isEqualTo(100L);
        // IPv4 关键字列表已清洗（去除空白与 null）
        assertThat(ipv4Captor.getValue()).containsExactly("127.0.0.1");

        // 响应字段映射正确，gseAgentAlive->alive
        V4SearchScopeHostResult result = response.getData();
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getOffset()).isEqualTo(5);
        assertThat(result.getLength()).isEqualTo(20);
        assertThat(result.getData()).hasSize(1);
        V4ScopeHostDTO host = result.getData().get(0);
        assertThat(host.getHostId()).isEqualTo(1000L);
        assertThat(host.getIp()).isEqualTo("127.0.0.1");
        assertThat(host.getCloudAreaId()).isEqualTo(0L);
        assertThat(host.getCloudAreaName()).isEqualTo("默认管控区域");
        assertThat(host.getOsType()).isEqualTo("1");
        assertThat(host.getAlive()).isEqualTo(1);
    }

    @Test
    @DisplayName("搜索：alive 不传则透传 null；空结果 total=0、data 为空列表")
    void searchScopeHost_aliveNullAndEmptyResult() {
        V4SearchScopeHostRequest request = new V4SearchScopeHostRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(1L);
        when(scopeHostService.searchHost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(buildHostPageData(0L, Collections.emptyList()));

        EsbV4Response<V4SearchScopeHostResult> response =
            resource.searchScopeHost(USERNAME, APP_CODE, request);

        ArgumentCaptor<Integer> aliveCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(scopeHostService).searchHost(
            any(), any(), aliveCaptor.capture(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        assertThat(aliveCaptor.getValue()).isNull();

        assertThat(response.getData().getTotal()).isEqualTo(0L);
        assertThat(response.getData().getData()).isEmpty();
        assertThat(response.getData().getOffset()).isEqualTo(0);
        assertThat(response.getData().getLength()).isEqualTo(10);
    }

    @Test
    @DisplayName("搜索：biz_set 无拓扑节点仍可搜索（nodeList 透传 null）")
    void searchScopeHost_bizSetIgnoreTopoNode() {
        V4SearchScopeHostRequest request = new V4SearchScopeHostRequest();
        request.setScopeType("biz_set");
        request.setScopeId("9991001");
        when(appScopeMappingService.getAppIdByScope("biz_set", "9991001")).thenReturn(100L);
        when(scopeHostService.searchHost(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(buildHostPageData(1L, Collections.singletonList(buildHost())));

        EsbV4Response<V4SearchScopeHostResult> response =
            resource.searchScopeHost(USERNAME, APP_CODE, request);

        assertThat(response.getData().getData()).hasSize(1);
    }

    @Test
    @DisplayName("搜索：响应 JSON 不暴露 agentId、bk_biz_id")
    void searchScopeHost_responseHidesSensitiveFields() throws Exception {
        V4ScopeHostDTO host = com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiV4ScopeHostConverter
            .toScopeHostDTO(buildHost());
        String json = new ObjectMapper().writeValueAsString(host);
        assertThat(json).doesNotContain("agentId");
        assertThat(json).doesNotContain("agent_id");
        assertThat(json).doesNotContain("bk_biz_id");
        assertThat(json).contains("bk_host_id");
        assertThat(json).contains("os_type");
    }

    // ---------------------- helpers ----------------------

    private CcTopologyNodeVO buildNode(String objectId,
                                       String objectName,
                                       Long instanceId,
                                       String instanceName,
                                       int count,
                                       List<CcTopologyNodeVO> child) {
        CcTopologyNodeVO node = new CcTopologyNodeVO();
        node.setObjectId(objectId);
        node.setObjectName(objectName);
        node.setInstanceId(instanceId);
        node.setInstanceName(instanceName);
        node.setCount(count);
        node.setChild(child);
        return node;
    }

    private ApplicationHostDTO buildHost() {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(1000L);
        host.setIp("127.0.0.1");
        host.setIpv6("0000:0000:0000:0000:0000:0000:0000:0001");
        host.setCloudAreaId(0L);
        host.setCloudAreaName("默认管控区域");
        host.setHostName("host-a");
        host.setOsName("linux centos");
        host.setOsType("1");
        host.setAgentId("010000525400c48e5edc17431721099896");
        host.setGseAgentAlive(true);
        return host;
    }

    private PageData<ApplicationHostDTO> buildHostPageData(long total, List<ApplicationHostDTO> data) {
        PageData<ApplicationHostDTO> pageData = new PageData<>();
        pageData.setStart(0);
        pageData.setPageSize(10);
        pageData.setTotal(total);
        pageData.setData(data);
        return pageData;
    }
}
