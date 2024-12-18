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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试-简单同环比计算器
 */
public class SimpleMomYoyCalculatorTest {

    @Test
    public void testCalc() {
        // 从0上升
        StatisticsDTO statisticsDTO = new StatisticsDTO("150");
        StatisticsDTO momStatisticsDTO = new StatisticsDTO("0");
        StatisticsDTO yoyStatisticsDTO = new StatisticsDTO("0");
        SimpleMomYoyCalculator simpleMomYoyCalculator = new SimpleMomYoyCalculator(
            statisticsDTO,
            momStatisticsDTO,
            yoyStatisticsDTO
        );
        CommonStatisticWithRateVO commonStatisticWithRateVO = simpleMomYoyCalculator.calc();
        assertThat(commonStatisticWithRateVO.getCount()).isEqualTo(150L);
        assertThat(commonStatisticWithRateVO.getMomValue()).isEqualTo(150L);
        assertThat(commonStatisticWithRateVO.getMomRate()).isEqualTo(1.0f);
        assertThat(commonStatisticWithRateVO.getMomTrend()).isEqualTo(DataTrendEnum.UP.getValue());
        assertThat(commonStatisticWithRateVO.getYoyValue()).isEqualTo(150L);
        assertThat(commonStatisticWithRateVO.getYoyRate()).isEqualTo(1.0f);
        assertThat(commonStatisticWithRateVO.getYoyTrend()).isEqualTo(DataTrendEnum.UP.getValue());
        // 非0上升
        momStatisticsDTO = new StatisticsDTO("100");
        yoyStatisticsDTO = new StatisticsDTO("50");
        simpleMomYoyCalculator = new SimpleMomYoyCalculator(
            statisticsDTO,
            momStatisticsDTO,
            yoyStatisticsDTO
        );
        commonStatisticWithRateVO = simpleMomYoyCalculator.calc();
        assertThat(commonStatisticWithRateVO.getCount()).isEqualTo(150L);
        assertThat(commonStatisticWithRateVO.getMomValue()).isEqualTo(50L);
        assertThat(commonStatisticWithRateVO.getMomRate()).isEqualTo(0.5f);
        assertThat(commonStatisticWithRateVO.getMomTrend()).isEqualTo(DataTrendEnum.UP.getValue());
        assertThat(commonStatisticWithRateVO.getYoyValue()).isEqualTo(100L);
        assertThat(commonStatisticWithRateVO.getYoyRate()).isEqualTo(2.0f);
        assertThat(commonStatisticWithRateVO.getYoyTrend()).isEqualTo(DataTrendEnum.UP.getValue());
        // 下降
        statisticsDTO = new StatisticsDTO("50");
        momStatisticsDTO = new StatisticsDTO("100");
        yoyStatisticsDTO = new StatisticsDTO("200");
        simpleMomYoyCalculator = new SimpleMomYoyCalculator(
            statisticsDTO,
            momStatisticsDTO,
            yoyStatisticsDTO
        );
        commonStatisticWithRateVO = simpleMomYoyCalculator.calc();
        assertThat(commonStatisticWithRateVO.getCount()).isEqualTo(50L);
        assertThat(commonStatisticWithRateVO.getMomValue()).isEqualTo(-50L);
        assertThat(commonStatisticWithRateVO.getMomRate()).isEqualTo(-0.5f);
        assertThat(commonStatisticWithRateVO.getMomTrend()).isEqualTo(DataTrendEnum.DOWN.getValue());
        assertThat(commonStatisticWithRateVO.getYoyValue()).isEqualTo(-150L);
        assertThat(commonStatisticWithRateVO.getYoyRate()).isEqualTo(-0.75f);
        assertThat(commonStatisticWithRateVO.getYoyTrend()).isEqualTo(DataTrendEnum.DOWN.getValue());
    }
}
