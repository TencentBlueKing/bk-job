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

package com.tencent.bk.job.backup.model.dto;

import com.tencent.bk.job.backup.constant.LogEntityTypeEnum;
import com.tencent.bk.job.backup.model.web.LogEntityVO;
import lombok.Data;

/**
 * @since 29/7/2020 11:11
 */
@Data
public class LogEntityDTO {

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 作业 ID
     */
    private String jobId;

    /**
     * 日志类型
     */
    private LogEntityTypeEnum type;

    /**
     * 日志产生时间
     */
    private Long timestamp;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 日志关联的模版 ID
     */
    private Long templateId;

    /**
     * 日志关联的执行方案信息
     */
    private Long planId;

    /**
     * 日志内链接的文字
     */
    private String linkText;

    /**
     * 日志内链接的跳转地址
     */
    private String linkUrl;

    public LogEntityDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public static LogEntityVO toVO(LogEntityDTO logEntityDTO) {
        if (logEntityDTO == null) {
            return null;
        }
        LogEntityVO logEntityVO = new LogEntityVO();
        logEntityVO.setType(logEntityDTO.getType().getType());
        logEntityVO.setTimestamp(logEntityDTO.getTimestamp());
        logEntityVO.setContent(logEntityDTO.getContent());
        logEntityVO.setTemplateId(logEntityDTO.getTemplateId());
        logEntityVO.setPlanId(logEntityDTO.getPlanId());
        logEntityVO.setLinkText(logEntityDTO.getLinkText());
        logEntityVO.setLinkUrl(logEntityDTO.getLinkUrl());
        return logEntityVO;
    }
}
