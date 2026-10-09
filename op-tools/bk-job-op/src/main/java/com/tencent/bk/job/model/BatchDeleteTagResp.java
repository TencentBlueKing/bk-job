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
 * 批量删除仓库Tag的响应体
 * <p>
 * 只支持按完整Tag列表删除，不支持按版本前缀删除：前缀删除等价于一键清空整个版本系列，
 * 调用方可先list_tags再按列表删除，多一次调用换掉误删全系列的风险。
 * <p>
 * 5个分桶互斥，条数之和恒等于totalCount：
 * deletedTags + notFoundTags + duplicatedTags + ignoredTags + invalidTags == totalCount。
 */
@Data
public class BatchDeleteTagResp {

    /**
     * 请求是否被受理，不代表全部Tag都已删除
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
     * 实际删除条数
     */
    private int deletedCount;

    /**
     * 已删除的归一化Tag
     */
    private List<String> deletedTags = new ArrayList<>();

    /**
     * 格式合法但库中不存在的归一化Tag，删除幂等，不视为失败
     */
    private List<String> notFoundTags = new ArrayList<>();

    /**
     * 批内重复出现、只删一次的归一化Tag；其首次出现的那条按实际结果落在deletedTags或notFoundTags
     */
    private List<String> duplicatedTags = new ArrayList<>();

    /**
     * 不纳管的Tag，保留原始输入值。这类Tag本就不会入库，无需删除
     */
    private List<String> ignoredTags = new ArrayList<>();

    /**
     * 格式非法的Tag，保留原始输入值
     */
    private List<String> invalidTags = new ArrayList<>();

    public static BatchDeleteTagResp fail(String message) {
        BatchDeleteTagResp resp = new BatchDeleteTagResp();
        resp.setResult(false);
        resp.setMessage(message);
        return resp;
    }
}
