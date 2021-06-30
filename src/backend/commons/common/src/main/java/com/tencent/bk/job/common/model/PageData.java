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

package com.tencent.bk.job.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PageData<T> {
    private Integer start;
    private Integer pageSize;
    private Long total;
    private List<T> data;
    private Boolean canCreate;
    /**
     * 是否存在任意一个资源
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("existAny")
    private Boolean existAny;

    public PageData(Integer start, Integer pageSize, Long total, List<T> data) {
        this.start = start;
        this.pageSize = pageSize;
        this.total = total;
        this.data = data;
    }

    public static <T> PageData<T> from(PageData<T> pageData) {
        PageData<T> newPageData = new PageData<>();
        newPageData.setTotal(pageData.getTotal());
        newPageData.setStart(pageData.getStart());
        newPageData.setPageSize(pageData.getPageSize());
        newPageData.setData(pageData.getData());
        return newPageData;
    }

    public static <T, R> PageData<T> from(PageData<R> pageData, Function<R, T> converter) {
        if (CollectionUtils.isEmpty(pageData.getData())) {
            return emptyPageData(pageData.getStart(), pageData.getPageSize());
        }
        PageData<T> esbPageData = new PageData<>();
        esbPageData.setTotal(pageData.getTotal());
        esbPageData.setStart(pageData.getStart());
        esbPageData.setPageSize(pageData.getPageSize());
        esbPageData.setData(pageData.getData().stream().map(converter).collect(Collectors.toList()));
        return esbPageData;
    }

    public static <T> PageData<T> emptyPageData(Integer start, Integer length) {
        return new PageData<>(start, length, 0L, Collections.emptyList());
    }
}
