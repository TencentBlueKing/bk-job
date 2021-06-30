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

package com.tencent.bk.job.execute.engine.gse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.execute.engine.consts.GSECode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * GSE拷贝文件的执行结果
 */
@Setter
@Getter
@ToString
public class CopyFileRsp {

    /**
     * 执行内容
     */
    @JsonProperty("content")
    private GSEFileTaskResult gseFileTaskResult;

    /**
     * 0 执行正常包括执行中和执行成功， 非0 执行失败， 115 预留
     */
    @JsonProperty("error_code")
    private Integer errorCode;

    /**
     * 兼容gse协议，等同于error_code
     */
    @JsonProperty("errcode")
    private Integer errCode;

    /**
     * 错误信息
     */
    @JsonProperty("error_msg")
    private String errorMsg;

    /**
     * 错误信息,兼容gse协议，等同于error_msg
     */
    @JsonProperty("errmsg")
    private String errMsg;

    public Integer getFinalErrorCode() {
        if (errorCode != null) {
            return errorCode;
        } else if (errCode != null) {
            return errCode;
        } else {
            return GSECode.AtomicErrorCode.ERROR.getValue();
        }
    }

    public String getFinalErrorMsg() {
        if (errorCode != null) {
            return errorMsg;
        } else {
            return errMsg;
        }
    }
}
