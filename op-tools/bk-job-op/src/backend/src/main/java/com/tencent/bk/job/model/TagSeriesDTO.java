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

/**
 * 解析后的版本前缀，用于按前缀检索Tag
 * <p>
 * minor/patch/type为null代表该段未指定，即查询范围更宽：
 * <ul>
 *     <li>{@code 3} -&gt; major=3，命中3.x.x全部Tag</li>
 *     <li>{@code 3.10} -&gt; major=3,minor=10</li>
 *     <li>{@code 3.10.1} -&gt; major=3,minor=10,patch=1</li>
 *     <li>{@code 3.10.1-alpha} -&gt; 再叠加type=ALPHA</li>
 * </ul>
 */
@Data
@AllArgsConstructor
public class TagSeriesDTO {

    /**
     * 主版本号，必定非空
     */
    private Integer major;

    /**
     * 小版本号，未指定时为null
     */
    private Integer minor;

    /**
     * 修订号，未指定时为null
     */
    private Integer patch;

    /**
     * 先行版本类型，未指定时为null；指定为稳定版没有意义，故该字段只会是ALPHA/BETA/RC
     */
    private TagTypeEnum type;

    /**
     * 归一化后的前缀字符串，如3.10.1-alpha
     */
    public String toNormalizedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major);
        if (minor != null) {
            sb.append('.').append(minor);
        }
        if (patch != null) {
            sb.append('.').append(patch);
        }
        if (type != null) {
            sb.append('-').append(type.getPreType());
        }
        return sb.toString();
    }
}
