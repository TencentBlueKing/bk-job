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
    <div class="platform-dashboard" v-bkloading="{ isLoading: isLoading, opacity: 0.8 }">
        <card-layout title="Linux OS">
            <div class="container">
                <div class="nums">{{ data.LINUX | formatNumber }}</div>
                <div
                    ref="LINUX"
                    class="dashboard"
                    v-bk-tooltips.right="calcPercentage(data.LINUX)"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon type="linux" class="platform-flag" style="font-size: 38px;" />
        </card-layout>
        <card-layout title="Windows OS">
            <div class="container">
                <div class="nums">{{ data.WINDOWS | formatNumber }}</div>
                <div
                    ref="WINDOWS"
                    class="dashboard"
                    v-bk-tooltips.right="calcPercentage(data.WINDOWS)"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon type="windows" class="platform-flag" style="font-size: 28px;" />
        </card-layout>
        <card-layout title="AIX OS">
            <div class="container">
                <div class="nums">{{ data.AIX | formatNumber }}</div>
                <div
                    ref="AIX"
                    class="dashboard"
                    v-bk-tooltips.right="calcPercentage(data.AIX)"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon type="aix" class="platform-flag" style="font-size: 24px;" />
        </card-layout>
        <card-layout title="其他 OS">
            <div class="container">
                <div class="nums">{{ data.OTHERS | formatNumber }}</div>
                <div
                    ref="OTHERS"
                    class="dashboard"
                    v-bk-tooltips.right="calcPercentage(data.OTHERS)"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon type="others" class="platform-flag" style="font-size: 28px;" />
        </card-layout>
    </div>
</template>
<script>
    import echarts from 'lib/echarts.min.js';
    import StatisticsService from '@service/statistics';
    import {
        formatNumber,
    } from '@utils/assist';

    import CardLayout from '../card-layout';

    export default {
        name: '',
        components: {
            CardLayout,
        },
        filters: {
            formatNumber (value) {
                return formatNumber(value);
            },
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
                    AIX: 0,
                    LINUX: 0,
                    OTHERS: 0,
                    WINDOWS: 0,
                },
            };
        },
        watch: {
            date () {
                this.fetchData();
            },
        },
        mounted () {
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                StatisticsService.fetchDistributionMetrics({
                    date: this.date,
                    metric: 'HOST_SYSTEM_TYPE',
                }).then((data) => {
                    this.data = data.labelAmountMap;
                    this.init();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            init () {
                const typeList = [
                    'LINUX',
                    'WINDOWS',
                    'AIX',
                    'OTHERS',
                ];
                
                typeList.forEach((typeItem) => {
                    if (!this.$refs.LINUX) {
                        return;
                    }
                    const other = typeList.reduce((result, item) => {
                        if (item === typeItem) {
                            return result;
                        }
                        return result + this.data[item];
                    }, 0);
                    const myChart = echarts.init(this.$refs[typeItem]);
                    myChart.setOption({
                        series: [
                            {
                                type: 'pie',
                                radius: [
                                    '8',
                                    '12',
                                ],
                                hoverOffset: 0,
                                label: {
                                    show: false,
                                },
                                data: [
                                    {
                                        value: this.data[typeItem],
                                        itemStyle: {
                                            color: '#85CCA8',
                                        },
                                    },
                                    {
                                        value: other,
                                        itemStyle: {
                                            color: '#EBECF0',
                                        },
                                        emphasis: {
                                            itemStyle: {
                                                color: '#F0F1F5',
                                            },
                                        },
                                    },
                                ],
                            },
                        ],
                    });
                });
            },
            calcPercentage (value) {
                const {
                    AIX,
                    LINUX,
                    OTHERS,
                    WINDOWS,
                } = this.data;
                const total = parseInt(AIX, 10) + parseInt(LINUX, 10) + parseInt(OTHERS, 10) + parseInt(WINDOWS, 10);
                if (!total) {
                    return '0 %';
                }
                return `${Math.round(value / total * 100).toFixed(2)} %`;
            },
        },
    };
</script>
<style lang='postcss'>
    .platform-dashboard {
        display: flex;
        height: 100%;
        color: #e5e6eb;

        .dashboard-card-layout {
            position: relative;
            flex: 1;
            border: none;

            &:nth-child(n+2) {
                border-left: 1px solid #f0f1f5;
            }

            .card-title {
                color: #979ba5;
            }
        }

        .container {
            display: flex;
            align-items: center;

            .dashboard {
                margin-left: 10px;
            }
        }

        .nums {
            font-size: 24px;
            font-weight: bold;
            color: #63656e;
        }

        .platform-flag {
            position: absolute;
            bottom: 24px;
            left: 20px;
        }
    }
</style>
