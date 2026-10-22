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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.CurrentTenantHostDAO;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPRecordDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIpHostLocator;
import com.tencent.bk.job.manage.model.web.request.chooser.host.HostIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhiteIPServiceImplResolveTest {

    private static final String TENANT_ID = "default";
    private static final String CLOUD_IP = "0:127.0.0.1";

    private ActionScopeDAO actionScopeDAO;
    private WhiteIPRecordDAO whiteIPRecordDAO;
    private ApplicationDAO applicationDAO;
    private MessageI18nService i18nService;
    private AppScopeMappingService appScopeMappingService;
    private CurrentTenantHostDAO currentTenantHostDAO;
    private IBizCmdbClient bizCmdbClient;
    private WhiteIPServiceImpl service;

    @BeforeEach
    void setUp() {
        actionScopeDAO = mock(ActionScopeDAO.class);
        whiteIPRecordDAO = mock(WhiteIPRecordDAO.class);
        applicationDAO = mock(ApplicationDAO.class);
        i18nService = mock(MessageI18nService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        currentTenantHostDAO = mock(CurrentTenantHostDAO.class);
        bizCmdbClient = mock(IBizCmdbClient.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        WhiteIpHostValidator whiteIpHostValidator = new WhiteIpHostValidator(
            currentTenantHostDAO,
            bizCmdbClient,
            i18nService
        );
        service = new WhiteIPServiceImpl(
            actionScopeDAO,
            whiteIPRecordDAO,
            applicationDAO,
            i18nService,
            appScopeMappingService,
            whiteIpHostValidator,
            bizCmdbClient
        );
        JobContextUtil.setUser(new User(TENANT_ID, "tester", "tester"));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("仅主机 ID 时直接采用该 ID，不查管控区域 IP")
    void resolveByHostIdOnly() {
        WhiteIpHostLocator locator = new WhiteIpHostLocator(8L, 0L, "127.0.0.1");

        List<HostIdWithMeta> hosts = service.resolveWhiteIpHosts(Collections.singletonList(locator));

        assertThat(hosts).extracting(HostIdWithMeta::getHostId).containsExactly(8L);
        assertThat(hosts.get(0).getMeta()).isNull();
        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
        verify(currentTenantHostDAO, never()).listHostInfoByHostIds(any());
    }

    @Test
    @DisplayName("同时有主机 ID 与 IP 时只用主机 ID")
    void hostIdTakesPriorityOverIp() {
        WhiteIpHostLocator locator = new WhiteIpHostLocator(8L, 3L, "127.0.0.2");

        List<HostIdWithMeta> hosts = service.resolveWhiteIpHosts(Collections.singletonList(locator));

        assertThat(hosts).extracting(HostIdWithMeta::getHostId).containsExactly(8L);
        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
    }

    @Test
    @DisplayName("主机 ID 小于 1 时参数错误，不回退 IP")
    void rejectNonPositiveHostIdWithoutIpFallback() {
        WhiteIpHostLocator locator = new WhiteIpHostLocator(0L, 0L, "127.0.0.1");

        assertIllegalHost(Collections.singletonList(locator));
        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
        verify(whiteIPRecordDAO, never()).insertWhiteIPRecord(any());
    }

    @Test
    @DisplayName("只传管控区域或只传 IP 时参数错误")
    void rejectIncompleteCloudIp() {
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, null)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, null, "127.0.0.1")));
        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
    }

    @Test
    @DisplayName("本地与 CMDB 是同一台主机时采用该主机，并且两边都会查询")
    void acceptCloudIpWhenLocalAndCmdbAgree() {
        when(currentTenantHostDAO.listHostInfoByCloudIps(Collections.singletonList(CLOUD_IP)))
            .thenReturn(Arrays.asList(host(5L), host(5L)));
        when(bizCmdbClient.listHostsByCloudIps(TENANT_ID, Collections.singletonList(CLOUD_IP)))
            .thenReturn(Collections.singletonList(host(5L)));

        List<HostIdWithMeta> hosts = service.resolveWhiteIpHosts(
            Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1"))
        );

        assertThat(hosts).extracting(HostIdWithMeta::getHostId).containsExactly(5L);
        verify(currentTenantHostDAO).listHostInfoByCloudIps(Collections.singletonList(CLOUD_IP));
        verify(bizCmdbClient).listHostsByCloudIps(TENANT_ID, Collections.singletonList(CLOUD_IP));
    }

    @Test
    @DisplayName("本地没有、CMDB 恰好一台时采用 CMDB 主机")
    void acceptCloudIpFromCmdbWhenLocalMisses() {
        when(currentTenantHostDAO.listHostInfoByCloudIps(any())).thenReturn(Collections.emptyList());
        when(bizCmdbClient.listHostsByCloudIps(any(), any())).thenReturn(Collections.singletonList(host(6L)));

        List<HostIdWithMeta> hosts = service.resolveWhiteIpHosts(
            Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1"))
        );

        assertThat(hosts).extracting(HostIdWithMeta::getHostId).containsExactly(6L);
    }

    @Test
    @DisplayName("本地唯一但 CMDB 没有或多台不一致时参数错误，不插入")
    void rejectCloudIpWhenCmdbDoesNotConfirmSingleHost() {
        when(currentTenantHostDAO.listHostInfoByCloudIps(any())).thenReturn(Collections.singletonList(host(5L)));
        when(bizCmdbClient.listHostsByCloudIps(any(), any())).thenReturn(Collections.emptyList());
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1")));

        when(bizCmdbClient.listHostsByCloudIps(any(), any())).thenReturn(Collections.singletonList(host(7L)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1")));

        when(currentTenantHostDAO.listHostInfoByCloudIps(any()))
            .thenReturn(Arrays.asList(host(5L), host(7L)));
        when(bizCmdbClient.listHostsByCloudIps(any(), any())).thenReturn(Collections.singletonList(host(5L)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1")));

        when(currentTenantHostDAO.listHostInfoByCloudIps(any())).thenReturn(Collections.emptyList());
        when(bizCmdbClient.listHostsByCloudIps(any(), any())).thenReturn(Arrays.asList(host(5L), host(7L)));
        assertIllegalHost(Collections.singletonList(new WhiteIpHostLocator(null, 0L, "127.0.0.1")));

        verify(whiteIPRecordDAO, never()).insertWhiteIPRecord(any());
        verify(bizCmdbClient, org.mockito.Mockito.atLeastOnce()).listHostsByCloudIps(any(), any());
    }

    @Test
    @DisplayName("解析成功后按主机 ID 去重并保持首次出现顺序")
    void dedupeResolvedHostIds() {
        List<HostIdWithMeta> hosts = service.resolveWhiteIpHosts(Arrays.asList(
            new WhiteIpHostLocator(8L, null, null),
            new WhiteIpHostLocator(9L, null, null),
            new WhiteIpHostLocator(8L, null, null)
        ));

        assertThat(hosts).extracting(HostIdWithMeta::getHostId).containsExactly(8L, 9L);
    }

    @Test
    @DisplayName("主机 ID 在本地和 CMDB 都不存在时失败，不查 IP，不插入")
    void rejectMissingHostIdWithoutIpLookupOrInsert() {
        when(currentTenantHostDAO.listHostInfoByHostIds(any())).thenReturn(Collections.emptyList());
        when(bizCmdbClient.listHostsByHostIds(any(), any())).thenReturn(Collections.emptyList());
        WhiteIPRecordCreateUpdateReq request = new WhiteIPRecordCreateUpdateReq();
        request.setAllScope(true);
        request.setHostList(Collections.singletonList(new HostIdWithMeta(99L, null)));
        request.setRemark("remark");
        request.setActionScopeIdList(Collections.singletonList(1L));

        assertThatThrownBy(() -> service.createWhiteIP("tester", request))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(exception -> assertThat(((InvalidParamException) exception).getErrorCode())
                .isEqualTo(ErrorCode.HOST_ID_NOT_EXIST_IN_CMDB));

        verify(currentTenantHostDAO, never()).listHostInfoByCloudIps(any());
        verify(bizCmdbClient, never()).listHostsByCloudIps(any(), any());
        verify(whiteIPRecordDAO, never()).insertWhiteIPRecord(any());
    }

    @Test
    @DisplayName("全部业务创建时忽略请求里的业务列表，目标业务为公共业务")
    void createAllScopeIgnoresScopeList() {
        when(currentTenantHostDAO.listHostInfoByHostIds(any()))
            .thenReturn(Collections.singletonList(host(5L)));
        when(whiteIPRecordDAO.insertWhiteIPRecord(any())).thenReturn(11L);
        when(whiteIPRecordDAO.getWhiteIPRecordById(11L)).thenReturn(new WhiteIPRecordDTO(
            11L,
            Collections.singletonList(0L),
            "remark",
            Collections.emptyList(),
            Collections.emptyList(),
            "tester",
            1L,
            "tester",
            1L
        ));
        when(applicationDAO.listAppsByAppIds(any())).thenReturn(Collections.emptyList());
        WhiteIPRecordCreateUpdateReq request = new WhiteIPRecordCreateUpdateReq();
        request.setAllScope(true);
        request.setScopeList(Collections.singletonList(new ResourceScope("biz", "2")));
        request.setHostList(Collections.singletonList(new HostIdWithMeta(5L, null)));
        request.setRemark("remark");
        request.setActionScopeIdList(Collections.singletonList(1L));

        service.createWhiteIP("tester", request);

        ArgumentCaptor<WhiteIPRecordDTO> captor = ArgumentCaptor.forClass(WhiteIPRecordDTO.class);
        verify(whiteIPRecordDAO).insertWhiteIPRecord(captor.capture());
        assertThat(captor.getValue().getAppIdList()).containsExactly(0L);
        verify(appScopeMappingService, never()).getAppIdByScopeList(any());
    }

    private void assertIllegalHost(List<WhiteIpHostLocator> locators) {
        assertThatThrownBy(() -> service.resolveWhiteIpHosts(locators))
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
