package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.api.web.WebSearchToolsResource;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.web.vo.TaskLinkVO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebSearchToolsResourceImpl implements WebSearchToolsResource {
    private final StepInstanceService stepInstanceService;
    private final GseTaskService gseTaskService;
    private final ApplicationService applicationService;
    private final AppAuthService appAuthService;

    /**
     * 作业平台web访问地址，可配置多个，用","分隔
     */
    @Value("${job.web.url:}")
    private String jobWebUrl;

    /**
     * 链接地址
     */
    private final static String linkTemplate = "%s/api_execute/%s?stepInstanceId=%s&executeCount=%s&batch=%s";

    @Autowired
    public WebSearchToolsResourceImpl(StepInstanceService stepInstanceService,
                                      GseTaskService gseTaskService,
                                      ApplicationService applicationService,
                                      AppAuthService appAuthService) {
        this.stepInstanceService = stepInstanceService;
        this.gseTaskService = gseTaskService;
        this.applicationService = applicationService;
        this.appAuthService = appAuthService;
    }

    @Override
    public Response<TaskLinkVO> getTaskLink(String username, String gseTaskId) {
        AppResourceScope appResourceScope = JobContextUtil.getAppResourceScope();
        // 鉴权
        AuthResult authResult = appAuthService.auth(username, ActionId.ACCESS_BUSINESS, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        authResult = appAuthService.auth(username, ActionId.VIEW_HISTORY, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        GseTaskDTO gseTaskDTO = gseTaskService.getGseTaskByGseTaskId(gseTaskId);
        if (gseTaskDTO == null) {
            throw new InternalException(gseTaskId + " does not exist", ErrorCode.ILLEGAL_PARAM);
        }

        StepInstanceBaseDTO stepInstanceBase = stepInstanceService.getStepInstanceBase(gseTaskDTO.getStepInstanceId());
        ServiceApplicationDTO applicationDTO = applicationService.getAppById(stepInstanceBase.getAppId());
        TaskLinkVO taskLinkVO = new TaskLinkVO();
        taskLinkVO.setScopeType(applicationDTO.getScopeType());
        taskLinkVO.setScopeId(applicationDTO.getScopeId());
        taskLinkVO.setAppId(stepInstanceBase.getAppId());
        taskLinkVO.setJobInstanceId(stepInstanceBase.getTaskInstanceId());
        taskLinkVO.setStepInstanceId(gseTaskDTO.getStepInstanceId());
        taskLinkVO.setExecuteCount(gseTaskDTO.getExecuteCount());
        taskLinkVO.setBatch(gseTaskDTO.getBatch());
        taskLinkVO.setGseTaskId(gseTaskDTO.getGseTaskId());
        taskLinkVO.setLink(buildLink(taskLinkVO));
        return Response.buildSuccessResp(taskLinkVO);
    }

    /**
     * 拼接链接地址
     */
    private String buildLink(TaskLinkVO taskLinkVO) {
        if (jobWebUrl.indexOf(",") != -1) {
            String[] jobWebUrls = jobWebUrl.split(",");
            List<String> links = new ArrayList();
            for (String webUrl : jobWebUrls) {
                links.add(String.format(linkTemplate, webUrl, taskLinkVO.getJobInstanceId(),
                    taskLinkVO.getStepInstanceId(), taskLinkVO.getExecuteCount(), taskLinkVO.getBatch()));
            }
            return links.toString();
        } else {
            return String.format(linkTemplate, jobWebUrl, taskLinkVO.getJobInstanceId(),
                taskLinkVO.getStepInstanceId(), taskLinkVO.getExecuteCount(), taskLinkVO.getBatch());
        }
    }
}
