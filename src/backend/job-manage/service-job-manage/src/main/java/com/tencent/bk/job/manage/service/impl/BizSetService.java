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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.common.consts.globalsetting.GlobalSettingKeys;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BizSetService {
    private final GlobalSettingDAO globalSettingDAO;

    @Autowired
    public BizSetService(GlobalSettingDAO globalSettingDAO) {
        this.globalSettingDAO = globalSettingDAO;
    }

    /**
     * Job自有业务集是否已完全迁移至CMDB
     *
     * @return 布尔值
     */
    public boolean isBizSetMigratedToCMDB() {
        GlobalSettingDTO globalSettingDTO =
            globalSettingDAO.getGlobalSetting(GlobalSettingKeys.KEY_IS_BIZSET_MIGRATED_TO_CMDB);
        return globalSettingDTO != null && Boolean.parseBoolean(globalSettingDTO.getValue());
    }

    /**
     * 设置Job自有业务集是否已完全迁移至CMDB的迁移状态
     *
     * @param isMigrated 是否已完成迁移
     * @return 当前迁移状态
     */
    public boolean setBizSetMigratedToCMDB(Boolean isMigrated) {
        GlobalSettingDTO globalSettingDTO = GlobalSettingDTO.builder()
            .key(GlobalSettingKeys.KEY_IS_BIZSET_MIGRATED_TO_CMDB)
            .value(isMigrated.toString())
            .description("Updated at " + TimeUtil.getCurrentTimeStr())
            .build();
        globalSettingDAO.upsertGlobalSetting(globalSettingDTO);
        log.debug("set " + GlobalSettingKeys.KEY_IS_BIZSET_MIGRATED_TO_CMDB + ":" + isMigrated);
        return isBizSetMigratedToCMDB();
    }

}
