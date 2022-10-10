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
    <card-layout
        v-bkloading="{ isLoading, opacity: 0.8 }"
        class="crontab-status-dashboard"
        :title="$t('dashboard.定时任务开关分布')">
        <div class="wraper">
            <div
                ref="dashboard"
                style="width: 176px; height: 176px;" />
            <div class="item-list">
                <div
                    v-for="item in list"
                    :key="item.key"
                    class="item"
                    @mouseover="handleMouseover(item.label)">
                    <div :style="calcItemCircleStyles(item.key)" />
                    <div>{{ item.label }}</div>
                </div>
            </div>
        </div>
    </card-layout>
</template>
<script>
    import echarts from 'lib/echarts.min.js';
    import _ from 'lodash';

    import StatisticsService from '@service/statistics';

    import CardLayout from '../card-layout';
    import {
        chartsOptionsBase,
    } from '../common/assist';

    import I18n from '@/i18n';

    const colorMap = {
        OPEN: '#85CCA8',
        CLOSED: '#F5BABA',
    };

    export default {
        name: '',
        components: {
            CardLayout,
        },
        props: {
            date: {
                type: String,
                required: true,
            },
        },
        data () {
            return {
                isLoading: true,
                data: {
                    CLOSED: 0,
                    OPEN: 0,
                },
            };
        },
        watch: {
            date () {
                this.fetchData();
            },
        },
        created () {
            this.list = [
                {
                    label: I18n.t('dashboard.开启'),
                    key: 'OPEN',
                },
                {
                    label: I18n.t('dashboard.关闭'),
                    key: 'CLOSED',
                },
            ];
        },
        mounted () {
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                StatisticsService.fetchDistributionMetrics({
                    date: this.date,
                    metric: 'CRON_STATUS',
                }).then((data) => {
                    this.data = data.labelAmountMap;
                    this.init();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            init () {
                this.myChart = echarts.init(this.$refs.dashboard);
                const data = [];
                let maxItem = this.list[0];// eslint-disable-line prefer-destructuring
                this.list.forEach((item) => {
                    const currentValue = this.data[item.key];
                    if (this.data[maxItem.key] < currentValue) {
                        maxItem = item;
                    }
                    data.push({
                        value: currentValue,
                        key: item.key,
                        name: item.label,
                        itemStyle: {
                            color: colorMap[item.key],
                        },
                    });
                });
                setTimeout(() => {
                    this.myChart.dispatchAction({ type: 'highlight', name: maxItem.label });
                });
                const options = {
                    ...chartsOptionsBase,
                    series: [
                        {
                            type: 'pie',
                            radius: [
                                '60',
                                '80',
                            ],
                            selectedMode: 'single',
                            hoverOffset: 8,
                            selectedOffset: 0,
                            avoidLabelOverlap: false,
                            label: {
                                normal: {
                                    show: false,
                                    position: 'center',
                                },
                                emphasis: {
                                    show: true,
                                    formatter: [
                                        '{value|{d}%}',
                                        '{b}',
                                    ].join('\n'),

                                    rich: {
                                        name: {
                                            fontSize: 12,
                                            lineHieght: 16,
                                            color: '#63656E',
                                        },
                                        value: {
                                            fontSize: 26,
                                            fontWeight: 600,
                                            lineHeight: 42,
                                            color: '#63656E',
                                        },
                                    },
                                },
                            },
                            data,
                        },
                    ],
                };
                this.myChart.setOption(options);
                this.myChart.dispatchAction({ type: 'highlight', name: maxItem.label });
                this.myChart.on('mouseover', (params) => {
                    this.handleMouseover(params.data.name);
                });
            },
            calcItemCircleStyles (key) {
                return {
                    width: '8px',
                    height: '8px',
                    marginRight: '6px',
                    borderRadius: '50%',
                    backgroundColor: colorMap[key],
                };
            },
            handleMouseover (label) {
                const others = _.filter(this.list, _ => _.label !== label).map(_ => _.label);
                this.myChart.dispatchAction({ type: 'highlight', name: label });
                this.myChart.dispatchAction({ type: 'downplay', name: others });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .crontab-status-dashboard {
        .wraper {
            display: flex;
            align-items: center;
            flex-direction: column;

            .item-list {
                display: flex;
                width: 180px;
                margin-top: 20px;
                font-size: 12px;
                line-height: 18px;
                color: #979ba5;
                flex-direction: column;
                align-items: center;

                .item {
                    display: flex;
                    padding-left: 10px;
                    cursor: pointer;
                    flex: 1 1 50%;
                    align-items: center;

                    &:nth-child(n + 1) {
                        margin-top: 10px;
                    }
                }
            }
        }
    }
</style>
