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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.Response;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "job-manage:web:GlobalSettings")
@RequestMapping("/web/globalSettings")
@RestController
@WebAPI
public interface WebGlobalSettingsResource {

    @Operation(summary = "获取通知渠道列表及生效状态", produces = "application/json")
    @GetMapping("/notify/listChannels")
    Response<List<NotifyChannelWithIconVO>> listNotifyChannel(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @Operation(summary = "超级管理员设置启用的通知渠道", produces = "application/json")
    @PostMapping("/notify/setAvailableChannels")
    Response<Integer> setAvailableNotifyChannel(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "创建或更新请求体", required = true)
        @RequestBody
            SetAvailableNotifyChannelReq req
    );

    @Operation(summary = "超级管理员保存消息模板", produces = "application/json")
    @PostMapping("/notify/channelTemplate")
    Response<Integer> saveChannelTemplate(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "设置通知渠道消息模板请求体", required = true)
        @RequestBody
            ChannelTemplateReq req
    );

    @Operation(summary = "消息发送预览", produces = "application/json")
    @PostMapping("/notify/channelTemplate/send")
    Response<Integer> sendChannelTemplate(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "消息预览请求体", required = true)
        @RequestBody
            ChannelTemplatePreviewReq req
    );

    @Operation(summary = "消息模板详情", produces = "application/json")
    @GetMapping("/notify/channelTemplate/detail")
    Response<ChannelTemplateDetailWithDefaultVO> getChannelTemplateDetail(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(description = "渠道Code")
        @RequestParam(value = "channelCode")
            String channelCode,
        @Parameter(description = "消息类型Code")
        @RequestParam(value = "messageTypeCode")
            String messageTypeCode
    );

    @Operation(summary = "查询各渠道消息模板配置状态", produces = "application/json")
    @GetMapping("/notify/channelTemplate/configStatus")
    Response<List<ChannelTemplateStatusVO>> listChannelTemplateStatus(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @Operation(summary = "获取现有通知黑名单用户列表", produces = "application/json")
    @GetMapping("/notify/users/blacklist")
    Response<List<NotifyBlackUserInfoVO>> listNotifyBlackUsers(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Parameter(description = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @Parameter(description = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );


    @Operation(summary = "设置通知黑名单", produces = "application/json")
    @PostMapping("/notify/users/blacklist")
    Response<List<String>> saveNotifyBlackUsers(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "创建或更新请求体", required = true)
        @RequestBody
            NotifyBlackUsersReq req
    );


    @Operation(summary = "获取执行历史保留时间", produces = "application/json")
    @GetMapping("/history/expireTime")
    Response<Long> getHistoryExpireTime(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "设置执行历史保留时间", produces = "application/json")
    @PostMapping("/history/expireTime")
    Response<Integer> setHistoryExpireTime(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "创建或更新请求体", required = true)
        @RequestBody
            HistoryExpireReq req
    );


    @Operation(summary = "获取账号命名规则", produces = "application/json")
    @GetMapping("/account/nameRules")
    Response<AccountNameRulesWithDefaultVO> getAccountNameRules(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );


    @Operation(summary = "设置账号命名规则", produces = "application/json")
    @PostMapping("/account/setNameRules")
    Response<Boolean> setAccountNameRules(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "创建或更新请求体", required = true)
        @RequestBody
            AccountNameRulesReq req
    );

    @Operation(summary = "设置文件上传设置", produces = "application/json")
    @PostMapping("/file/upload")
    Response<Boolean> saveFileUploadSettings(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @Parameter(description = "创建或更新请求体", required = true)
        @RequestBody
        @Validated
            FileUploadSettingReq req
    );


    @Operation(summary = "获取文件上传设置", produces = "application/json")
    @GetMapping("/file/upload")
    Response<FileUploadSettingVO> getFileUploadSettings(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @CompatibleImplementation(name = "platform_info", deprecatedVersion = "3.11.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    @Operation(summary = "获取平台信息-包含默认配置", produces = "application/json")
    @GetMapping("/platformInfoWithDefault")
    Response<PlatformInfoWithDefaultVO> getPlatformInfoWithDefault(
        @Parameter(description = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username
    );
}
