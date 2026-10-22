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
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiWhiteIpV4ResourceImpl;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiWhiteIpV4Validator;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.ActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateWhiteIpRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4ListWhiteIpRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4WhiteIpHostReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4WhiteIpScopeReq;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4WhiteIpPageDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4WhiteIpRecordDTO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPHostVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import com.tencent.bk.job.manage.service.WhiteIPService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenApiWhiteIpV4ResourceImplTest {

    private static final String USERNAME = "tester";
    private static final String APP_CODE = "bk_job";

    private WhiteIPService whiteIPService;
    private NoResourceScopeAuthService noResourceScopeAuthService;
    private ActionScopeDAO actionScopeDAO;
    private AppScopeMappingService appScopeMappingService;
    private OpenApiWhiteIpV4ResourceImpl resource;
    private User user;

    @BeforeEach
    void setUp() {
        whiteIPService = mock(WhiteIPService.class);
        noResourceScopeAuthService = mock(NoResourceScopeAuthService.class);
        actionScopeDAO = mock(ActionScopeDAO.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        MessageI18nService messageI18nService = mock(MessageI18nService.class);
        when(messageI18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        OpenApiWhiteIpV4Validator validator = new OpenApiWhiteIpV4Validator(
            actionScopeDAO,
            appScopeMappingService,
            messageI18nService
        );
        resource = new OpenApiWhiteIpV4ResourceImpl(
            whiteIPService,
            noResourceScopeAuthService,
            actionScopeDAO,
            validator
        );
        user = new User("default", USERNAME, USERNAME);
        when(noResourceScopeAuthService.authCreateWhiteList(user)).thenReturn(AuthResult.pass(user));
        when(noResourceScopeAuthService.authManageWhiteList(user)).thenReturn(AuthResult.pass(user));
        JobContextUtil.setUser(user);
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("没有新增白名单权限时抛出带鉴权结果的权限异常，不创建")
    void createRequiresPermission() {
        AuthResult denied = AuthResult.fail(user);
        when(noResourceScopeAuthService.authCreateWhiteList(user)).thenReturn(denied);

        assertThatThrownBy(() -> resource.createWhiteIp(USERNAME, APP_CODE, createRequest()))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).getAuthResult()).isSameAs(denied));

        verify(whiteIPService, never()).createWhiteIP(any(), any());
        verify(whiteIPService, never()).resolveWhiteIpHosts(any());
    }

    @Test
    @DisplayName("非法生效范围 code 是参数错误，不会变成枚举解析异常")
    void rejectIllegalActionScopeCode() {
        V4CreateWhiteIpRequest request = createRequest();
        request.setActionScopeList(Collections.singletonList("1"));

        assertIllegalField(() -> resource.createWhiteIp(USERNAME, APP_CODE, request), "action_scope_list");

        verify(actionScopeDAO, never()).getActionScopeByCode(any());
        verify(whiteIPService, never()).createWhiteIP(any(), any());
    }

    @Test
    @DisplayName("合法生效范围去重后换成数据库 ID，code 在库中无行则内部错误")
    void mapActionScopeCodesAndFailWhenRowMissing() {
        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(actionScope(1L, "SCRIPT_EXECUTE"));
        when(actionScopeDAO.getActionScopeByCode("FILE_DISTRIBUTION"))
            .thenReturn(actionScope(2L, "FILE_DISTRIBUTION"));
        when(whiteIPService.resolveWhiteIpHosts(any()))
            .thenReturn(Collections.singletonList(new HostIdWithMeta(8L, null)));
        when(whiteIPService.createWhiteIP(any(), any())).thenReturn(createdRecord());
        when(actionScopeDAO.getActionScopeByIds(any())).thenReturn(Arrays.asList(
            actionScope(1L, "SCRIPT_EXECUTE"),
            actionScope(2L, "FILE_DISTRIBUTION")
        ));
        V4CreateWhiteIpRequest request = createRequest();
        request.setAllScope(true);
        request.setScopeList(Collections.singletonList(scope("biz", "2")));
        request.setActionScopeList(Arrays.asList("SCRIPT_EXECUTE", "SCRIPT_EXECUTE", "FILE_DISTRIBUTION"));

        EsbV4Response<OpenApiV4WhiteIpRecordDTO> response = resource.createWhiteIp(USERNAME, APP_CODE, request);

        ArgumentCaptor<WhiteIPRecordCreateUpdateReq> captor =
            ArgumentCaptor.forClass(WhiteIPRecordCreateUpdateReq.class);
        verify(whiteIPService).createWhiteIP(eq(USERNAME), captor.capture());
        assertThat(captor.getValue().isAllScope()).isTrue();
        assertThat(captor.getValue().getScopeList()).isNull();
        assertThat(captor.getValue().getActionScopeIdList()).containsExactly(1L, 2L);
        assertThat(captor.getValue().getHostList()).extracting(HostIdWithMeta::getHostId).containsExactly(8L);
        verify(appScopeMappingService, never()).getAppIdByScope(anyString(), anyString());
        verify(actionScopeDAO, times(1)).getActionScopeByCode("SCRIPT_EXECUTE");
        assertThat(response.getData().getCloudAreaId()).isEqualTo(1L);
        assertThat(response.getData().getActionScopeList())
            .containsExactly("SCRIPT_EXECUTE", "FILE_DISTRIBUTION");
        assertThat(response.getData().getHostList().get(1).getCloudAreaId()).isEqualTo(9L);

        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(null);
        V4CreateWhiteIpRequest missingRow = createRequest();
        missingRow.setAllScope(true);
        missingRow.setActionScopeList(Collections.singletonList("SCRIPT_EXECUTE"));
        assertThatThrownBy(() -> resource.createWhiteIp(USERNAME, APP_CODE, missingRow))
            .isInstanceOf(InternalException.class)
            .satisfies(exception -> assertThat(((InternalException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    @DisplayName("业务范围不存在或为空时参数错误，不创建")
    void rejectMissingScope() {
        V4CreateWhiteIpRequest emptyScope = createRequest();
        emptyScope.setAllScope(false);
        emptyScope.setScopeList(null);
        emptyScope.setActionScopeList(Collections.singletonList("SCRIPT_EXECUTE"));
        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(actionScope(1L, "SCRIPT_EXECUTE"));
        assertIllegalField(() -> resource.createWhiteIp(USERNAME, APP_CODE, emptyScope), "scope_list");

        V4CreateWhiteIpRequest unknownScope = createRequest();
        unknownScope.setAllScope(false);
        unknownScope.setScopeList(Collections.singletonList(scope("biz", "2")));
        unknownScope.setActionScopeList(Collections.singletonList("SCRIPT_EXECUTE"));
        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(null);
        assertIllegalField(() -> resource.createWhiteIp(USERNAME, APP_CODE, unknownScope), "scope_list");
        verify(whiteIPService, never()).createWhiteIP(any(), any());
        verify(whiteIPService, never()).resolveWhiteIpHosts(any());
    }

    @Test
    @DisplayName("没有管理白名单权限时抛出带鉴权结果的权限异常，不查询")
    void listRequiresPermission() {
        AuthResult denied = AuthResult.fail(user);
        when(noResourceScopeAuthService.authManageWhiteList(user)).thenReturn(denied);

        assertThatThrownBy(() -> resource.listWhiteIp(USERNAME, APP_CODE, new V4ListWhiteIpRequest()))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).getAuthResult()).isSameAs(denied));

        verify(whiteIPService, never()).listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("不传分页时按 start=0、length=10 查询，且不传排序字段")
    void listUsesDefaultPage() {
        when(whiteIPService.listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageData<>(0, 10, 0L, Collections.emptyList()));

        OpenApiV4WhiteIpPageDTO page = resource.listWhiteIp(USERNAME, APP_CODE, new V4ListWhiteIpRequest()).getData();

        assertThat(page.getStart()).isEqualTo(0);
        assertThat(page.getLength()).isEqualTo(10);
        assertThat(page.getTotal()).isEqualTo(0L);
        assertThat(page.getData()).isEmpty();
        verify(whiteIPService).listWhiteIPRecord(
            eq(USERNAME), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10), isNull(), isNull()
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 201, -1})
    @DisplayName("页大小越界是参数错误，不查询")
    void rejectIllegalLength(int length) {
        V4ListWhiteIpRequest request = new V4ListWhiteIpRequest();
        request.setLength(length);

        assertIllegalField(() -> resource.listWhiteIp(USERNAME, APP_CODE, request), "length");
        verify(whiteIPService, never()).listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("起始偏移小于 0 是参数错误，不查询")
    void rejectNegativeStart() {
        V4ListWhiteIpRequest request = new V4ListWhiteIpRequest();
        request.setStart(-1);

        assertIllegalField(() -> resource.listWhiteIp(USERNAME, APP_CODE, request), "start");
        verify(whiteIPService, never()).listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("更新人排序缺省为降序；未指定排序字段时不把 order 传给查询")
    void mapOrderField() {
        when(whiteIPService.listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageData<>(0, 10, 0L, Collections.emptyList()));
        V4ListWhiteIpRequest byModifier = new V4ListWhiteIpRequest();
        byModifier.setOrderField("last_modifier");
        resource.listWhiteIp(USERNAME, APP_CODE, byModifier);
        verify(whiteIPService).listWhiteIPRecord(
            eq(USERNAME), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(0), eq(10), eq("last_modify_user"), eq(0)
        );

        V4ListWhiteIpRequest defaultOrder = new V4ListWhiteIpRequest();
        defaultOrder.setOrder(1);
        resource.listWhiteIp(USERNAME, APP_CODE, defaultOrder);
        verify(whiteIPService).listWhiteIPRecord(
            eq(USERNAME), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(0), eq(10), isNull(), isNull()
        );
    }

    @Test
    @DisplayName("非法排序字段、数字生效范围是参数错误")
    void rejectIllegalOrderAndNumericActionScope() {
        V4ListWhiteIpRequest illegalOrderField = new V4ListWhiteIpRequest();
        illegalOrderField.setOrderField("id");
        assertIllegalField(() -> resource.listWhiteIp(USERNAME, APP_CODE, illegalOrderField), "order_field");

        V4ListWhiteIpRequest numericScope = new V4ListWhiteIpRequest();
        numericScope.setActionScopeList(Collections.singletonList("1"));
        assertIllegalField(() -> resource.listWhiteIp(USERNAME, APP_CODE, numericScope), "action_scope_list");
        verify(whiteIPService, never()).listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("业务范围映射成 appId 字符串，业务名称条件保持为空")
    void mapScopeListToAppIdStr() {
        when(appScopeMappingService.getAppIdByScope("biz", "2")).thenReturn(20L);
        when(appScopeMappingService.getAppIdByScope("biz_set", "3")).thenReturn(30L);
        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(actionScope(1L, "SCRIPT_EXECUTE"));
        when(actionScopeDAO.getActionScopeByCode("FILE_DISTRIBUTION"))
            .thenReturn(actionScope(2L, "FILE_DISTRIBUTION"));
        when(whiteIPService.listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageData<>(0, 10, 0L, Collections.emptyList()));
        V4ListWhiteIpRequest request = new V4ListWhiteIpRequest();
        request.setIp("127.0.0.1,127.0.0.2");
        request.setScopeList(Arrays.asList(scope("biz", "2"), scope("biz_set", "3")));
        request.setActionScopeList(Arrays.asList("SCRIPT_EXECUTE", "FILE_DISTRIBUTION"));
        request.setCreator("admin");
        request.setLastModifier("admin");

        resource.listWhiteIp(USERNAME, APP_CODE, request);

        verify(whiteIPService).listWhiteIPRecord(
            eq(USERNAME),
            eq("127.0.0.1,127.0.0.2"),
            eq("20,30"),
            isNull(),
            eq("1,2"),
            eq("admin"),
            eq("admin"),
            eq(0),
            eq(10),
            isNull(),
            isNull()
        );
    }

    @Test
    @DisplayName("列表生效范围 code 合法但库中无行时是内部错误")
    void listActionScopeMissingRowIsInternalError() {
        when(actionScopeDAO.getActionScopeByCode("SCRIPT_EXECUTE")).thenReturn(null);
        V4ListWhiteIpRequest request = new V4ListWhiteIpRequest();
        request.setActionScopeList(Collections.singletonList("SCRIPT_EXECUTE"));

        assertThatThrownBy(() -> resource.listWhiteIp(USERNAME, APP_CODE, request))
            .isInstanceOf(InternalException.class)
            .satisfies(exception -> assertThat(((InternalException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR));
        verify(whiteIPService, never()).listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("列表记录使用汇总值作为管控区域，生效范围只返回 code")
    void listResponseUsesSummaryCloudAreaAndActionScopeCode() {
        WhiteIPRecordVO record = new WhiteIPRecordVO();
        record.setId(1001L);
        record.setCloudAreaId(99L);
        record.setAllScope(false);
        record.setHostList(Collections.singletonList(new WhiteIPHostVO(101L, 1L, "127.0.0.1", null)));
        record.setActionScopeList(Collections.singletonList(new ActionScopeVO(1L, "脚本执行", null, null, null, null)));
        ScopeVO deletedScope = new ScopeVO();
        deletedScope.setName("[Deleted:8]");
        record.setScopeList(Collections.singletonList(deletedScope));
        record.setRemark("remark");
        record.setCreator(USERNAME);
        record.setCreateTime(1735689600000L);
        record.setLastModifier(USERNAME);
        record.setLastModifyTime(1735689600000L);
        when(whiteIPService.listWhiteIPRecord(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageData<>(0, 10, 1L, Collections.singletonList(record)));
        when(actionScopeDAO.getActionScopeByIds(Collections.singletonList(1L)))
            .thenReturn(Collections.singletonList(actionScope(1L, "SCRIPT_EXECUTE")));

        OpenApiV4WhiteIpRecordDTO data = resource.listWhiteIp(USERNAME, APP_CODE, new V4ListWhiteIpRequest())
            .getData().getData().get(0);

        assertThat(data.getCloudAreaId()).isEqualTo(99L);
        assertThat(data.getHostList().get(0).getCloudAreaId()).isEqualTo(1L);
        assertThat(data.getActionScopeList()).containsExactly("SCRIPT_EXECUTE");
        assertThat(data.getScopeList()).hasSize(1);
        assertThat(data.getScopeList().get(0).getName()).isEqualTo("[Deleted:8]");
        assertThat(data.getScopeList().get(0).getScopeType()).isNull();
        assertThat(data.getScopeList().get(0).getScopeId()).isNull();
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

    private V4CreateWhiteIpRequest createRequest() {
        V4CreateWhiteIpRequest request = new V4CreateWhiteIpRequest();
        request.setAllScope(false);
        request.setRemark("remark");
        V4WhiteIpHostReq host = new V4WhiteIpHostReq();
        host.setHostId(8L);
        request.setHostList(Collections.singletonList(host));
        request.setActionScopeList(Collections.singletonList("SCRIPT_EXECUTE"));
        request.setScopeList(Collections.singletonList(scope("biz", "2")));
        return request;
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

    private WhiteIPRecordDTO createdRecord() {
        ApplicationDTO app = new ApplicationDTO();
        app.setName("demo-biz");
        app.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
        return new WhiteIPRecordDTO(
            1001L,
            Collections.singletonList(2L),
            "remark",
            Arrays.asList(
                new WhiteIPIPDTO(null, null, 1L, 101L, "127.0.0.1", null, null, null, null, null),
                new WhiteIPIPDTO(null, null, 9L, 102L, "127.0.0.2", null, null, null, null, null)
            ),
            Arrays.asList(
                new WhiteIPActionScopeDTO(null, null, 1L, null, null, null, null),
                new WhiteIPActionScopeDTO(null, null, 2L, null, null, null, null)
            ),
            USERNAME,
            1735689600000L,
            USERNAME,
            1735689600000L
        );
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
