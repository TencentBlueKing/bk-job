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

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询基础类
 */
@Getter
@Setter
@ToString
public class BaseSearchCondition {
    /**
     * 分页起始
     */
    @ApiModelProperty(value = "分页起始", required = false)
    private Integer start;
    /**
     * 分页大小
     */
    @ApiModelProperty(value = "分页大小", required = false)
    private Integer length;

    /**
     * 排序 0：逆序 1：正序
     */
    @ApiModelProperty(value = "排序 0：逆序 1：正序", required = false)
    private Integer order;

    /**
     * 排序的字段
     */
    @ApiModelProperty(value = "排序的字段", required = false)
    private String orderField;


    /**
     * 创建人
     */
    private String creator;
    /**
     * 创建时间范围-起始
     */
    private Long createTimeStart;

    /**
     * 创建时间范围-结束
     */
    private Long createTimeEnd;
    /**
     * 最后修改人
     */
    private String lastModifyUser;

    /**
     * 修改时间范围-起始
     */
    private Long lastModifyTimeStart;

    /**
     * 修改时间范围-结束
     */
    private Long lastModifyTimeEnd;

    public int getLengthOrDefault(int defaultLength) {
        return (length == null || length <= 0) ? defaultLength : length;
    }

    public int getStartOrDefault(int defaultStart) {
        return (start == null || start < 0) ? defaultStart : start;
    }

    public boolean isGetAll() {
        return start != null && start == -1
            && length != null && length == -1;
    }

}
