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

package com.tencent.bk.job.manage.model.dto.converter;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagVO;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

/**
 * 脚本DTO转换器
 */
public class ScriptConverter {
    public static ScriptVO convertToScriptVO(ScriptDTO script) {
        if (script == null) {
            return null;
        }
        ScriptVO scriptVO = new ScriptVO();
        if (script.getAppId() != null && !script.getAppId().equals(PUBLIC_APP_ID)) {
            AppScopeMappingService appScopeMappingService =
                ApplicationContextRegister.getBean(AppScopeMappingService.class);
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(script.getAppId());
            scriptVO.setScopeType(resourceScope.getType().getValue());
            scriptVO.setScopeId(resourceScope.getId());
        }

        scriptVO.setLastModifyUser(script.getLastModifyUser());
        scriptVO.setCategory(script.getCategory());
        scriptVO.setContent(script.getContent());
        if (null != script.getCreateTime()) {
            scriptVO.setCreateTime(script.getCreateTime());
        }
        scriptVO.setCreator(script.getCreator());
        scriptVO.setScriptVersionId(script.getScriptVersionId());
        scriptVO.setId(script.getId());
        scriptVO.setPublicScript(script.isPublicScript());
        if (null != script.getLastModifyTime()) {
            scriptVO.setLastModifyTime(script.getLastModifyTime());
        }
        scriptVO.setName(script.getName());
        scriptVO.setType(script.getType());
        scriptVO.setVersion(script.getVersion());
        scriptVO.setStatus(script.getStatus());
        scriptVO.setVersionDesc(script.getVersionDesc());
        scriptVO.setDescription(script.getDescription());
        if (script.getTags() != null && !script.getTags().isEmpty()) {
            List<TagVO> tagVOS = new ArrayList<>();
            for (TagDTO tagDTO : script.getTags()) {
                TagVO tagVO = new TagVO();
                tagVO.setId(tagDTO.getId());
                tagVO.setName(tagDTO.getName());
                tagVOS.add(tagVO);
            }
            scriptVO.setTags(tagVOS);
        }
        return scriptVO;
    }

    public static BasicScriptVO convertToBasicScriptVO(ScriptDTO script) {
        if (script == null) {
            return null;
        }
        BasicScriptVO scriptVO = new BasicScriptVO();
        if (script.getAppId() != null && !script.getAppId().equals(PUBLIC_APP_ID)) {
            AppScopeMappingService appScopeMappingService =
                ApplicationContextRegister.getBean(AppScopeMappingService.class);
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(script.getAppId());
            scriptVO.setScopeType(resourceScope.getType().getValue());
            scriptVO.setScopeId(resourceScope.getId());
        }
        scriptVO.setCategory(script.getCategory());
        scriptVO.setScriptVersionId(script.getScriptVersionId());
        scriptVO.setId(script.getId());
        scriptVO.setPublicScript(script.isPublicScript());
        scriptVO.setName(script.getName());
        scriptVO.setType(script.getType());
        scriptVO.setVersion(script.getVersion());
        scriptVO.setStatus(script.getStatus());
        return scriptVO;
    }


    public static ServiceScriptDTO convertToServiceScriptDTO(ScriptDTO script) {
        if (script == null) {
            return null;
        }
        ServiceScriptDTO scriptDTO = new ServiceScriptDTO();
        scriptDTO.setAppId(script.getAppId());
        scriptDTO.setLastModifyUser(script.getLastModifyUser());
        scriptDTO.setCategory(script.getCategory());
        scriptDTO.setContent(script.getContent());
        if (null != script.getCreateTime()) {
            scriptDTO.setCreateTime(script.getCreateTime());
        }
        scriptDTO.setCreator(script.getCreator());
        scriptDTO.setScriptVersionId(script.getScriptVersionId());
        scriptDTO.setId(script.getId());
        scriptDTO.setPublicScript(script.isPublicScript());
        if (null != script.getLastModifyTime()) {
            scriptDTO.setLastModifyTime(script.getLastModifyTime());
        }
        scriptDTO.setName(script.getName());
        scriptDTO.setType(script.getType());
        scriptDTO.setVersion(script.getVersion());
        scriptDTO.setStatus(script.getStatus());
        scriptDTO.setVersionDesc(script.getVersionDesc());
        scriptDTO.setDescription(script.getDescription());
        return scriptDTO;
    }
}
