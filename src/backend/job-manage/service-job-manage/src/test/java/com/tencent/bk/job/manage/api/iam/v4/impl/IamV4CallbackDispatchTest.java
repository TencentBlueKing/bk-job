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

package com.tencent.bk.job.manage.api.iam.v4.impl;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.crontab.api.iam.v4.IamV4CronCallbackResource;
import com.tencent.bk.job.file_gateway.api.iam.v4.IamV4FileSourceCallbackResource;
import com.tencent.bk.sdk.iamv4.callback.constants.V4CallbackMethod;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackParentDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4CallbackResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回调统一入口的分派单测。
 *
 * <p>分派键只有 {@code type}——它恒为被查询的资源类型，多实例视图不改变这一点：列举拓扑节点视图下某集群
 * 的模块，请求是 {@code type=module} + {@code parent={set,x}}；视图首段的业务列表是 {@code type=biz}
 * 且不带 parent。parent 只用于定位上级，由各资源实现自行解析。</p>
 */
class IamV4CallbackDispatchTest {

    private static final String TENANT_ID = "default";

    private final List<BaseIamV4CallbackService> services = new ArrayList<>();

    private IamV4CronCallbackResource cronCallbackResource;
    private IamV4FileSourceCallbackResource fileSourceCallbackResource;
    private IamV4CallbackResourceImpl dispatcher;

    @BeforeEach
    void setUp() {
        for (String resourceType : Arrays.asList(
            IamV4ResourceTypeId.BIZ, IamV4ResourceTypeId.BIZ_CONTAINER, IamV4ResourceTypeId.BIZ_DYNAMIC_GROUP,
            IamV4ResourceTypeId.HOST, IamV4ResourceTypeId.SET, IamV4ResourceTypeId.MODULE,
            IamV4ResourceTypeId.DYNAMIC_GROUP, IamV4ResourceTypeId.CLUSTER, IamV4ResourceTypeId.NAMESPACE,
            IamV4ResourceTypeId.SCRIPT)) {
            BaseIamV4CallbackService handler = mock(BaseIamV4CallbackService.class);
            when(handler.getResourceType()).thenReturn(resourceType);
            // 用响应体回传命中的资源类型，便于断言
            when(handler.baseCallback(anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.<V4CallbackResp<?>>ok(V4CallbackResp.ok(resourceType)));
            services.add(handler);
        }
        cronCallbackResource = mock(IamV4CronCallbackResource.class);
        fileSourceCallbackResource = mock(IamV4FileSourceCallbackResource.class);
        dispatcher = new IamV4CallbackResourceImpl(services, cronCallbackResource, fileSourceCallbackResource);
    }

    @Test
    @DisplayName("host 各实例视图的层级：按被查询的资源类型分派，parent 只定位上级")
    void hostViewLevelsDispatchByQueriedType() {
        // 业务下主机：业务 -> 主机
        assertThat(list(IamV4ResourceTypeId.HOST, IamV4ResourceTypeId.BIZ))
            .isEqualTo(IamV4ResourceTypeId.HOST);
        // 拓扑节点：业务 -> 集群 -> 模块
        assertThat(list(IamV4ResourceTypeId.SET, IamV4ResourceTypeId.BIZ))
            .isEqualTo(IamV4ResourceTypeId.SET);
        assertThat(list(IamV4ResourceTypeId.MODULE, IamV4ResourceTypeId.SET))
            .isEqualTo(IamV4ResourceTypeId.MODULE);
        // 动态分组：业务 -> 动态分组
        assertThat(list(IamV4ResourceTypeId.DYNAMIC_GROUP, IamV4ResourceTypeId.BIZ_DYNAMIC_GROUP))
            .isEqualTo(IamV4ResourceTypeId.DYNAMIC_GROUP);
        // 业务下容器：业务 -> 容器集群 -> 命名空间
        assertThat(list(IamV4ResourceTypeId.CLUSTER, IamV4ResourceTypeId.BIZ_CONTAINER))
            .isEqualTo(IamV4ResourceTypeId.CLUSTER);
        assertThat(list(IamV4ResourceTypeId.NAMESPACE, IamV4ResourceTypeId.CLUSTER))
            .isEqualTo(IamV4ResourceTypeId.NAMESPACE);
    }

    @Test
    @DisplayName("同一 parent 下 type 不同即返回不同层级：parent=biz 时主机与集群各归各")
    void sameParentDifferentTypeGoesToDifferentHandler() {
        assertThat(list(IamV4ResourceTypeId.HOST, IamV4ResourceTypeId.BIZ))
            .isEqualTo(IamV4ResourceTypeId.HOST);
        assertThat(list(IamV4ResourceTypeId.SET, IamV4ResourceTypeId.BIZ))
            .isEqualTo(IamV4ResourceTypeId.SET);
    }

    @Test
    @DisplayName("视图首段：type 为业务变体且不带 parent")
    void viewFirstSegmentHasNoParent() {
        V4CallbackRequestDTO request = new V4CallbackRequestDTO();
        request.setType(IamV4ResourceTypeId.BIZ);
        request.setMethod(V4CallbackMethod.LIST_INSTANCE.getValue());

        assertThat(dataOf(dispatcher.callback(TENANT_ID, "Basic x", "req-1", request)))
            .isEqualTo(IamV4ResourceTypeId.BIZ);

        V4CallbackRequestDTO containerRequest = new V4CallbackRequestDTO();
        containerRequest.setType(IamV4ResourceTypeId.BIZ_CONTAINER);
        containerRequest.setMethod(V4CallbackMethod.LIST_INSTANCE.getValue());

        assertThat(dataOf(dispatcher.callback(TENANT_ID, "Basic x", "req-1", containerRequest)))
            .isEqualTo(IamV4ResourceTypeId.BIZ_CONTAINER);
    }

    @Test
    @DisplayName("fetch_instance_info 同样按自身 type 分派")
    void fetchInstanceInfoDispatchesByType() {
        V4CallbackRequestDTO request = new V4CallbackRequestDTO();
        request.setType(IamV4ResourceTypeId.MODULE);
        request.setMethod(V4CallbackMethod.FETCH_INSTANCE_INFO.getValue());
        V4CallbackFilterDTO filter = new V4CallbackFilterDTO();
        filter.setIds(Collections.singletonList("300"));
        request.setFilter(filter);

        assertThat(dataOf(dispatcher.callback(TENANT_ID, "Basic x", "req-1", request)))
            .isEqualTo(IamV4ResourceTypeId.MODULE);
    }

    @Test
    @DisplayName("数据在其他微服务的资源类型：转发且原样带上租户、认证头与 X-Request-Id")
    void remoteResourceForwardedWithHeaders() {
        V4CallbackRequestDTO request = new V4CallbackRequestDTO();
        request.setType(IamV4ResourceTypeId.CRON);
        request.setMethod(V4CallbackMethod.LIST_INSTANCE.getValue());

        dispatcher.callback(TENANT_ID, "Basic x", "req-1", request);

        verify(cronCallbackResource).callback(TENANT_ID, "Basic x", "req-1", request);
    }

    @Test
    @DisplayName("未注册的资源类型：返回 400，不透传给任何处理器")
    void unknownTypeRejected() {
        V4CallbackRequestDTO request = new V4CallbackRequestDTO();
        request.setType("unknown_type");
        request.setMethod(V4CallbackMethod.LIST_INSTANCE.getValue());

        ResponseEntity<V4CallbackResp<?>> response = dispatcher.callback(TENANT_ID, "Basic x", "req-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("缺少 type：返回 400")
    void missingTypeRejected() {
        V4CallbackRequestDTO request = new V4CallbackRequestDTO();
        request.setMethod(V4CallbackMethod.LIST_INSTANCE.getValue());

        ResponseEntity<V4CallbackResp<?>> response = dispatcher.callback(TENANT_ID, "Basic x", "req-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * 模拟权限中心列举 {@code queriedType} 的实例，其上级类型为 {@code parentType}，
     * 返回实际命中的处理器所属资源类型
     */
    private String list(String queriedType, String parentType) {
        V4CallbackRequestDTO request = new V4CallbackRequestDTO();
        request.setType(queriedType);
        request.setMethod(V4CallbackMethod.LIST_INSTANCE.getValue());
        V4CallbackFilterDTO filter = new V4CallbackFilterDTO();
        V4CallbackParentDTO parent = new V4CallbackParentDTO();
        parent.setType(parentType);
        parent.setId("1");
        filter.setParent(parent);
        request.setFilter(filter);
        return dataOf(dispatcher.callback(TENANT_ID, "Basic x", "req-1", request));
    }

    private String dataOf(ResponseEntity<V4CallbackResp<?>> response) {
        V4CallbackResp<?> body = response.getBody();
        assertThat(body).isNotNull();
        return (String) body.getData();
    }
}
