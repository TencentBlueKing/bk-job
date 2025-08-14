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

package com.tencent.bk.job.manage.service.artifactory;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import com.tentent.bk.job.common.api.artifactory.IRealProjectNameStore;

/**
 * 将真实仓库名称存放于全局配置中的实现
 */
public class GlobalSettingRealProjectNameStore implements IRealProjectNameStore {

    /**
     * 存储Job系统数据的租户ID，固定为default
     */
    private final String tenantIdForJobSystem = TenantIdConstants.DEFAULT_TENANT_ID;
    private final GlobalSettingDAO globalSettingDAO;

    public GlobalSettingRealProjectNameStore(GlobalSettingDAO globalSettingDAO) {
        this.globalSettingDAO = globalSettingDAO;
    }

    /**
     * 等待存储服务准备就绪，在job-manage服务中，Bean准备好即可立即使用
     *
     * @param maxWaitSeconds 最大等待时间
     * @return 是否准备就绪
     */
    @Override
    public boolean waitUntilStoreServiceReady(Integer maxWaitSeconds) {
        return true;
    }

    /**
     * 保存真实项目名称
     *
     * @param saveKey         用于存储真实项目名称的Key
     * @param realProjectName 真实项目名称
     */
    @Override
    public void saveRealProjectName(String saveKey, String realProjectName) {
        String description = "Job`s self-use project in artifactory, updated at " + TimeUtil.getCurrentTimeStr();
        GlobalSettingDTO globalSettingDTO = GlobalSettingDTO.builder()
            .tenantId(tenantIdForJobSystem)
            .key(saveKey)
            .value(realProjectName)
            .description(description)
            .build();
        globalSettingDAO.upsertGlobalSetting(globalSettingDTO);
    }

    /**
     * 查询真实项目名称
     *
     * @param saveKey 用于存储真实项目名称的Key
     * @return 真实项目名称
     */
    @Override
    public String queryRealProjectName(String saveKey) {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(
            saveKey,
            tenantIdForJobSystem
        );
        if (globalSettingDTO != null) {
            return globalSettingDTO.getValue();
        }
        return null;
    }
}
