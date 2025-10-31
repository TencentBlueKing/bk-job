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

import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.index.IndexGreetingDAO;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.web.vo.common.AgentStatistics;
import com.tencent.bk.job.manage.model.web.vo.index.GreetingVO;
import com.tencent.bk.job.manage.model.web.vo.index.JobAndScriptStatistics;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import com.tencent.bk.job.manage.service.IndexService;
import com.tencent.bk.job.manage.service.agent.statistics.ScopeAgentStatisticsService;
import com.tencent.bk.job.manage.service.host.ScopeAgentStatusHostService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexServiceImpl implements IndexService {

    private final IndexGreetingDAO indexGreetingDAO;
    private final ScopeAgentStatusHostService scopeAgentStatusHostService;
    private final ScopeAgentStatisticsService scopeAgentStatisticsService;
    private final TaskTemplateService taskTemplateService;
    private final TaskTemplateDAO taskTemplateDAO;
    private final ScriptDAO scriptDAO;

    @Autowired
    public IndexServiceImpl(IndexGreetingDAO indexGreetingDAO,
                            ScopeAgentStatusHostService scopeAgentStatusHostService,
                            ScopeAgentStatisticsService scopeAgentStatisticsService,
                            TaskTemplateService taskTemplateService,
                            TaskTemplateDAO taskTemplateDAO,
                            ScriptDAO scriptDAO) {
        this.indexGreetingDAO = indexGreetingDAO;
        this.scopeAgentStatusHostService = scopeAgentStatusHostService;
        this.scopeAgentStatisticsService = scopeAgentStatisticsService;
        this.taskTemplateService = taskTemplateService;
        this.taskTemplateDAO = taskTemplateDAO;
        this.scriptDAO = scriptDAO;
    }

    @Override
    public List<GreetingVO> listGreeting(String username) {
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        int currentSeconds = TimeUtil.getCurrentSeconds();
        return indexGreetingDAO.listActiveIndexGreeting(currentSeconds).stream().map(it -> {
            String content;
            if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
                content = it.getContentEn();
                if (StringUtils.isBlank(content)) {
                    log.warn("{} i18n resource of {} is null or blank, use default", normalLang, it.getContent());
                    content = it.getContent();
                }
            } else {
                content = it.getContent();
            }
            return new GreetingVO(it.getId(), content.replace("${time}",
                    TimeUtil.getCurrentTimeStrWithDescription("HH:mm"))
                .replace("${username}", username), it.getPriority());
        }).collect(Collectors.toList());
    }

    @Override
    public AgentStatistics getAgentStatistics(String username, AppResourceScope appResourceScope) {
        return scopeAgentStatisticsService.getAgentStatistics(appResourceScope);
    }

    @Override
    public PageData<HostInfoVO> listHostsByAgentStatus(String username,
                                                       Long appId,
                                                       Integer status,
                                                       Long start,
                                                       Long pageSize) {
        Pair<Long, Long> pagePair = PageUtil.normalizePageParam(start, pageSize);
        start = pagePair.getLeft();
        pageSize = pagePair.getRight();
        PageData<ApplicationHostDTO> hostsPageData = scopeAgentStatusHostService.listHostsByAgentStatus(
            username,
            appId,
            status,
            start,
            pageSize
        );
        List<HostInfoVO> hostInfoVOList = hostsPageData.getData().stream()
            .filter(Objects::nonNull)
            .map(ApplicationHostDTO::toVO)
            .collect(Collectors.toList());
        return new PageData<>(
            start.intValue(),
            pageSize.intValue(),
            hostsPageData.getTotal(),
            hostInfoVOList
        );
    }

    @Override
    public PageData<String> listIPsByAgentStatus(String username, Long appId, Integer agentStatus, Long start,
                                                 Long pageSize) {
        PageData<HostInfoVO> hostInfoVOPageData = listHostsByAgentStatus(username, appId, agentStatus, start, pageSize);
        PageData<String> resultPageData = new PageData<>();
        resultPageData.setStart(hostInfoVOPageData.getStart());
        resultPageData.setPageSize(hostInfoVOPageData.getPageSize());
        resultPageData.setTotal(hostInfoVOPageData.getTotal());
        resultPageData.setData(hostInfoVOPageData.getData().stream()
            .map(it -> it.getCloudArea().getId() + ":" + it.getIp()).collect(Collectors.toList()));
        return resultPageData;
    }

    @Override
    public JobAndScriptStatistics getJobAndScriptStatistics(String username, Long appId) {
        return new JobAndScriptStatistics(taskTemplateDAO.getAllTemplateCount(appId),
            scriptDAO.countScriptByAppId(appId));
    }

    @Override
    public List<TaskTemplateVO> listMyFavorTasks(String username, Long appId, Long limit) {
        return taskTemplateService.getFavoredTemplateBasicInfo(
            appId, username).stream().map(TaskTemplateInfoDTO::toVO).collect(Collectors.toList());
    }
}
