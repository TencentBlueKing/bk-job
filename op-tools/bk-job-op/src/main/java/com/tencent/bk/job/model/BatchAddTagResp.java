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

package com.tencent.bk.job.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量写入仓库Tag的响应体
 * <p>
 * 参数校验不通过时result为false；参数校验通过即为true，此时单个Tag的处理结果由4个分桶字段体现，
 * 允许部分成功——全量同步场景下必然混入大量不纳管/非法Tag，整批拒绝会让接口不可用。
 */
@Data
public class BatchAddTagResp {

    /**
     * 请求是否被受理，不代表全部Tag都已入库
     */
    private boolean result;

    /**
     * result为false时的原因说明
     */
    private String message;

    /**
     * 请求中的Tag总数（去重前）
     */
    private int totalCount;

    /**
     * 实际新增入库条数
     */
    private int addedCount;

    /**
     * 新增入库的归一化Tag
     */
    private List<String> addedTags = new ArrayList<>();

    /**
     * 库中已存在（含批内重复）而未重复写入的归一化Tag
     */
    private List<String> existedTags = new ArrayList<>();

    /**
     * 不纳管的Tag，保留原始输入值。包含开发自测临时先行版与四段式历史Tag，属预期噪声，调用方不应据此告警
     */
    private List<String> ignoredTags = new ArrayList<>();

    /**
     * 格式非法的Tag，保留原始输入值，便于调用方定位。大写先行标识（如-Alpha.1）会落在这里
     */
    private List<String> invalidTags = new ArrayList<>();

    public static BatchAddTagResp fail(String message) {
        BatchAddTagResp resp = new BatchAddTagResp();
        resp.setResult(false);
        resp.setMessage(message);
        return resp;
    }
}
