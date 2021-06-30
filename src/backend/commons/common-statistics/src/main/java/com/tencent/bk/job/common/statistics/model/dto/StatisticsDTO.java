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

package com.tencent.bk.job.common.statistics.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 通用统计数据模型
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO implements Cloneable {
    /**
     * id
     */
    private Long id;
    /**
     * id
     */
    private Long appId;
    /**
     * 资源
     */
    private String resource;
    /**
     * 维度
     */
    private String dimension;
    /**
     * 维度取值
     */
    private String dimensionValue;
    /**
     * 统计单位日期
     */
    private String date;
    /**
     * 统计值
     */
    private String value;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 更新时间
     */
    private Long lastModifyTime;

    @Override
    public StatisticsDTO clone() {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setId(id);
        statisticsDTO.setAppId(appId);
        statisticsDTO.setResource(resource);
        statisticsDTO.setDimension(dimension);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setDate(date);
        statisticsDTO.setValue(value);
        statisticsDTO.setCreateTime(createTime);
        statisticsDTO.setLastModifyTime(lastModifyTime);
        return statisticsDTO;
    }
}
