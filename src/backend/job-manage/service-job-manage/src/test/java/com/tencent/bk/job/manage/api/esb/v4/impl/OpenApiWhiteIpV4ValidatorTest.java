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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiWhiteIpV4Validator;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4WhiteIpScopeReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenApiWhiteIpV4ValidatorTest {

    private ActionScopeDAO actionScopeDAO;
    private AppScopeMappingService appScopeMappingService;
    private OpenApiWhiteIpV4Validator validator;

    @BeforeEach
    void setUp() {
        actionScopeDAO = mock(ActionScopeDAO.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        MessageI18nService messageI18nService = mock(MessageI18nService.class);
        when(messageI18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        validator = new OpenApiWhiteIpV4Validator(actionScopeDAO, appScopeMappingService, messageI18nService);
    }

    @Test
    @DisplayName("requireActionScopeIds：去重合法 code 并转为数据库 ID，空或非法抛参数错误，缺行抛内部错误")
    void testRequireActionScopeIds() {
        assertIllegalField(() -> validator.requireActionScopeIds(null), "action_scope_list");
        assertIllegalField(() -> validator.requireActionScopeIds(Collections.emptyList()), "action_scope_list");
        assertIllegalField(
            () -> validator.requireActionScopeIds(Collections.singletonList("1")),
            "action_scope_list"
        );

        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(actionScope(1L, "SCRIPT_EXECUTE"));
        when(actionScopeDAO.getActionScopeByCode("FILE_DISTRIBUTION"))
            .thenReturn(actionScope(2L, "FILE_DISTRIBUTION"));

        List<Long> ids = validator.requireActionScopeIds(
            Arrays.asList("SCRIPT_EXECUTE", "SCRIPT_EXECUTE", "FILE_DISTRIBUTION")
        );
        assertThat(ids).containsExactly(1L, 2L);
        verify(actionScopeDAO, times(1)).getActionScopeByCode("SCRIPT_EXECUTE");

        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(null);
        assertThatThrownBy(() -> validator.requireActionScopeIds(Collections.singletonList("SCRIPT_EXECUTE")))
            .isInstanceOf(InternalException.class)
            .satisfies(ex -> assertThat(((InternalException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    @DisplayName("optionalActionScopeIds：空列表返回空，非空时校验并转换")
    void testOptionalActionScopeIds() {
        assertThat(validator.optionalActionScopeIds(null)).isEmpty();
        assertThat(validator.optionalActionScopeIds(Collections.emptyList())).isEmpty();

        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(actionScope(1L, "SCRIPT_EXECUTE"));
        assertThat(validator.optionalActionScopeIds(Collections.singletonList("SCRIPT_EXECUTE")))
            .containsExactly(1L);
    }

    @Test
    @DisplayName("requireResourceScopes：校验业务范围列表非空、类型合法且在 Job 中存在")
    void testRequireResourceScopes() {
        assertIllegalField(() -> validator.requireResourceScopes(null), "scope_list");
        assertIllegalField(() -> validator.requireResourceScopes(Collections.emptyList()), "scope_list");
        assertIllegalField(
            () -> validator.requireResourceScopes(Collections.singletonList(scope("invalid", "2"))),
            "scope_list"
        );

        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(null);
        assertIllegalField(
            () -> validator.requireResourceScopes(Collections.singletonList(scope("biz", "2"))),
            "scope_list"
        );

        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(20L);
        List<ResourceScope> scopes = validator.requireResourceScopes(Collections.singletonList(scope("biz", "2")));
        assertThat(scopes).containsExactly(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
    }

    @Test
    @DisplayName("toAppIdStr：空列表返回 null，非空时拼接映射后的 appId")
    void testToAppIdStr() {
        assertThat(validator.toAppIdStr(null)).isNull();
        assertThat(validator.toAppIdStr(Collections.emptyList())).isNull();

        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(20L);
        when(appScopeMappingService.getAppIdByScope("biz_set", "3")).thenReturn(30L);
        assertThat(validator.toAppIdStr(Arrays.asList(scope("biz", "2"), scope("biz_set", "3"))))
            .isEqualTo("20,30");
    }

    @Test
    @DisplayName("normalizeStart：null 默认为 0，负数抛参数错误")
    void testNormalizeStart() {
        assertThat(validator.normalizeStart(null)).isEqualTo(0);
        assertThat(validator.normalizeStart(0)).isEqualTo(0);
        assertThat(validator.normalizeStart(15)).isEqualTo(15);
        assertIllegalField(() -> validator.normalizeStart(-1), "start");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 201, -1})
    @DisplayName("normalizeLength：null 默认为 10，1~200 合法，越界抛参数错误")
    void testNormalizeLength(int illegalLength) {
        assertThat(validator.normalizeLength(null)).isEqualTo(10);
        assertThat(validator.normalizeLength(1)).isEqualTo(1);
        assertThat(validator.normalizeLength(200)).isEqualTo(200);
        assertIllegalField(() -> validator.normalizeLength(illegalLength), "length");
    }

    @Test
    @DisplayName("normalizeOrderField 与 normalizeOrder：映射合法排序字段与方向，非法抛参数错误")
    void testNormalizeOrderFieldAndOrder() {
        assertThat(validator.normalizeOrderField(null)).isNull();
        assertThat(validator.normalizeOrderField("  ")).isNull();
        assertThat(validator.normalizeOrderField("last_modify_time")).isEqualTo("last_modify_time");
        assertThat(validator.normalizeOrderField("last_modifier")).isEqualTo("last_modify_user");
        assertIllegalField(() -> validator.normalizeOrderField("id"), "order_field");

        assertThat(validator.normalizeOrder(null, 1)).isNull();
        assertThat(validator.normalizeOrder("last_modify_time", null)).isEqualTo(0);
        assertThat(validator.normalizeOrder("last_modify_time", 0)).isEqualTo(0);
        assertThat(validator.normalizeOrder("last_modify_time", 1)).isEqualTo(1);
        assertIllegalField(() -> validator.normalizeOrder("last_modify_time", 2), "order");
    }

    private void assertIllegalField(ThrowingCallable callable, String field) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(InvalidParamException.class)
            .satisfies(exception -> {
                InvalidParamException invalidParamException = (InvalidParamException) exception;
                assertThat(invalidParamException.getErrorCode())
                    .isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
                assertThat(invalidParamException.getErrorParams()[0]).isEqualTo(field);
            });
    }

    private V4WhiteIpScopeReq scope(String type, String id) {
        V4WhiteIpScopeReq scope = new V4WhiteIpScopeReq();
        scope.setScopeType(type);
        scope.setScopeId(id);
        return scope;
    }

    private ActionScopeDTO actionScope(Long id, String code) {
        return new ActionScopeDTO(id, code, null, null, null, null, null, null);
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
