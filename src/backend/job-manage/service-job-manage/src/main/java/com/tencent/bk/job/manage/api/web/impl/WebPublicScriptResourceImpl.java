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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.web.WebPublicScriptResource;
import com.tencent.bk.job.manage.api.web.WebScriptResource;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptSyncReq;
import com.tencent.bk.job.manage.model.web.request.ScriptTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class WebPublicScriptResourceImpl implements WebPublicScriptResource {
    /**
     * 脚本处理的通用服务，与普通脚本不同的是，公共脚本的业务ID=0
     * 鉴权在通用脚本逻辑中处理
     */
    private final WebScriptResource scriptResourceProxy;

    @Autowired
    public WebPublicScriptResourceImpl(@Qualifier("webScriptResourceImpl") WebScriptResource scriptResourceProxy) {
        this.scriptResourceProxy = scriptResourceProxy;
    }

    @Override
    public Response<ScriptVO> getScriptVersionDetail(String username, Long scriptVersionId) {
        return scriptResourceProxy.getScriptVersionDetail(username, null, null, null, scriptVersionId);
    }

    @Override
    public Response<ScriptVO> getScript(String username, String scriptId) {
        return scriptResourceProxy.getScript(username, null, null, null, scriptId);
    }

    @Override
    public Response<ScriptVO> getScriptBasicInfo(String username, String scriptId) {
        return scriptResourceProxy.getScriptBasicInfo(username, null, null, null, scriptId);
    }

    @Override
    public Response<ScriptVO> getOnlineScriptVersionByScriptId(String username, String scriptId) {
        return scriptResourceProxy.getOnlineScriptVersionByScriptId(username, null, null,
            null, scriptId, true);
    }

    @Override
    public Response<PageData<ScriptVO>> listPageScript(String username,
                                                       String name,
                                                       Integer type,
                                                       String tags,
                                                       Long panelTag,
                                                       Integer panelType,
                                                       String creator,
                                                       String lastModifyUser,
                                                       String scriptId,
                                                       Integer start,
                                                       Integer pageSize,
                                                       String orderField,
                                                       Integer order) {
        return scriptResourceProxy.listPageScript(username, null, null, null, true, name,
            type, tags, panelTag, panelType, creator, lastModifyUser, scriptId,
            start, pageSize, orderField, order);
    }

    @Override
    public Response<List<ScriptVO>> listScriptBasicInfo(String username, List<String> scriptIds) {
        return scriptResourceProxy.listScriptBasicInfo(username, null, null, null, scriptIds);
    }

    @Override
    public Response<List<ScriptVO>> listScriptVersion(String username, String scriptId) {
        return scriptResourceProxy.listScriptVersion(username, null, null, null, scriptId);
    }

    @Override
    public Response listAppScriptNames(String username, String scriptName) {
        return scriptResourceProxy.listAppScriptNames(username, null, null, null, scriptName);
    }

    @Override
    public Response<List<BasicScriptVO>> listScriptOnline(String username) {
        return scriptResourceProxy.listScriptOnline(username, null, null, null, true);
    }

    @Override
    public Response updateScriptInfo(String username, String scriptId, ScriptInfoUpdateReq scriptInfoUpdateReq) {
        return scriptResourceProxy.updateScriptInfo(username, null, null, null, scriptId,
            scriptInfoUpdateReq);
    }

    @Override
    public Response<ScriptVO> saveScript(String username, ScriptCreateUpdateReq scriptCreateUpdateReq) {
        return scriptResourceProxy.saveScript(username, null, null, null, scriptCreateUpdateReq);
    }

    @Override
    public Response publishScriptVersion(String username, String scriptId, Long scriptVersionId) {
        return scriptResourceProxy.publishScriptVersion(username, null, null, null, scriptId,
            scriptVersionId);
    }

    @Override
    public Response disableScriptVersion(String username, String scriptId, Long scriptVersionId) {
        return scriptResourceProxy.disableScriptVersion(username, null, null, null, scriptId,
            scriptVersionId);
    }

    @Override
    public Response deleteScriptByScriptId(String username, String scriptId) {
        return scriptResourceProxy.deleteScriptByScriptId(username, null, null, null, scriptId);
    }

    @Override
    public Response deleteScriptByScriptVersionId(String username, Long scriptVersionId) {
        return scriptResourceProxy.deleteScriptByScriptVersionId(username, null, null, null, scriptVersionId);
    }

    @Override
    public Response<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(String username,
                                                                                   String scriptId,
                                                                                   Long scriptVersionId) {
        return scriptResourceProxy.listScriptSyncTemplateSteps(username, null, null, null, scriptId,
            scriptVersionId);
    }

    @Override
    public Response<List<ScriptSyncResultVO>> syncScripts(String username, String scriptId,
                                                          Long scriptVersionId, ScriptSyncReq scriptSyncReq) {
        return scriptResourceProxy.syncScripts(username, null, null, null, scriptId, scriptVersionId,
            scriptSyncReq);
    }

    @Override
    public Response<ScriptCiteCountVO> getPublicScriptCiteCount(String username, String scriptId,
                                                                Long scriptVersionId) {
        return scriptResourceProxy.getScriptCiteCount(username, null, null, null, scriptId, scriptVersionId);
    }

    @Override
    public Response<ScriptCiteInfoVO> getPublicScriptCiteInfo(String username, String scriptId,
                                                              Long scriptVersionId) {
        return scriptResourceProxy.getScriptCiteInfo(username, null, null, null, scriptId, scriptVersionId);
    }

    @Override
    public Response<?> batchUpdatePublicScriptTags(String username,
                                                   ScriptTagBatchPatchReq tagBatchUpdateReq) {
        return scriptResourceProxy.batchUpdateScriptTags(username, null, null, null, tagBatchUpdateReq);
    }

    @Override
    public Response<TagCountVO> getTagPublicScriptCount(String username) {
        return scriptResourceProxy.getTagScriptCount(username, null, null, null);
    }
}
