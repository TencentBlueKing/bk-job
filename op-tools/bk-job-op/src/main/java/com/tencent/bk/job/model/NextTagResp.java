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

/**
 * 求某版本系列下一个Tag的响应体
 * <p>
 * 本接口只返回<b>建议值</b>，不承诺唯一性分配：Tag的权威唯一性由Git打Tag那一步保证，
 * 调用方在Git打Tag失败时应重新调用本接口取新的建议值。服务端不做任何冲突保护。
 */
@Data
public class NextTagResp {

    /**
     * 计算是否成功
     */
    private boolean result;

    /**
     * 提示信息。result为false时为失败原因；成功时可能携带稳定版修订号顺延的提示
     */
    private String message;

    /**
     * 归一化后的版本系列。注意两个分支的粒度不同：
     * <ul>
     *     <li>先行版分支为x.y.z-{type}，即在同一个修订号内递增先行号</li>
     *     <li>稳定版分支为x.y，即在同一个小版本内递增修订号</li>
     * </ul>
     * 这与list_tags中prefix的粒度（传什么就是什么）不是同一个概念。
     */
    private String series;

    /**
     * 上述series范围内当前最大的Tag，系列为空时为null，便于调用方核对推导依据
     */
    private String currentMaxTag;

    /**
     * 计算出的下一个Tag，带小写v前缀
     */
    private String nextTag;

    public static NextTagResp fail(String message) {
        NextTagResp resp = new NextTagResp();
        resp.setResult(false);
        resp.setMessage(message);
        return resp;
    }
}
