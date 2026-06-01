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

package com.tencent.bk.job.execute.api.op;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.execute.dao.CallbackUrlWhiteInfoDAO;
import com.tencent.bk.job.execute.model.CallbackUrlWhiteInfoDTO;
import com.tencent.bk.job.execute.model.op.req.BatchAddCallbackUrlWhitelistReq;
import com.tencent.bk.job.execute.model.op.req.BatchDeleteCallbackUrlWhitelistReq;
import com.tencent.bk.job.execute.model.op.vo.CallbackUrlWhitelistVO;
import com.tencent.bk.job.execute.service.validation.CallbackUrlValidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackUrlWhitelistOpResourceImplTest {

    private CallbackUrlWhiteInfoDAO dao;
    private CallbackUrlValidateService validateService;
    private CallbackUrlWhitelistOpResourceImpl resource;

    @BeforeEach
    void setUp() {
        dao = mock(CallbackUrlWhiteInfoDAO.class);
        validateService = mock(CallbackUrlValidateService.class);
        resource = new CallbackUrlWhitelistOpResourceImpl(dao, validateService);

        // Response 构造时会通过 I18nUtil -> ApplicationContextRegister 反查 MessageI18nService，
        // 单测中注入一个最小可用的 ApplicationContext，避免 NPE；
        // getBean(MessageI18nService.class) 默认返回 null 由 I18nUtil 内部兜底为空串。
        ApplicationContext mockContext = mock(ApplicationContext.class);
        new ApplicationContextRegister().setApplicationContext(mockContext);
    }

    private BatchAddCallbackUrlWhitelistReq.Item item(String baseUrl, String desc) {
        BatchAddCallbackUrlWhitelistReq.Item it = new BatchAddCallbackUrlWhitelistReq.Item();
        it.setBaseUrl(baseUrl);
        it.setDescription(desc);
        return it;
    }

    @Test
    @DisplayName("batchAdd - 空入参抛 ILLEGAL_PARAM")
    void batchAddRejectsEmpty() {
        BatchAddCallbackUrlWhitelistReq req = new BatchAddCallbackUrlWhitelistReq();
        req.setItems(Collections.emptyList());
        assertThatThrownBy(() -> resource.batchAdd("admin", req))
            .isInstanceOf(InvalidParamException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ILLEGAL_PARAM);

        assertThatThrownBy(() -> resource.batchAdd("admin", null))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("batchAdd - 每条都过格式校验；任一不通过整体抛异常")
    void batchAddRunsFormatValidation() {
        BatchAddCallbackUrlWhitelistReq req = new BatchAddCallbackUrlWhitelistReq();
        req.setItems(Arrays.asList(
            item("http://a.com/", "ok"),
            item("ftp://b/", "bad")
        ));
        doThrow(new InvalidParamException(ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL, "ftp://b/"))
            .when(validateService).validateWhitelistBaseUrl("ftp://b/");

        assertThatThrownBy(() -> resource.batchAdd("admin", req))
            .isInstanceOf(InvalidParamException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL);

        verify(dao, never()).batchInsert(any());
        verify(validateService, never()).invalidateCache();
    }

    @Test
    @DisplayName("batchAdd - 已存在的 baseUrl 拒绝并提示")
    void batchAddDetectsExisting() {
        BatchAddCallbackUrlWhitelistReq req = new BatchAddCallbackUrlWhitelistReq();
        req.setItems(Arrays.asList(
            item("http://a.com/", "x"),
            item("http://b.com/", "y")
        ));
        when(dao.filterExistingBaseUrls(anyCollection()))
            .thenReturn(Collections.singletonList("http://a.com/"));

        assertThatThrownBy(() -> resource.batchAdd("admin", req))
            .isInstanceOf(InvalidParamException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CALLBACK_URL_WHITELIST_ALREADY_EXISTS);

        verify(dao, never()).batchInsert(any());
    }

    @Test
    @DisplayName("batchAdd - 正常入库后调用 invalidateCache 并返回受影响条数")
    void batchAddHappyPath() {
        BatchAddCallbackUrlWhitelistReq req = new BatchAddCallbackUrlWhitelistReq();
        req.setItems(Arrays.asList(
            item("http://a.com/", "x"),
            item("http://b.com/", "y")
        ));
        when(dao.filterExistingBaseUrls(anyCollection())).thenReturn(Collections.emptyList());
        when(dao.batchInsert(any())).thenReturn(2);

        Response<Integer> resp = resource.batchAdd("admin", req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo(2);
        verify(validateService).invalidateCache();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CallbackUrlWhiteInfoDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(dao).batchInsert(captor.capture());
        List<CallbackUrlWhiteInfoDTO> inserted = captor.getValue();
        assertThat(inserted).hasSize(2);
        assertThat(inserted).extracting(CallbackUrlWhiteInfoDTO::getBaseUrl)
            .containsExactlyInAnyOrder("http://a.com/", "http://b.com/");
        assertThat(inserted).allSatisfy(dto -> {
            assertThat(dto.getCreator()).isEqualTo("admin");
            assertThat(dto.getLastModifyUser()).isEqualTo("admin");
        });
    }

    @Test
    @DisplayName("list - 透传 DAO 分页结果并转 VO，时间字段格式化为字符串")
    void listConvertsToVO() {
        CallbackUrlWhiteInfoDTO dto = new CallbackUrlWhiteInfoDTO();
        dto.setId(1L);
        dto.setBaseUrl("http://a.com/");
        dto.setDescription("desc");
        dto.setCreator("alice");
        dto.setLastModifyUser("bob");
        dto.setCreateTime(LocalDateTime.of(2026, 5, 18, 10, 0, 0));
        dto.setLastModifyTime(LocalDateTime.of(2026, 5, 19, 11, 0, 0));

        PageData<CallbackUrlWhiteInfoDTO> pageData = new PageData<>();
        pageData.setStart(0);
        pageData.setPageSize(10);
        pageData.setTotal(1L);
        pageData.setData(Collections.singletonList(dto));
        when(dao.listByPage(0, 10)).thenReturn(pageData);

        Response<PageData<CallbackUrlWhitelistVO>> resp = resource.list("admin", 0, 10);

        assertThat(resp.isSuccess()).isTrue();
        PageData<CallbackUrlWhitelistVO> data = resp.getData();
        assertThat(data.getTotal()).isEqualTo(1L);
        assertThat(data.getData()).hasSize(1);
        CallbackUrlWhitelistVO vo = data.getData().get(0);
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getBaseUrl()).isEqualTo("http://a.com/");
        assertThat(vo.getCreator()).isEqualTo("alice");
        assertThat(vo.getLastModifyUser()).isEqualTo("bob");
        assertThat(vo.getCreateTime()).isEqualTo("2026-05-18 10:00:00");
        assertThat(vo.getLastModifyTime()).isEqualTo("2026-05-19 11:00:00");
    }

    @Test
    @DisplayName("batchDelete - 空入参抛 ILLEGAL_PARAM")
    void batchDeleteRejectsEmpty() {
        BatchDeleteCallbackUrlWhitelistReq req = new BatchDeleteCallbackUrlWhitelistReq();
        req.setIdList(Collections.emptyList());
        assertThatThrownBy(() -> resource.batchDelete("admin", req))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> resource.batchDelete("admin", null))
            .isInstanceOf(InvalidParamException.class);
        verify(dao, never()).deleteByIds(anyCollection());
    }

    @Test
    @DisplayName("batchDelete - 透传 ID 列表并 invalidateCache")
    void batchDeleteHappyPath() {
        BatchDeleteCallbackUrlWhitelistReq req = new BatchDeleteCallbackUrlWhitelistReq();
        req.setIdList(Arrays.asList(1L, 2L, 3L));
        when(dao.deleteByIds(anyCollection())).thenReturn(3);

        Response<Integer> resp = resource.batchDelete("admin", req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo(3);
        verify(dao).deleteByIds(req.getIdList());
        verify(validateService, atLeastOnce()).invalidateCache();
    }
}
