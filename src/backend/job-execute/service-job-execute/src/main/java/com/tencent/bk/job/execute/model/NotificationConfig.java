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

package com.tencent.bk.job.execute.model;

import lombok.Data;

/**
 * 通知配置
 */
@Data
public class NotificationConfig {
    private Integer id;
    private String userName;

    private Boolean isNormalSuccessRtx;
    private Boolean isNormalSuccessEmail;
    private Boolean isNormalSuccessSms;
    private Boolean isNormalSuccessWexin;
    private Boolean isNormalFailRtx;
    private Boolean isNormalFailEmail;
    private Boolean isNormalFailSms;
    private Boolean isNormalFailWexin;
    private Boolean isNormalWaitingRtx;
    private Boolean isNormalWaitingEmail;
    private Boolean isNormalWaitingSms;
    private Boolean isNormalWaitingWexin;
    private Boolean isApiSuccessRtx;
    private Boolean isApiSuccessEmail;
    private Boolean isApiSuccessSms;
    private Boolean isApiSuccessWexin;
    private Boolean isApiFailRtx;
    private Boolean isApiFailEmail;
    private Boolean isApiFailSms;
    private Boolean isApiFailWexin;
    private Boolean isApiWaitingRtx;
    private Boolean isApiWaitingEmail;
    private Boolean isApiWaitingSms;
    private Boolean isApiWaitingWexin;
    private Boolean isCronSuccessRtx;
    private Boolean isCronSuccessEmail;
    private Boolean isCronSuccessSms;
    private Boolean isCronSuccessWexin;
    private Boolean isCronFailRtx;
    private Boolean isCronFailEmail;
    private Boolean isCronFailSms;
    private Boolean isCronFailWexin;
    private Boolean isCronWaitingRtx;
    private Boolean isCronWaitingEmail;
    private Boolean isCronWaitingSms;
    private Boolean isCronWaitingWexin;
    private String lang;

    public NotificationConfig() {
        isNormalSuccessRtx = false;
        isNormalSuccessEmail = false;
        isNormalSuccessSms = false;
        isNormalSuccessWexin = false;
        isNormalFailRtx = false;
        isNormalFailEmail = true;
        isNormalFailSms = true;
        isNormalFailWexin = false;
        isNormalWaitingRtx = false;
        isNormalWaitingEmail = true;
        isNormalWaitingSms = true;
        isNormalWaitingWexin = false;
        isApiSuccessRtx = false;
        isApiSuccessEmail = false;
        isApiSuccessSms = false;
        isApiSuccessWexin = false;
        isApiFailRtx = false;
        isApiFailEmail = true;
        isApiFailSms = true;
        isApiFailWexin = false;
        isApiWaitingRtx = false;
        isApiWaitingEmail = true;
        isApiWaitingSms = true;
        isApiWaitingWexin = false;
        isCronSuccessRtx = false;
        isCronSuccessEmail = false;
        isCronSuccessSms = false;
        isCronSuccessWexin = false;
        isCronFailRtx = false;
        isCronFailEmail = true;
        isCronFailSms = true;
        isCronFailWexin = false;
        isCronWaitingRtx = false;
        isCronWaitingEmail = true;
        isCronWaitingSms = true;
        isCronWaitingWexin = false;
    }

}
