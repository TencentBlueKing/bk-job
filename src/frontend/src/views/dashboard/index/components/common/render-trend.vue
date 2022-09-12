<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
    <div
        v-bkloading="{ isLoading, opacity: 0.8 }"
        class="render-trend-box">
        <div class="data-wraper">
            <div class="total">
                {{ data.count }}
            </div>
            <div
                class="data-row"
                :class="{
                    'down': data.yoyTrend < 0,
                    'up': data.yoyTrend > 0,
                    'equal': data.yoyTrend === 0,
                }"
                style="margin-top: 12px;">
                <span class="label">{{ $t('dashboard.同比') }}</span>
                <span>
                    <Icon
                        v-if="data.yoyTrend < 0"
                        type="decrease-line" />
                    <Icon
                        v-if="data.yoyTrend === 0"
                        type="equally" />
                    <Icon
                        v-if="data.yoyTrend > 0"
                        type="increase-line" />
                </span>
                <span>{{ data.yoyValue }}</span>
            </div>
            <div
                class="data-row"
                :class="{
                    'down': data.momTrend < 0,
                    'up': data.momTrend > 0,
                    'equal': data.momTrend === 0,
                }"
                style="margin-top: 10px;">
                <span class="label">{{ $t('dashboard.环比') }}</span>
                <span>
                    <Icon
                        v-if="data.momTrend < 0"
                        type="decrease-line" />
                    <Icon
                        v-if="data.momTrend === 0"
                        type="equally" />
                    <Icon
                        v-if="data.momTrend > 0"
                        type="increase-line" />
                </span>
                <span>{{ data.momValue }}</span>
            </div>
        </div>
        <div class="data-chart">
            <div
                ref="trend"
                style="width: 100%; height: 60px;" />
        </div>
    </div>
</template>
<script>
    import echarts from 'lib/echarts.min.js';
    import _ from 'lodash';

    import StatisticsService from '@service/statistics';

    import { prettyDateFormat } from '@utils/assist';
    
    const styleMap = {
        up: {
            itemStyle: {
                color: '#4BC7AD',
            },
            areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0,
                        color: 'rgba(75, 199, 173, 0.32)',
                    },
                    {
                        offset: 1,
                        color: 'rgba(75, 199, 173, 0)',
                    },
                ]),
            },
        },
        down: {
            itemStyle: {
                color: '#FF5656',
            },
            areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0,
                        color: 'rgba(255, 86, 86, 0.28)',
                    },
                    {
                        offset: 1,
                        color: 'rgba(255, 86, 86, 0)',
                    },
                ]),
            },
        },
    };

    export default {
        name: '',
        props: {
            metric: {
                type: String,
                required: true,
            },
            date: {
                type: String,
                required: true,
            },
        },
        data () {
            return {
                isLoading: true,
                data: {
                    count: 0,
                    momRate: 0,
                    yoyRate: 0,
                },
            };
        },
        watch: {
            date () {
                this.fetchData();
            },
        },
        mounted () {
            this.trendData = [];
            this.fetchData();
            const resize = _.throttle(() => {
                this.myChart && this.myChart.resize();
            }, 300);
            window.addEventListener('resize', resize);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', resize);
            });
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                Promise.all([
                    StatisticsService.fetchTotalMetrics({
                        date: this.date,
                        metric: this.metric,
                    }).then((data) => {
                        this.data = data || {};
                    }),
                    StatisticsService.fetchTrendsMetrics({
                        endDate: this.date,
                        startDate: prettyDateFormat(new Date(this.date) - 6 * 86400000),
                        metric: this.metric,
                    }).then((data) => {
                        this.initTrend(data);
                    }),
                ]).finally(() => {
                    this.isLoading = false;
                });
            },
            initTrend (data) {
                this.myChart = echarts.init(this.$refs.trend);
                const dateList = [];
                const valueList = [];
                data.forEach(({ date, value }) => {
                    dateList.push(date);
                    valueList.push(value);
                });

                const styles = _.head(valueList) <= _.last(valueList) ? 'up' : 'down';

                this.myChart.setOption({
                    grid: {
                        top: 5,
                        right: 5,
                        bottom: 5,
                        left: 5,
                    },
                    xAxis: {
                        boundaryGap: false,
                        data: dateList,
                        show: false,
                    },
                    yAxis: {
                        type: 'value',
                        min: 'dataMin',
                        max: 'dataMax',
                        boundaryGap: [
                            0,
                            '100%',
                        ],
                        show: false,
                    },
                    series: [
                        {
                            name: 'trend',
                            type: 'line',
                            symbol: 'none',
                            sampling: 'average',
                            ...styleMap[styles],
                            data: valueList,
                        },
                    ],
                });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .render-trend-box {
        display: flex;
        align-items: flex-end;

        .data-wraper {
            display: flex;
            margin-right: auto;
            font-size: 12px;
            line-height: 16px;
            color: #babcc2;
            white-space: nowrap;
            /* stylelint-disable declaration-block-no-redundant-longhand-properties */
            flex-direction: column;
            flex-wrap: nowrap;

            .total {
                font-size: 24px;
                font-weight: 500;
                line-height: 32px;
                color: #63656e;
            }

            .data-row {
                display: flex;
                align-items: center;

                &.up {
                    color: #4bc7ad;
                }

                &.down {
                    color: #ff5656;
                }
            }

            .label {
                margin-right: 6px;
                color: #babcc2;
            }
        }

        .data-chart {
            flex: 1;
            padding-left: 30px;
            overflow: hidden;
        }
    }
</style>
