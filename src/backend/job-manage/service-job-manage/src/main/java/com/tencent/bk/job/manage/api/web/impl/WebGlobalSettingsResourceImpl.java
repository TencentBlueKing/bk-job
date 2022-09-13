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

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.web.WebGlobalSettingsResource;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRulesReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.FileUploadSettingReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.HistoryExpireReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.SetTitleFooterReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplatePreviewReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplateReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.FileUploadSettingVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.TitleFooterWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateDetailWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateStatusVO;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
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
    public Response<List<NotifyChannelWithIconVO>> listNotifyChannel(String username) {
        return Response.buildSuccessResp(globalSettingsService.listNotifyChannel(username));
    }

    @Override
    public Response<Integer> setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req) {
        return Response.buildSuccessResp(globalSettingsService.setAvailableNotifyChannel(username, req));
    }

    @Override
    public Response<Integer> saveChannelTemplate(String username, ChannelTemplateReq req) {
        return Response.buildSuccessResp(globalSettingsService.saveChannelTemplate(username, req));
    }

    @Override
    public Response<Integer> sendChannelTemplate(String username, ChannelTemplatePreviewReq req) {
        return Response.buildSuccessResp(globalSettingsService.sendChannelTemplate(username, req));
    }

    @Override
    public Response<ChannelTemplateDetailWithDefaultVO> getChannelTemplateDetail(String username,
                                                                                 String channelCode,
                                                                                 String messageTypeCode) {
        return Response.buildSuccessResp(globalSettingsService.getChannelTemplateDetail(username, channelCode,
            messageTypeCode));
    }

    @Override
    public Response<List<ChannelTemplateStatusVO>> listChannelTemplateStatus(String username) {
        return Response.buildSuccessResp(globalSettingsService.listChannelTemplateStatus(username));
    }

    @Override
    public Response<List<UserVO>> listUsers(String username, String prefixStr, Long offset, Long limit) {
        return Response.buildSuccessResp(globalSettingsService.listUsers(username, prefixStr, offset, limit));
    }

    @Override
    public Response<List<NotifyBlackUserInfoVO>> listNotifyBlackUsers(String username, Integer start,
                                                                      Integer pageSize) {
        return Response.buildSuccessResp(globalSettingsService.listNotifyBlackUsers(username, start, pageSize));
    }

    @Override
    public Response<List<String>> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req) {
        return Response.buildSuccessResp(globalSettingsService.saveNotifyBlackUsers(username, req));
    }

    @Override
    public Response<Long> getHistoryExpireTime(String username) {
        return Response.buildSuccessResp(globalSettingsService.getHistoryExpireTime(username));
    }

    @Override
    public Response<Integer> setHistoryExpireTime(String username, HistoryExpireReq req) {
        return Response.buildSuccessResp(globalSettingsService.setHistoryExpireTime(username, req));
    }

    @Override
    public Response<AccountNameRulesWithDefaultVO> getAccountNameRules(String username) {
        return Response.buildSuccessResp(globalSettingsService.getAccountNameRules());
    }

    @Override
    public Response<Boolean> setAccountNameRules(String username, AccountNameRulesReq req) {
        return Response.buildSuccessResp(globalSettingsService.setAccountNameRules(username, req));
    }

    @Override
    public Response<Boolean> saveFileUploadSettings(String username, FileUploadSettingReq req) {
        return Response.buildSuccessResp(globalSettingsService.saveFileUploadSettings(username, req));
    }

    @Override
    public Response<FileUploadSettingVO> getFileUploadSettings(String username) {
        return Response.buildSuccessResp(globalSettingsService.getFileUploadSettings());
    }

    @Override
    public Response<Boolean> setTitleFooter(String username, SetTitleFooterReq req) {
        return Response.buildSuccessResp(globalSettingsService.setTitleFooter(username, req));
    }

    @Override
    public Response<TitleFooterWithDefaultVO> getTitleFooterWithDefault(String username) {
        return Response.buildSuccessResp(globalSettingsService.getTitleFooterWithDefault(username));
    }
}
