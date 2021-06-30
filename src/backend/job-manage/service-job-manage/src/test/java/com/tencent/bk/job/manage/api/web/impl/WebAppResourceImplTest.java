/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.cc.service.CloudAreaService;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.impl.AuthServiceImpl;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.WhiteIPService;
import com.tencent.bk.job.manage.service.impl.ApplicationFavorService;
import com.tencent.bk.job.manage.service.impl.ApplicationServiceImpl;
import com.tencent.bk.job.manage.service.impl.WhiteIPServiceImpl;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("业务管理API测试")
public class WebAppResourceImplTest {

    private static ApplicationService applicationService;
    private static ApplicationFavorService applicationFavorService;
    private static WebAppResourceImpl appResource;
    private static QueryAgentStatusClient queryAgentStatusClient;
    private static WhiteIPService whiteIPService;
    private static AuthService authService;
    private static CloudAreaService cloudAreaService;
    private static AccountService accountService;
    private static DSLContext dslContext;
    private static HostTopoDAO hostTopoDAO;

    @BeforeAll
    static void initTest() {
        ApplicationHostDAO applicationHostDAO = mock(ApplicationHostDAO.class);
        ApplicationInfoDAO applicationInfoDAO = mock(ApplicationInfoDAO.class);
        TopologyHelper topologyHelper = mock(TopologyHelper.class);
        queryAgentStatusClient = mock(QueryAgentStatusClient.class);
        whiteIPService = mock(WhiteIPServiceImpl.class);
        accountService = mock(AccountService.class);
        cloudAreaService = mock(CloudAreaService.class);
        accountService = mock(AccountService.class);
        dslContext = mock(DSLContext.class);
        hostTopoDAO = mock(HostTopoDAO.class);
        applicationService =
            new ApplicationServiceImpl(dslContext, applicationHostDAO, applicationInfoDAO, hostTopoDAO, topologyHelper,
                queryAgentStatusClient, cloudAreaService, accountService);
        authService = mock(AuthServiceImpl.class);
        applicationFavorService = mock(ApplicationFavorService.class);
        appResource = new WebAppResourceImpl(applicationService, applicationFavorService, authService);
    }

    @Test
    @DisplayName("获取业务列表，返回成功响应")
    public void whenListAppThenReturnSuccResp() {
        List<ApplicationInfoDTO> applicationInfoDTOList = applicationService.listAllAppsFromLocalDB();
        assertThat(!applicationInfoDTOList.isEmpty());
    }

}
