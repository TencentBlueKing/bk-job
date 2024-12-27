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

package com.tencent.bk.job.analysis.util.calc;

import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.consts.DataTrendEnum;
import com.tencent.bk.job.analysis.model.web.CommonStatisticWithRateVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象的同环比计算器
 */
@Slf4j
public abstract class AbstractMomYoyCalculator {
    // 当前统计数据
    private final StatisticsDTO statisticsDTO;
    // 环比上一周期的统计数据
    private final StatisticsDTO momStatisticsDTO;
    // 同比上一周期的统计数据
    private final StatisticsDTO yoyStatisticsDTO;

    public AbstractMomYoyCalculator(StatisticsDTO statisticsDTO,
                                    StatisticsDTO momStatisticsDTO,
                                    StatisticsDTO yoyStatisticsDTO) {
        this.statisticsDTO = statisticsDTO;
        this.momStatisticsDTO = momStatisticsDTO;
        this.yoyStatisticsDTO = yoyStatisticsDTO;
    }

    /**
     * 从序列化的存储数据中解析统计量数值
     *
     * @param serializedData 序列化的存储数据
     * @return 统计量数值
     */
    protected abstract Long getCountFromSerializedData(String serializedData);

    /**
     * 根据几个时间点的统计数据计算同环比数据
     *
     * @return 同环比数据
     */
    public CommonStatisticWithRateVO calc() {
        if (statisticsDTO == null) {
            return null;
        }
        Long count = getCountFromSerializedData(statisticsDTO.getValue());
        CommonStatisticWithRateVO commonStatisticWithRateVO = new CommonStatisticWithRateVO();
        commonStatisticWithRateVO.setCount(count);
        if (momStatisticsDTO != null) {
            // 环比计算
            Increment increment = calcIncrement(count, momStatisticsDTO);
            commonStatisticWithRateVO.setMomValue(increment.getValue());
            commonStatisticWithRateVO.setMomRate(increment.getRate());
            commonStatisticWithRateVO.setMomTrend(increment.getTrend().getValue());
        }
        if (yoyStatisticsDTO != null) {
            // 同比计算
            Increment increment = calcIncrement(count, yoyStatisticsDTO);
            commonStatisticWithRateVO.setYoyValue(increment.getValue());
            commonStatisticWithRateVO.setYoyRate(increment.getRate());
            commonStatisticWithRateVO.setYoyTrend(increment.getTrend().getValue());
        }
        return commonStatisticWithRateVO;
    }

    /**
     * 根据当前统计值与上一时间点的统计数据计算数据增量
     *
     * @param count                 当前统计值
     * @param previousStatisticsDTO 上一时间点统计数据
     * @return 数据增量
     */
    private Increment calcIncrement(Long count, StatisticsDTO previousStatisticsDTO) {
        Long previousCount = getCountFromSerializedData(previousStatisticsDTO.getValue());
        // 增量计算
        long value = count - previousCount;
        // 从无到有的初始增长率认定为1
        float rate = 1f;
        if (previousCount > 0) {
            rate = (count - previousCount) / (float) previousCount;
        }
        DataTrendEnum trend = DataTrendEnum.NOT_CHANGE;
        if (value > 0) {
            trend = DataTrendEnum.UP;
        } else if (value < 0) {
            trend = DataTrendEnum.DOWN;
        }
        return new Increment(value, rate, trend);
    }

    /**
     * 数据增量
     */
    @AllArgsConstructor
    @Getter
    static class Increment {
        // 增量值
        private final long value;
        // 增量比率
        private final float rate;
        // 增量趋势
        private final DataTrendEnum trend;
    }
}
