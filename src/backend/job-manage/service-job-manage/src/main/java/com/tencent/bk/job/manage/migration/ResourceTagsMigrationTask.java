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

package com.tencent.bk.job.manage.migration;

import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.dao.ResourceTagDAO;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResourceTagsMigrationTask {
    private final ResourceTagDAO resourceTagDAO;
    private final TaskTemplateDAO taskTemplateDAO;
    private final ScriptDAO scriptDAO;

    @Autowired
    public ResourceTagsMigrationTask(ResourceTagDAO resourceTagDAO,
                                     TaskTemplateDAO taskTemplateDAO,
                                     ScriptDAO scriptDAO) {
        this.resourceTagDAO = resourceTagDAO;
        this.taskTemplateDAO = taskTemplateDAO;
        this.scriptDAO = scriptDAO;
    }

    public ServiceResponse<List<ResourceTagDTO>> execute() {
        log.info("Get resource tags for script/template start...");
        List<ResourceTagDTO> resourceTags = new ArrayList<>();
        addTemplateTags(resourceTags);
        addAppScriptTags(resourceTags);

        log.info("Batch save resource tags start...");
        resourceTagDAO.batchSaveResourceTags(resourceTags);
        log.info("Batch save resource tags successfully");

        return ServiceResponse.buildSuccessResp(resourceTags);
    }


    private void addTemplateTags(List<ResourceTagDTO> resourceTags) {
        Map<Long, List<Long>> templateTags =
            taskTemplateDAO.listAllTemplateTagsCompatible();
        int templateCount = templateTags.size();
        templateTags.forEach((templateId, tagIds) -> {
            if (CollectionUtils.isNotEmpty(tagIds)) {
                tagIds.forEach(tagId -> resourceTags.add(
                    new ResourceTagDTO(JobResourceTypeEnum.TEMPLATE.getValue(),
                        String.valueOf(templateId), tagId)));

            }
        });
        log.info("Build template tag records successfully, templateSize: {}", templateCount);
    }

    private void addAppScriptTags(List<ResourceTagDTO> resourceTags) {
        Map<String, List<Long>> scriptTags =
            scriptDAO.listAllScriptTagsCompatible();
        int scriptCount = scriptTags.size();
        scriptTags.forEach((scriptId, tagIds) -> {
            if (CollectionUtils.isNotEmpty(tagIds)) {
                tagIds.forEach(tagId -> resourceTags.add(
                    new ResourceTagDTO(JobResourceTypeEnum.APP_SCRIPT.getValue(), scriptId, tagId)));
            }
        });
        log.info("Build script tag records successfully, scriptSize: {}", scriptCount);
    }


}
