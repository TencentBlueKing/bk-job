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

package com.tencent.bk.job.analysis.task.statistics.task;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.CurrentTenantStatisticsDAO;
import com.tencent.bk.job.analysis.dao.NoTenantStatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.tenant.TenantDTO;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public abstract class BaseStatisticsTask implements IStatisticsTask {
    protected final BasicServiceManager basicServiceManager;
    protected final CurrentTenantStatisticsDAO currentTenantStatisticsDAO;
    protected final NoTenantStatisticsDAO noTenantStatisticsDAO;
    protected final DSLContext dslContext;
    protected final TenantService tenantService;

    protected BaseStatisticsTask(BasicServiceManager basicServiceManager,
                                 CurrentTenantStatisticsDAO currentTenantStatisticsDAO,
                                 NoTenantStatisticsDAO noTenantStatisticsDAO,
                                 DSLContext dslContext,
                                 TenantService tenantService) {
        this.basicServiceManager = basicServiceManager;
        this.currentTenantStatisticsDAO = currentTenantStatisticsDAO;
        this.noTenantStatisticsDAO = noTenantStatisticsDAO;
        this.dslContext = dslContext;
        this.tenantService = tenantService;
    }

    public List<ServiceApplicationDTO> getTargetApps() {
        return basicServiceManager.getRemoteAppService().listAppsByTenantId(getCurrentTenantId());
    }

    public String getDayTimeStr(LocalDateTime dateTime) {
        return TimeUtil.getTimeStr(dateTime, StatisticsConstants.DATE_PATTERN);
    }

    public String getTodayTimeStr() {
        return TimeUtil.getTodayStartTimeStr(StatisticsConstants.DATE_PATTERN);
    }

    public LocalDateTime getTodayStartTime() {
        return TimeUtil.getTodayStartTime();
    }

    public LocalDateTime getTomorrowStartTime() {
        return getTodayStartTime().plusDays(1);
    }

    @Override
    public Boolean call() {
        List<TenantDTO> tenantDTOList = tenantService.listEnabledTenant();
        boolean result = true;
        for (TenantDTO tenantDTO : tenantDTOList) {
            result = result && callTaskWithTenant(tenantDTO.getId());
        }
        return result;
    }

    private Boolean callTaskWithTenant(String tenantId) {
        String className = getClass().getSimpleName();
        log.debug(className + " begin, tenantId={}", tenantId);
        StopWatch stopWatch = new StopWatch(className);
        stopWatch.start(className + " today task time consuming(tenantId=" + tenantId + ")");
        //每日统计
        try {
            // 设置当前租户ID
            JobContextUtil.setUser(new User(tenantId, null, null));
            // 需要统计跨天的任务
            // 统计昨天的数据
            genStatisticsByDay(LocalDateTime.now().minusDays(1));
            // 统计今天的数据
            genStatisticsByDay(LocalDateTime.now());
            return true;
        } catch (Throwable t) {
            log.warn(className + " error", t);
            return false;
        } finally {
            // 清理租户ID
            JobContextUtil.setUser(null);
            stopWatch.stop();
            log.info(stopWatch.prettyPrint());
        }
    }

    @Override
    public boolean isDataComplete(String targetDateStr) {
        return noTenantStatisticsDAO.existsStatisticsByDate(targetDateStr);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取含有租户ID的基础统计信息对象
     *
     * @return 统计信息对象
     */
    protected StatisticsDTO getBasicStatisticsDTO() {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setTenantId(JobContextUtil.getTenantId());
        return statisticsDTO;
    }

    protected String getCurrentTenantId() {
        return JobContextUtil.getTenantId();
    }
}
