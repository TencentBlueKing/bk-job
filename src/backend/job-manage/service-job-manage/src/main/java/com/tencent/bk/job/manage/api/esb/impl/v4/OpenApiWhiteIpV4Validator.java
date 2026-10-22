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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4WhiteIpScopeReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OpenApiWhiteIpV4Validator {

    private static final int DEFAULT_PAGE_LENGTH = 10;
    private static final int MAX_PAGE_LENGTH = 200;
    private static final String ORDER_FIELD_LAST_MODIFY_TIME = "last_modify_time";
    private static final String ORDER_FIELD_LAST_MODIFIER = "last_modifier";
    private static final String DAO_ORDER_LAST_MODIFY_USER = "last_modify_user";

    private static final String FIELD_SCOPE_LIST = "scope_list";
    private static final String FIELD_ACTION_SCOPE_LIST = "action_scope_list";
    private static final String FIELD_START = "start";
    private static final String FIELD_LENGTH = "length";
    private static final String FIELD_ORDER = "order";
    private static final String FIELD_ORDER_FIELD = "order_field";

    private static final String REASON_SCOPE_EMPTY = "whiteip.illegal.scope.empty";
    private static final String REASON_SCOPE_NOT_EXIST = "whiteip.illegal.scope.not.exist";
    private static final String REASON_SCOPE_TYPE = "whiteip.illegal.scope.type";
    private static final String REASON_ACTION_SCOPE = "whiteip.illegal.action.scope";
    private static final String REASON_START = "whiteip.illegal.page.start";
    private static final String REASON_LENGTH = "whiteip.illegal.page.length";
    private static final String REASON_ORDER = "whiteip.illegal.order";
    private static final String REASON_ORDER_FIELD = "whiteip.illegal.order.field";

    private final ActionScopeDAO actionScopeDAO;
    private final AppScopeMappingService appScopeMappingService;
    private final MessageI18nService messageI18nService;

    @Autowired
    public OpenApiWhiteIpV4Validator(ActionScopeDAO actionScopeDAO,
                                     AppScopeMappingService appScopeMappingService,
                                     MessageI18nService messageI18nService) {
        this.actionScopeDAO = actionScopeDAO;
        this.appScopeMappingService = appScopeMappingService;
        this.messageI18nService = messageI18nService;
    }

    public List<ResourceScope> requireResourceScopes(List<V4WhiteIpScopeReq> scopeList) {
        if (CollectionUtils.isEmpty(scopeList)) {
            throw illegalParam(FIELD_SCOPE_LIST, REASON_SCOPE_EMPTY);
        }
        List<ResourceScope> scopes = new ArrayList<>();
        for (V4WhiteIpScopeReq scope : scopeList) {
            requireAppId(scope);
            scopes.add(new ResourceScope(scope.getScopeType(), scope.getScopeId()));
        }
        return scopes;
    }

    public String toAppIdStr(List<V4WhiteIpScopeReq> scopeList) {
        if (CollectionUtils.isEmpty(scopeList)) {
            return null;
        }
        List<String> appIds = new ArrayList<>();
        for (V4WhiteIpScopeReq scope : scopeList) {
            appIds.add(String.valueOf(requireAppId(scope)));
        }
        return String.join(",", appIds);
    }

    public List<Long> requireActionScopeIds(List<String> actionScopeCodes) {
        if (CollectionUtils.isEmpty(actionScopeCodes)) {
            throw illegalParam(FIELD_ACTION_SCOPE_LIST, REASON_ACTION_SCOPE);
        }
        return loadActionScopeIds(normalizeActionScopeCodes(actionScopeCodes));
    }

    public List<Long> optionalActionScopeIds(List<String> actionScopeCodes) {
        if (CollectionUtils.isEmpty(actionScopeCodes)) {
            return Collections.emptyList();
        }
        return loadActionScopeIds(normalizeActionScopeCodes(actionScopeCodes));
    }

    public int normalizeStart(Integer start) {
        if (start == null) {
            return 0;
        }
        if (start < 0) {
            throw illegalParam(FIELD_START, REASON_START);
        }
        return start;
    }

    public int normalizeLength(Integer length) {
        if (length == null) {
            return DEFAULT_PAGE_LENGTH;
        }
        if (length < 1 || length > MAX_PAGE_LENGTH) {
            throw illegalParam(FIELD_LENGTH, REASON_LENGTH);
        }
        return length;
    }

    public String normalizeOrderField(String orderField) {
        if (isBlank(orderField)) {
            return null;
        }
        if (ORDER_FIELD_LAST_MODIFY_TIME.equals(orderField)) {
            return ORDER_FIELD_LAST_MODIFY_TIME;
        }
        if (ORDER_FIELD_LAST_MODIFIER.equals(orderField)) {
            return DAO_ORDER_LAST_MODIFY_USER;
        }
        throw illegalParam(FIELD_ORDER_FIELD, REASON_ORDER_FIELD);
    }

    public Integer normalizeOrder(String daoOrderField, Integer order) {
        if (daoOrderField == null) {
            return null;
        }
        if (order == null) {
            return 0;
        }
        if (order != 0 && order != 1) {
            throw illegalParam(FIELD_ORDER, REASON_ORDER);
        }
        return order;
    }

    private Long requireAppId(V4WhiteIpScopeReq scope) {
        if (scope == null || isBlank(scope.getScopeType()) || isBlank(scope.getScopeId())) {
            throw illegalParam(FIELD_SCOPE_LIST, REASON_SCOPE_NOT_EXIST);
        }
        if (!ResourceScopeTypeEnum.isValid(scope.getScopeType())) {
            throw illegalParam(FIELD_SCOPE_LIST, REASON_SCOPE_TYPE);
        }
        Long appId = appScopeMappingService.getAppIdByScope(scope.getScopeType(), scope.getScopeId());
        if (appId == null) {
            throw illegalParam(FIELD_SCOPE_LIST, REASON_SCOPE_NOT_EXIST);
        }
        return appId;
    }

    private Set<String> normalizeActionScopeCodes(List<String> actionScopeCodes) {
        Set<String> codes = new LinkedHashSet<>();
        for (String code : actionScopeCodes) {
            if (!isAllowedActionScope(code)) {
                throw illegalParam(FIELD_ACTION_SCOPE_LIST, REASON_ACTION_SCOPE);
            }
            codes.add(code);
        }
        return codes;
    }

    private boolean isAllowedActionScope(String code) {
        return ActionScopeEnum.SCRIPT_EXECUTE.name().equals(code)
            || ActionScopeEnum.FILE_DISTRIBUTION.name().equals(code);
    }

    private List<Long> loadActionScopeIds(Set<String> codes) {
        List<Long> ids = new ArrayList<>();
        for (String code : codes) {
            ActionScopeDTO actionScope = actionScopeDAO.getActionScopeByCode(code);
            if (actionScope == null || actionScope.getId() == null) {
                log.error("cannot find action scope in db by code {}, please check init data", code);
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            ids.add(actionScope.getId());
        }
        return ids;
    }

    private InvalidParamException illegalParam(String field, String reasonKey) {
        return new InvalidParamException(
            ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
            new Object[]{field, messageI18nService.getI18n(reasonKey)}
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
