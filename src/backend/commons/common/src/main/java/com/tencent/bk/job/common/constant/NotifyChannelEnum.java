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

package com.tencent.bk.job.common.constant;

import com.tencent.bk.job.common.model.vo.NotifyChannelVO;
import com.tencent.bk.job.common.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotifyChannelEnum {
    /**
     * 短信
     */
    SMS("sms", "job.manage.notify.channel.sms"),

    /**
     * 邮件
     */
    EMAIL("mail", "job.manage.notify.channel.email"),

    /**
     * 微信
     */
    WECHAT("weixin", "job.manage.notify.channel.wechat"),

    /**
     * 企业微信
     */
    WORK_WECHAT("work-weixin", "job.manage.notify.channel.work-wechat");

    private final String channel;
    private final String defaultName;

    public static NotifyChannelEnum getByChannel(String channel) {
        for (NotifyChannelEnum notifyChannel : NotifyChannelEnum.values()) {
            if (notifyChannel.channel.equals(channel)) {
                return notifyChannel;
            }
        }
        return null;
    }

    public static NotifyChannelVO getVO(String notifyChannelName) {
        NotifyChannelEnum notifyChannel = NotifyChannelEnum.valueOf(notifyChannelName);
        return new NotifyChannelVO(notifyChannel.getChannel(), I18nUtil.getI18nMessage(notifyChannel.getDefaultName()));
    }
}
