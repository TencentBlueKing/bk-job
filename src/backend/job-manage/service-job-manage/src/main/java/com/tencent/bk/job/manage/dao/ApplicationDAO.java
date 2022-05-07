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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;


public interface ApplicationDAO {

    ApplicationDTO getCacheAppById(long appId);

    ApplicationDTO getAppById(long appId);

    List<Long> getBizIdsByOptDeptId(Long optDeptId);

    List<ApplicationDTO> listAppsByAppIds(List<Long> appIdList);

    List<ApplicationDTO> listBizAppsByBizIds(Collection<Long> bizIdList);

    List<Long> listAllBizAppBizIds();

    List<ApplicationDTO> listAllApps();

    List<ApplicationDTO> listAllAppsWithDeleted();

    List<ApplicationDTO> listAllBizApps();

    List<ApplicationDTO> listAllBizSetApps();

    List<ApplicationDTO> listAllBizAppsWithDeleted();

    List<ApplicationDTO> listAllBizSetAppsWithDeleted();

    @DeprecatedAppLogic
    List<ApplicationDTO> listAppsByType(AppTypeEnum appType);

    List<ApplicationDTO> listAppsByScopeType(ResourceScopeTypeEnum scopeType);

    Long insertApp(DSLContext dslContext, ApplicationDTO applicationDTO);

    Long insertAppWithSpecifiedAppId(DSLContext dslContext, ApplicationDTO applicationDTO);

    int updateApp(DSLContext dslContext, ApplicationDTO applicationDTO);

    /**
     * 恢复已删除的Job业务
     *
     * @param dslContext DB操作删上下文
     * @param appId      Job业务ID
     * @return 受影响数据行数
     */
    int restoreDeletedApp(DSLContext dslContext, long appId);

    /**
     * 对Job业务进行软删除
     *
     * @param dslContext DB操作删上下文
     * @param appId      Job业务ID
     * @return 受影响数据行数
     */
    int deleteAppByIdSoftly(DSLContext dslContext, long appId);

    int updateMaintainers(long appId, String maintainers);

    int updateSubBizIds(long appId, String subBizIds);

    Integer countApps();

    Integer countBizSetAppsWithDeleted();

    ApplicationDTO getAppByScope(ResourceScope scope);

    /**
     * 根据资源范围获取业务，包含已经被逻辑删除的业务
     *
     * @param scope 资源范围
     * @return 业务
     */
    ApplicationDTO getAppByScopeIncludingDeleted(ResourceScope scope);
}
