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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.web.WebGlobalSettingsResource;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRulesReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.FileUploadSettingReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.HistoryExpireReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplatePreviewReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplateReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.FileUploadSettingVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateDetailWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateStatusVO;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import com.tencent.bk.job.manage.service.globalsetting.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class WebGlobalSettingsResourceImpl implements WebGlobalSettingsResource {

    private final GlobalSettingsService globalSettingsService;

    @Autowired
    public WebGlobalSettingsResourceImpl(GlobalSettingsService globalSettingsService) {
        this.globalSettingsService = globalSettingsService;
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<List<NotifyChannelWithIconVO>> listNotifyChannel(String username) {
        return Response.buildSuccessResp(globalSettingsService.listNotifyChannel(username));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<Integer> setAvailableNotifyChannel(String username,
                                                       @AuditRequestBody SetAvailableNotifyChannelReq req) {
        return Response.buildSuccessResp(globalSettingsService.setAvailableNotifyChannel(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<Integer> saveChannelTemplate(String username,
                                                 @AuditRequestBody ChannelTemplateReq req) {
        return Response.buildSuccessResp(globalSettingsService.saveChannelTemplate(username, req));
    }

    @Override
    public Response<Integer> sendChannelTemplate(String username,
                                                 @AuditRequestBody ChannelTemplatePreviewReq req) {
        return Response.buildSuccessResp(globalSettingsService.sendChannelTemplate(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<ChannelTemplateDetailWithDefaultVO> getChannelTemplateDetail(String username,
                                                                                 String channelCode,
                                                                                 String messageTypeCode) {
        return Response.buildSuccessResp(globalSettingsService.getChannelTemplateDetail(username, channelCode,
            messageTypeCode));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<List<ChannelTemplateStatusVO>> listChannelTemplateStatus(String username) {
        return Response.buildSuccessResp(globalSettingsService.listChannelTemplateStatus(username));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<List<UserVO>> listUsers(String username, String prefixStr, Long offset, Long limit) {
        return Response.buildSuccessResp(globalSettingsService.listUsers(username, prefixStr, offset, limit));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<List<NotifyBlackUserInfoVO>> listNotifyBlackUsers(String username, Integer start,
                                                                      Integer pageSize) {
        return Response.buildSuccessResp(globalSettingsService.listNotifyBlackUsers(username, start, pageSize));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<List<String>> saveNotifyBlackUsers(String username,
                                                       @AuditRequestBody NotifyBlackUsersReq req) {
        return Response.buildSuccessResp(globalSettingsService.saveNotifyBlackUsers(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<Long> getHistoryExpireTime(String username) {
        return Response.buildSuccessResp(globalSettingsService.getHistoryExpireTime(username));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<Integer> setHistoryExpireTime(String username,
                                                  @AuditRequestBody HistoryExpireReq req) {
        return Response.buildSuccessResp(globalSettingsService.setHistoryExpireTime(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<AccountNameRulesWithDefaultVO> getAccountNameRules(String username) {
        return Response.buildSuccessResp(globalSettingsService.getAccountNameRules());
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<Boolean> setAccountNameRules(String username,
                                                 @AuditRequestBody AccountNameRulesReq req) {
        return Response.buildSuccessResp(globalSettingsService.setAccountNameRules(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<Boolean> saveFileUploadSettings(String username,
                                                    @AuditRequestBody FileUploadSettingReq req) {
        return Response.buildSuccessResp(globalSettingsService.saveFileUploadSettings(username, req));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<FileUploadSettingVO> getFileUploadSettings(String username) {
        return Response.buildSuccessResp(globalSettingsService.getFileUploadSettings());
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.VIEW_GLOBAL_SETTINGS
    )
    public Response<PlatformInfoWithDefaultVO> getPlatformInfoWithDefault(String username) {
        return Response.buildSuccessResp(globalSettingsService.getPlatformInfoWithDefault(username));
    }
}
