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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询基础类
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class BaseSearchCondition implements Cloneable {
    /**
     * 分页起始
     */
    private Integer start;
    /**
     * 分页大小
     */
    private Integer length;

    /**
     * 分页-是否计算总数；默认计算。分页计算总数可能会影响 API 性能, 必要场景才可使用
     */
    private boolean countPageTotal = true;

    /**
     * 排序 0：降序 1：升序
     */
    private Integer order;

    /**
     * 排序的字段
     */
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

    public static BaseSearchCondition pageCondition(Integer start, Integer length) {
        BaseSearchCondition searchCondition = new BaseSearchCondition();
        searchCondition.setStart(start);
        searchCondition.setLength(length);
        searchCondition.setCountPageTotal(true);
        return searchCondition;
    }

    public static BaseSearchCondition pageCondition(Integer start, Integer length, boolean countPageTotal) {
        BaseSearchCondition searchCondition = new BaseSearchCondition();
        searchCondition.setStart(start);
        searchCondition.setLength(length);
        searchCondition.setCountPageTotal(countPageTotal);
        return searchCondition;
    }

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

    public BaseSearchCondition clone() {
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(length);
        baseSearchCondition.setOrderField(orderField);
        baseSearchCondition.setOrder(order);
        baseSearchCondition.setCreateTimeEnd(createTimeEnd);
        baseSearchCondition.setCreateTimeStart(createTimeStart);
        baseSearchCondition.setCreator(creator);
        baseSearchCondition.setLastModifyUser(lastModifyUser);
        baseSearchCondition.setLastModifyTimeStart(lastModifyTimeStart);
        baseSearchCondition.setLastModifyTimeEnd(lastModifyTimeEnd);
        return baseSearchCondition;
    }

}
