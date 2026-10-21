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

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 纳管的仓库Tag类型枚举
 * <p>
 * rank值越大代表版本越新：同一个x.y.z下 alpha < beta < rc < stable。
 * 先行标识大小写敏感，仅接受小写形式，{@code v3.10.1-Alpha.1} 不会匹配到任何类型。
 */
public enum TagTypeEnum {

    /** 开发中的先行版 */
    ALPHA("alpha", 1),

    /** 已通过测试的先行版 */
    BETA("beta", 2),

    /** 已通过灰度的先行版 */
    RC("rc", 3),

    /** 稳定版，无先行标识 */
    STABLE("", 4);

    /** 先行标识，稳定版为空串，与DB中的pre_type列取值一致 */
    private final String preType;

    /** 类型序，与DB中的pre_rank列取值一致 */
    private final int rank;

    TagTypeEnum(String preType, int rank) {
        this.preType = preType;
        this.rank = rank;
    }

    @JsonValue
    public String getPreType() {
        return preType;
    }

    public int getRank() {
        return rank;
    }

    /**
     * 根据先行标识获取类型，大小写敏感
     *
     * @param preType 先行标识，null或空串代表稳定版
     * @return 纳管的Tag类型；标识不在纳管白名单内时返回null
     */
    public static TagTypeEnum ofPreType(String preType) {
        if (preType == null || preType.isEmpty()) {
            return STABLE;
        }
        for (TagTypeEnum value : values()) {
            if (value != STABLE && value.preType.equals(preType)) {
                return value;
            }
        }
        return null;
    }
}
