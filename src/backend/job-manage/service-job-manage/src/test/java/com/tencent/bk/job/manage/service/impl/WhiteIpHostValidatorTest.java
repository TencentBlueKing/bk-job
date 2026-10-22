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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIpHostLocator;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhiteIpHostValidatorTest {

    private static final String TENANT_ID = "default";
    private static final String CLOUD_IP = "0:127.0.0.1";

    private CurrentTenantHostDAO currentTenantHostDAO;
    private IBizCmdbClient bizCmdbClient;
    private WhiteIpHostValidator validator;

    @BeforeEach
    void setUp() {
        currentTenantHostDAO = mock(CurrentTenantHostDAO.class);
        bizCmdbClient = mock(IBizCmdbClient.class);
        MessageI18nService i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        validator = new WhiteIpHostValidator(currentTenantHostDAO, bizCmdbClient, i18nService);
        JobContextUtil.setUser(new User(TENANT_ID, "tester", "tester"));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("仅主机 ID 或同时有主机 ID 与 IP 时直接采用主机 ID 并去重")
    void resolveByHostIdAndDedupe() {
        List<HostIdWithMeta> hosts = validator.resolveWhiteIpHosts(Arrays.asList(
            new WhiteIpHostLocator(8L, 0L, "127.0.0.1"),
            new WhiteIpHostLocator(9L, null, null),
            new WhiteIpHostLocator(8L, 3L, "127.0.0.2")
        ));

        assertThat(hosts).extracting(HostIdWithMeta::getHostId).containsExactly(8L, 9L);
        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
    }

    @Test
    @DisplayName("非正数主机 ID、缺失管控区域或 IP、负数管控区域、非法 IPv4 均抛参数错误")
    void rejectInvalidLocatorParameters() {
        assertIllegalHost(Collections.singletonList(null));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(0L, 0L, "127.0.0.1")));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(-1L, null, null)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, null)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, null, "127.0.0.1")));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, -1L, "127.0.0.1")));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, "999.0.0.1")));
        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
    }

    @Test
    @DisplayName("管控区域+IP 在本地与 CMDB 唯一一致或本地缺失 CMDB 唯一命中时解析成功，否则抛参数错误")
    void resolveByCloudIpUniqueness() {
        when(currentTenantHostDAO.listHostInfoByCloudIps(Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.singletonList(host(5L)));
        when(bizCmdbClient.listHostsByCloudIps(TENANT_ID, Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.singletonList(host(5L)));
        assertThat(validator.resolveWhiteIpHosts(
            Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1"))
        )).extracting(HostIdWithMeta::getHostId).containsExactly(5L);

        when(currentTenantHostDAO.listHostInfoByCloudIps(Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.emptyList());
        when(bizCmdbClient.listHostsByCloudIps(TENANT_ID, Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.singletonList(host(6L)));
        assertThat(validator.resolveWhiteIpHosts(
            Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1"))
        )).extracting(HostIdWithMeta::getHostId).containsExactly(6L);

        when(currentTenantHostDAO.listHostInfoByCloudIps(Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.singletonList(host(5L)));
        when(bizCmdbClient.listHostsByCloudIps(TENANT_ID, Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.singletonList(host(7L)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1")));
    }

    @Test
    @DisplayName("validateAndLoadHosts：优先从本地加载，缺失回源 CMDB，仍缺失抛 HOST_ID_NOT_EXIST_IN_CMDB")
    void testValidateAndLoadHosts() {
        assertThat(validator.validateAndLoadHosts(Collections.emptyList())).isEmpty();

        when(currentTenantHostDAO.listHostInfoByHostIds(Arrays.asList(1L, 2L)))
            .thenReturn(Collections.singletonList(host(1L)));
        when(bizCmdbClient.listHostsByHostIds(TENANT_ID, Collections.singletonList(2L)))
            .thenReturn(Collections.singletonList(host(2L)));

        Map<Long, ApplicationHostDTO> loaded = validator.validateAndLoadHosts(Arrays.asList(1L, 2L));
        assertThat(loaded).containsOnlyKeys(1L, 2L);

        when(currentTenantHostDAO.listHostInfoByHostIds(Collections.singletonList(99L)))
            .thenReturn(Collections.emptyList());
        when(bizCmdbClient.listHostsByHostIds(TENANT_ID, Collections.singletonList(99L)))
            .thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> validator.validateAndLoadHosts(Collections.singletonList(99L)))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(ex -> assertThat(((InvalidParamException) ex).getErrorCode())
                .isEqualTo(ErrorCode.HOST_ID_NOT_EXIST_IN_CMDB));
    }

    private void assertIllegalHost(List<WhiteIpHostLocator> locators) {
        assertThatThrownBy(() -> validator.resolveWhiteIpHosts(locators))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(exception -> {
                InvalidParamException invalidParamException = (InvalidParamException) exception;
                assertThat(invalidParamException.getErrorCode())
                    .isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON);
                assertThat(invalidParamException.getErrorParams()[0]).isEqualTo("host_list");
            });
    }

    private ApplicationHostDTO host(Long hostId) {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(hostId);
        host.setCloudAreaId(0L);
        host.setIp("127.0.0.1");
        return host;
    }
}
