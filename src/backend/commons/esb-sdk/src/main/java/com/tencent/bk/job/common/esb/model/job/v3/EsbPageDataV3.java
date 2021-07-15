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

package com.tencent.bk.job.common.esb.model.job.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.PageData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结构
 */
@NoArgsConstructor
@Data
public class EsbPageDataV3<T> {
    private Integer start;
    @JsonProperty("length")
    private Integer length;
    private Long total;
    private List<T> data;

    public EsbPageDataV3(Integer start, Integer length, Long total, List<T> data) {
        this.start = start;
        this.length = length;
        this.total = total;
        this.data = data;
    }

    public static <T> EsbPageDataV3<T> from(PageData<T> pageData) {
        EsbPageDataV3<T> esbPageData = new EsbPageDataV3<>();
        esbPageData.setTotal(pageData.getTotal());
        esbPageData.setStart(pageData.getStart());
        esbPageData.setLength(pageData.getPageSize());
        esbPageData.setData(pageData.getData());
        return esbPageData;
    }

    public static <T, R> EsbPageDataV3<T> from(PageData<R> pageData, Function<R, T> converter) {
        if (CollectionUtils.isEmpty(pageData.getData())) {
            return emptyPageData(pageData.getStart(), pageData.getPageSize());
        }
        EsbPageDataV3<T> esbPageData = new EsbPageDataV3<>();
        esbPageData.setTotal(pageData.getTotal());
        esbPageData.setStart(pageData.getStart());
        esbPageData.setLength(pageData.getPageSize());
        esbPageData.setData(pageData.getData().stream().map(converter).collect(Collectors.toList()));
        return esbPageData;
    }

    public static <T> EsbPageDataV3<T> emptyPageData(Integer start, Integer length) {
        return new EsbPageDataV3<>(start, length, 0L, Collections.emptyList());
    }
}
