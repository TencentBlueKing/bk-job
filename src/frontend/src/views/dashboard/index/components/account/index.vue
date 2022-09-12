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
        class="account-dashboard">
        <card-layout :title="$t('dashboard.Linux 账号数')">
            <div class="item-content">
                <div class="nums">
                    {{ data.LINUX }}
                </div>
                <div
                    ref="LINUX"
                    v-bk-tooltips.right="calcPercentage(data.LINUX)"
                    class="dashboard"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon
                class="type-flag"
                style="font-size: 37px;"
                type="linux" />
        </card-layout>
        <card-layout :title="$t('dashboard.Windows 账号数')">
            <div class="item-content">
                <div class="nums">
                    {{ data.WINDOWS | formatNumber }}
                </div>
                <div
                    ref="WINDOWS"
                    v-bk-tooltips.right="calcPercentage(data.WINDOWS)"
                    class="dashboard"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon
                class="type-flag"
                style="font-size: 36px;"
                type="windows" />
        </card-layout>
        <card-layout :title="$t('dashboard.DB 账号数')">
            <div class="item-content">
                <div class="nums">
                    {{ data.DB | formatNumber }}
                </div>
                <div
                    ref="DB"
                    v-bk-tooltips.right="calcPercentage(data.DB)"
                    class="dashboard"
                    style="width: 24px; height: 24px;" />
            </div>
            <Icon
                class="type-flag"
                style="font-size: 27px;"
                type="db" />
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
                    DB: 0,
                    LINUX: 0,
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
                    metric: 'ACCOUNT_TYPE',
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
                    'DB',
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
                const total = parseInt(this.data.DB, 10)
                    + parseInt(this.data.LINUX, 10) + parseInt(this.data.WINDOWS, 10);
                if (!total) {
                    return '0 %';
                }
                return `${Math.round(value / total * 100).toFixed(2)} %`;
            },
        },
    };
</script>
<style lang='postcss'>
    @import "@/css/mixins/media";

    .account-dashboard {
        .dashboard-card-layout {
            border: none;

            &:nth-child(n+2) {
                border-top: 1px solid #f0f1f5;
            }

            @media (--huge-viewports) {
                width: 400px;
            }

            @media (--large-viewports) {
                width: 300px;
            }

            @media (--medium-viewports) {
                width: 280px;
            }

            @media (--small-viewports) {
                width: 260px;
            }
        }

        .item-content {
            position: relative;
            display: flex;
            align-items: center;
            font-size: 24px;
            font-weight: bold;
            line-height: 32px;
            color: #63656e;

            .dashboard {
                margin-left: 10px;
            }
        }

        .type-flag {
            position: absolute;
            right: 24px;
            bottom: 24px;
            color: #e5e6eb;
        }
    }
</style>
