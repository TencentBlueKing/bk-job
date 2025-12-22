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

package com.tencent.bk.job.manage.service.globalsetting;

import com.tencent.bk.job.manage.api.common.constants.OSTypeEnum;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRule;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRulesReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.FileUploadSettingReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.HistoryExpireReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplatePreviewReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplateReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.FileUploadSettingVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateDetailWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;

import java.util.List;
import java.util.Map;

public interface GlobalSettingsService {
    Boolean isNotifyChannelConfiged(String tenantId);

    Boolean setNotifyChannelConfiged(String tenantId);

    List<NotifyBlackUserInfoVO> listNotifyBlackUsers(String username, Integer start, Integer pageSize);

    List<String> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req);

    Long getHistoryExpireTime(String username);

    Integer setHistoryExpireTime(String username, HistoryExpireReq req);

    AccountNameRule getCurrentAccountNameRule(OSTypeEnum osType);

    AccountNameRulesWithDefaultVO getAccountNameRules();

    Boolean setAccountNameRules(String username, AccountNameRulesReq req);

    Boolean saveFileUploadSettings(String username, FileUploadSettingReq req);

    FileUploadSettingVO getFileUploadSettings();

    /**
     * 获取渲染之后的平台信息
     *
     * @return 渲染之后的平台信息
     */
    PlatformInfoVO getRenderedPlatformInfo();

    /**
     * 获取全局设置-平台信息
     *
     * @param username 用户名
     */
    PlatformInfoWithDefaultVO getPlatformInfoWithDefault(String username);

    Integer saveChannelTemplate(String username, ChannelTemplateReq req);

    Integer sendChannelTemplate(String username, ChannelTemplatePreviewReq req);

    ChannelTemplateDetailWithDefaultVO getChannelTemplateDetail(String username, String channelCode,
                                                                String messageTypeCode);

    String getDocCenterBaseUrl();

    String getDocJobRootUrl();

    Map<String, String> getRelatedSystemUrls(String username);

    Map<String, Object> getJobConfig(String username);
}
