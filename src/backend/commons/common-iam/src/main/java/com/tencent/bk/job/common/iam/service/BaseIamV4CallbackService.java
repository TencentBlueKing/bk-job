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

package com.tencent.bk.job.common.iam.service;

import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.sdk.iamv4.callback.V4CallbackAuthUtil;
import com.tencent.bk.sdk.iamv4.callback.constants.V4CallbackMethod;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackPageDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackParentDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4CallbackResp;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.constants.IamV4Header;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IAM v4 资源实例回调基类。负责回调请求的 Basic token 校验、按 method 分发，以及父级实例到 Job 业务的转换。
 *
 * <p>各资源类型的 v4 回调实现继承本类，只需实现 {@link #listInstance} 与 {@link #fetchInstanceInfo}。</p>
 */
@Slf4j
public abstract class BaseIamV4CallbackService {

    /**
     * fetch_instance_info 的 requires 中表示拓扑路径的属性名
     */
    protected static final String BK_IAM_PATH_ATTR = "_bk_iam_path_";

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * v4 业务级资源类型（含各实例视图的首段变体）到 Job 资源范围类型的映射
     */
    private static final Map<String, ResourceScopeTypeEnum> SCOPE_TYPE_BY_PARENT_TYPE = new HashMap<>();

    static {
        SCOPE_TYPE_BY_PARENT_TYPE.put(IamV4ResourceTypeId.BIZ, ResourceScopeTypeEnum.BIZ);
        SCOPE_TYPE_BY_PARENT_TYPE.put(IamV4ResourceTypeId.BIZ_DYNAMIC_GROUP, ResourceScopeTypeEnum.BIZ);
        SCOPE_TYPE_BY_PARENT_TYPE.put(IamV4ResourceTypeId.BIZ_CONTAINER, ResourceScopeTypeEnum.BIZ);
        SCOPE_TYPE_BY_PARENT_TYPE.put(IamV4ResourceTypeId.BIZ_SET, ResourceScopeTypeEnum.BIZ_SET);
        SCOPE_TYPE_BY_PARENT_TYPE.put(IamV4ResourceTypeId.TENANT_SET, ResourceScopeTypeEnum.TENANT_SET);
    }

    protected final AppScopeMappingService appScopeMappingService;
    protected final TenantEnvService tenantEnvService;
    protected final IamV4AuthService iamV4AuthService;

    protected BaseIamV4CallbackService(AppScopeMappingService appScopeMappingService,
                                       TenantEnvService tenantEnvService,
                                       IamV4AuthService iamV4AuthService) {
        this.appScopeMappingService = appScopeMappingService;
        this.tenantEnvService = tenantEnvService;
        this.iamV4AuthService = iamV4AuthService;
    }

    /**
     * 回调统一入口：先做 token 认证，再按 method 分发。
     *
     * <p>按蓝鲸新版 API 协议，成功返回 2xx + {@code {"data":...}}，失败返回非 2xx + {@code {"error":...}}；
     * 请求头 {@code X-Request-Id} 原样回传，便于与权限中心对账排查。</p>
     *
     * @param tenantId      租户 ID（回调请求头传入）
     * @param authorization Authorization 头，形如 {@code Basic base64(bk_iam:token)}
     * @param requestId     请求头 X-Request-Id
     * @param request       回调请求体
     */
    public ResponseEntity<V4CallbackResp<?>> baseCallback(String tenantId,
                                                          String authorization,
                                                          String requestId,
                                                          V4CallbackRequestDTO request) {
        String effectiveTenantId = resolveTenantId(tenantId);
        if (StringUtils.isBlank(effectiveTenantId)) {
            log.warn("Tenant id is required for IAM v4 callback|requestId={}", requestId);
            return buildResponse(HttpStatus.BAD_REQUEST, requestId,
                V4CallbackResp.fail("INVALID_REQUEST", "Header " + JobCommonHeaders.BK_TENANT_ID + " is required"));
        }
        // 取回调 token 依赖租户，认证前先把租户确定下来并写入上下文
        JobContextUtil.setUser(new User(effectiveTenantId, null, null));

        if (!authenticate(effectiveTenantId, authorization)) {
            log.warn("IAM v4 callback authenticate failed, reject|requestId={}", requestId);
            return buildResponse(HttpStatus.UNAUTHORIZED, requestId,
                V4CallbackResp.fail("UNAUTHORIZED", "Invalid IAM v4 callback token"));
        }

        V4CallbackMethod method = V4CallbackMethod.of(request.getMethod());
        if (method == null) {
            log.warn("Unknown IAM v4 callback method|method={}|requestId={}", request.getMethod(), requestId);
            return buildResponse(HttpStatus.BAD_REQUEST, requestId,
                V4CallbackResp.fail("INVALID_REQUEST", "Unknown callback method: " + request.getMethod()));
        }
        // 兜底捕获，保证处理异常时也返回 v4 协议的错误响应
        try {
            Object data = method == V4CallbackMethod.LIST_INSTANCE
                ? listInstance(request) : fetchInstanceInfo(request);
            return buildResponse(HttpStatus.OK, requestId, V4CallbackResp.ok(data));
        } catch (IllegalArgumentException e) {
            // 入参不合法（如无法识别的 parent 类型、非法的资源 ID 格式）
            log.warn("Invalid IAM v4 callback request|type={}|method={}|requestId={}",
                request.getType(), request.getMethod(), requestId, e);
            return buildResponse(HttpStatus.BAD_REQUEST, requestId,
                V4CallbackResp.fail("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Handle IAM v4 callback failed|type={}|method={}|requestId={}",
                request.getType(), request.getMethod(), requestId, e);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, requestId,
                V4CallbackResp.fail("INTERNAL", "Handle callback failed: " + e.getMessage()));
        }
    }

    private ResponseEntity<V4CallbackResp<?>> buildResponse(HttpStatus status,
                                                            String requestId,
                                                            V4CallbackResp<?> body) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (StringUtils.isNotBlank(requestId)) {
            builder.header(IamV4Header.X_REQUEST_ID, requestId);
        }
        return builder.body(body);
    }

    private boolean authenticate(String tenantId, String authorization) {
        try {
            String token = iamV4AuthService.getAuthToken(tenantId);
            return V4CallbackAuthUtil.verify(authorization, token);
        } catch (Exception e) {
            log.error("Get IAM v4 auth token failed while authenticating callback", e);
            return false;
        }
    }

    /**
     * 单租户模式下兼容权限中心不传租户头的调用，取默认租户。
     */
    private String resolveTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId) && !tenantEnvService.isTenantEnabled()) {
            return TenantIdConstants.DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    /**
     * 本实现负责的 v4 资源类型 ID（取值见 {@link IamV4ResourceTypeId}）。
     *
     * <p>v4 全系统只有一个回调地址、资源类型在请求体里，故需要按资源类型把请求分派到对应实现，
     * 这里的声明就是分派依据。</p>
     */
    public abstract String getResourceType();

    protected abstract V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request);

    protected abstract List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request);

    /**
     * 根据回调 list_instance 的上级过滤条件解析出 Job 内部业务 ID。
     *
     * <p>启用实例视图后同一资源类型可能出现在多个视图中，{@code parent.type} 会是不同的业务级首段
     * （{@code biz}/{@code biz_set}/{@code tenant_set} 及其视图变体），需按类型分派。</p>
     *
     * @param parent 上级过滤条件
     * @return Job 业务 ID；无上级过滤时返回 null
     * @throws IllegalArgumentException parent.type 无法识别
     */
    protected Long getAppIdByParent(V4CallbackParentDTO parent) {
        if (parent == null || StringUtils.isBlank(parent.getType()) || StringUtils.isBlank(parent.getId())) {
            return null;
        }
        return appScopeMappingService.getAppIdByScope(toResourceScope(parent));
    }

    /**
     * 把 v4 回调的上级实例转换为 Job 的资源范围。
     */
    protected ResourceScope toResourceScope(V4CallbackParentDTO parent) {
        ResourceScopeTypeEnum scopeType = SCOPE_TYPE_BY_PARENT_TYPE.get(parent.getType());
        if (scopeType == null) {
            throw new IllegalArgumentException("Unrecognized parent resource type: " + parent.getType());
        }
        return new ResourceScope(scopeType, parent.getId());
    }

    /**
     * 从 list_instance 的 ancestors 解析 CMDB bizId。
     *
     * <p>祖先里的业务首段可能是 {@code biz} / {@code biz_container} / {@code biz_dynamic_group}，
     * 按类型查找，不依赖位置。biz_set / tenant_set 不是 CMDB 业务，会被跳过。</p>
     */
    protected long parseAncestorBizId(V4CallbackFilterDTO filter) {
        if (filter == null || filter.getAncestors() == null || filter.getAncestors().isEmpty()) {
            throw new IllegalArgumentException("Ancestors with biz is required for listing instances");
        }
        for (V4CallbackParentDTO ancestor : filter.getAncestors()) {
            if (ancestor == null || StringUtils.isBlank(ancestor.getType()) || StringUtils.isBlank(ancestor.getId())) {
                continue;
            }
            ResourceScopeTypeEnum scopeType = SCOPE_TYPE_BY_PARENT_TYPE.get(ancestor.getType());
            if (scopeType != ResourceScopeTypeEnum.BIZ) {
                continue;
            }
            try {
                return Long.parseLong(ancestor.getId());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegal ancestor biz id: " + ancestor.getId());
            }
        }
        throw new IllegalArgumentException("Biz ancestor is required for listing instances");
    }

    /**
     * 解析 list_instance 的直接上级实例 ID，并校验类型。
     */
    protected long parseParentInstanceId(V4CallbackParentDTO parent, String expectedType) {
        if (parent == null || StringUtils.isBlank(parent.getType()) || StringUtils.isBlank(parent.getId())) {
            throw new IllegalArgumentException("Parent " + expectedType + " is required");
        }
        if (!expectedType.equals(parent.getType())) {
            throw new IllegalArgumentException(
                "Unexpected parent type: " + parent.getType() + ", expected " + expectedType);
        }
        try {
            return Long.parseLong(parent.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal parent id: " + parent.getId());
        }
    }

    /**
     * 把回调分页参数转换为 Job 的查询条件。权限中心页码从 1 开始。
     */
    protected BaseSearchCondition buildSearchCondition(V4CallbackPageDTO page) {
        BaseSearchCondition condition = new BaseSearchCondition();
        int pageNum = (page == null || page.getPage() == null || page.getPage() < 1) ? 1 : page.getPage();
        int pageSize = (page == null || page.getPageSize() == null || page.getPageSize() < 1)
            ? DEFAULT_PAGE_SIZE : page.getPageSize();
        condition.setStart((pageNum - 1) * pageSize);
        condition.setLength(pageSize);
        return condition;
    }

    /**
     * 判断 fetch_instance_info 是否要求返回拓扑路径。
     */
    protected boolean isPathRequired(V4CallbackRequestDTO request) {
        return request.getRequires() != null && request.getRequires().contains(BK_IAM_PATH_ATTR);
    }

    /**
     * 构造业务下资源的拓扑路径，如 {@code /biz,2/}。
     *
     * <p>路径只含祖先，不含资源实例自身。scope 为空（业务已删除等）时返回 null。</p>
     */
    protected String buildScopePaths(ResourceScope scope) {
        if (scope == null) {
            return null;
        }
        String scopeType = IamV4ResourceTypeMapper.getV4ResourceTypeIdForScope(scope);
        return "/" + scopeType + "," + scope.getId() + "/";
    }

    /**
     * 从 fetch_instance_info 的 {@code filter.root_resource_id} 解析 CMDB bizId。
     *
     * <p>set / module / dynamic_group / cluster / namespace 无法仅凭实例 ID 定位业务，
     * 权限中心会带上顶层资源 ID（对本系统即业务 ID）。</p>
     */
    protected long parseRootResourceBizId(V4CallbackFilterDTO filter) {
        if (filter == null || StringUtils.isBlank(filter.getRootResourceId())) {
            throw new IllegalArgumentException("root_resource_id is required for fetch_instance_info");
        }
        try {
            return Long.parseLong(filter.getRootResourceId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal root_resource_id: " + filter.getRootResourceId());
        }
    }

    /**
     * 拼接祖先路径，参数成对出现：type, id, ...。例如 {@code buildIamPath("biz", "20")} → {@code /biz,20/}。
     */
    protected String buildIamPath(String... typeAndIds) {
        if (typeAndIds == null || typeAndIds.length == 0 || typeAndIds.length % 2 != 0) {
            return null;
        }
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < typeAndIds.length; i += 2) {
            path.append('/').append(typeAndIds[i]).append(',').append(typeAndIds[i + 1]);
        }
        path.append('/');
        return path.toString();
    }

    /**
     * 把回调传入的实例 ID 解析为数字 ID。格式非法的 ID 记日志并跳过，不影响其余 ID 的处理。
     */
    protected List<Long> parseLongIds(List<String> ids) {
        List<Long> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            try {
                result.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                log.warn("Illegal instance id in IAM v4 callback, skip|id={}", id);
            }
        }
        return result;
    }
}
