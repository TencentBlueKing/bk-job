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

package com.tencent.bk.job.file_gateway.api.inner;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.file_gateway.dao.filesource.SpecifiedTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceAvailabilityDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 文件源可用性批量查询接口单测
 */
@ExtendWith(MockitoExtension.class)
class ServiceFileSourceResourceImplTest {

    private static final String TENANT_ID = "tenant-1";
    private static final long APP_ID = 100L;

    @Mock
    private SpecifiedTenantFileSourceDAO specifiedTenantFileSourceDAO;

    @InjectMocks
    private ServiceFileSourceResourceImpl resource;

    private MockedStatic<ApplicationContextRegister> applicationContextRegisterMock;

    @BeforeEach
    void setUp() {
        // InternalResponse 构造时会经 I18nUtil 去容器里取 MessageI18nService，纯单测下需要顶一个
        applicationContextRegisterMock = Mockito.mockStatic(ApplicationContextRegister.class);
        applicationContextRegisterMock.when(() -> ApplicationContextRegister.getBean(MessageI18nService.class))
            .thenReturn(Mockito.mock(MessageI18nService.class));
    }

    @AfterEach
    void tearDown() {
        applicationContextRegisterMock.close();
    }

    @Test
    @DisplayName("入参为空时直接返回空列表，不查库")
    void emptyInput_noQuery() {
        InternalResponse<List<ServiceFileSourceAvailabilityDTO>> resp =
            resource.checkFileSourceAvailability(TENANT_ID, APP_ID, Collections.emptyList());

        assertThat(resp.getData()).isEmpty();
        verifyNoInteractions(specifiedTenantFileSourceDAO);
    }

    @Test
    @DisplayName("可见且启用 → available；可见但禁用 → inAppScope 为 true 但 available 为 false")
    void visibleFileSource_availabilityDependsOnEnable() {
        when(specifiedTenantFileSourceDAO.listFileSourceBasicInfoByIds(anyString(), any()))
            .thenReturn(Arrays.asList(basicInfo(1, APP_ID, "own", true), basicInfo(2, APP_ID, "own2", false)));
        when(specifiedTenantFileSourceDAO.listFileSourceIdsInAppScope(anyString(), anyLong(), any()))
            .thenReturn(new HashSet<>(Arrays.asList(1, 2)));

        List<ServiceFileSourceAvailabilityDTO> result =
            resource.checkFileSourceAvailability(TENANT_ID, APP_ID, Arrays.asList(1, 2)).getData();

        assertThat(byId(result, 1)).satisfies(availability -> {
            assertThat(availability.isInAppScope()).isTrue();
            assertThat(availability.isAvailable()).isTrue();
            assertThat(availability.getAlias()).isEqualTo("own");
        });
        assertThat(byId(result, 2)).satisfies(availability -> {
            assertThat(availability.isInAppScope()).isTrue();
            assertThat(availability.isAvailable()).isFalse();
        });
    }

    @Test
    @DisplayName("存在但未共享给该业务 → exists 为 true，inAppScope 为 false")
    void existsButNotShared_notInAppScope() {
        when(specifiedTenantFileSourceDAO.listFileSourceBasicInfoByIds(anyString(), any()))
            .thenReturn(Collections.singletonList(basicInfo(1, APP_ID + 1, "other", true)));
        when(specifiedTenantFileSourceDAO.listFileSourceIdsInAppScope(anyString(), anyLong(), any()))
            .thenReturn(Collections.emptySet());

        ServiceFileSourceAvailabilityDTO availability =
            resource.checkFileSourceAvailability(TENANT_ID, APP_ID, Collections.singletonList(1))
                .getData().get(0);

        assertThat(availability.exists()).isTrue();
        assertThat(availability.isInAppScope()).isFalse();
        assertThat(availability.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("不存在或跨租户 → exists 为 false，归属、别名、启用状态均为 null")
    void notExists_allDetailsNull() {
        when(specifiedTenantFileSourceDAO.listFileSourceBasicInfoByIds(anyString(), any()))
            .thenReturn(Collections.emptyList());
        when(specifiedTenantFileSourceDAO.listFileSourceIdsInAppScope(anyString(), anyLong(), any()))
            .thenReturn(Collections.emptySet());

        ServiceFileSourceAvailabilityDTO availability =
            resource.checkFileSourceAvailability(TENANT_ID, APP_ID, Collections.singletonList(999))
                .getData().get(0);

        assertThat(availability.exists()).isFalse();
        assertThat(availability.getOwnerAppId()).isNull();
        assertThat(availability.getAlias()).isNull();
        assertThat(availability.getEnable()).isNull();
    }

    @Test
    @DisplayName("重复 ID 去重后每个 ID 只返回一条结果")
    void duplicatedIds_deduplicated() {
        when(specifiedTenantFileSourceDAO.listFileSourceBasicInfoByIds(anyString(), any()))
            .thenReturn(Collections.singletonList(basicInfo(1, APP_ID, "own", true)));
        when(specifiedTenantFileSourceDAO.listFileSourceIdsInAppScope(anyString(), anyLong(), any()))
            .thenReturn(new HashSet<>(Collections.singletonList(1)));

        List<ServiceFileSourceAvailabilityDTO> result =
            resource.checkFileSourceAvailability(TENANT_ID, APP_ID, Arrays.asList(1, 1, 1)).getData();

        assertThat(result).hasSize(1);
    }

    private static ServiceFileSourceAvailabilityDTO byId(List<ServiceFileSourceAvailabilityDTO> list, int id) {
        return list.stream().filter(item -> item.getId() == id).findFirst().orElseThrow(AssertionError::new);
    }

    private static FileSourceBasicInfoDTO basicInfo(Integer id, Long appId, String alias, Boolean enable) {
        FileSourceBasicInfoDTO basicInfo = new FileSourceBasicInfoDTO();
        basicInfo.setId(id);
        basicInfo.setAppId(appId);
        basicInfo.setAlias(alias);
        basicInfo.setEnable(enable);
        return basicInfo;
    }
}
