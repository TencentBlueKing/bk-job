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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结构化的仓库Tag，与DB表repo_tag一一对应
 * <p>
 * 版本号各段之所以拆成独立的整数列，是因为按版本前缀查询无法用字符串LIKE实现：
 * {@code LIKE '3.1%'} 会错误命中3.10.x与3.11.x，且字典序下 'v3.10.0' < 'v3.9.20' 与语义相反。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepoTagDTO {

    /**
     * 归一化后的完整Tag，小写v前缀，如v3.10.3-alpha.11
     */
    private String tag;

    /**
     * 主版本号
     */
    private Integer major;

    /**
     * 小版本号
     */
    private Integer minor;

    /**
     * 修订号
     */
    private Integer patch;

    /**
     * 先行版本类型：空串-稳定版，alpha/beta/rc
     */
    private String preType;

    /**
     * 类型序：1-alpha，2-beta，3-rc，4-stable，值越大版本越新
     */
    private Integer preRank;

    /**
     * 先行版本号，稳定版为0
     */
    private Integer preNum;
}
