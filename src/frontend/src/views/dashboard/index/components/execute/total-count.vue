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
    <card-layout class="execute-total-dashboard" :title="$t('dashboard.累计任务执行次数')">
        <render-trend metric="EXECUTED_TASK_COUNT" :date="date" />
        <div slot="extend">
            <Icon type="line-chart-line" v-bk-tooltips="$t('dashboard.查看趋势图')" @click="handleShowTrend" />
            <Icon type="table-line" v-bk-tooltips="$t('dashboard.查看列表')" @click="handleShowList" />
        </div>
        <trend-dialog
            v-model="isShowTrend"
            metric="EXECUTED_TASK_COUNT"
            :title="$t('dashboard.累计任务执行次数趋势图')"
            :name="$t('dashboard.累计任务执行次数')"
            :date="date" />
        <lower-component level="custom" :custom="isShowList">
            <jb-dialog
                v-model="isShowList"
                :title="$t('dashboard.累计任务执行次数列表')"
                :width="520"
                :show-footer="false"
                header-position="left">
                <div style="margin-top: 12px;" v-bkloading="{ isLoading }">
                    <bk-table :data="listData" :max-height="420">
                        <bk-table-column
                            :label="$t('dashboard.业务名')"
                            prop="appName"
                            key="appName"
                            align="left" />
                        <bk-table-column
                            :label="$t('dashboard.执行数')"
                            prop="value"
                            key="value"
                            align="left" />
                        <bk-table-column
                            :label="$t('dashboard.占比')"
                            prop="ratio"
                            key="ratio"
                            align="left" />
                    </bk-table>
                </div>
            </jb-dialog>
        </lower-component>
    </card-layout>
</template>
<script>
    import StatisticsService from '@service/statistics';
    import CardLayout from '../card-layout';
    import RenderTrend from '../common/render-trend';
    import TrendDialog from '../common/trend-dialog';

    export default {
        name: '',
        components: {
            CardLayout,
            RenderTrend,
            TrendDialog,
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
                isShowTrend: false,
                isShowList: false,
                listData: [],
            };
        },
        methods: {
            handleShowTrend () {
                this.isShowTrend = true;
            },
            handleShowList () {
                this.isShowList = true;
                this.isLoading = true;
                StatisticsService.fetchListByPerAppMetrics({
                    metric: 'EXECUTED_TASK_COUNT',
                }).then((data) => {
                    this.listData = Object.freeze(data);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .execute-total-dashboard {
        .wraper {
            display: flex;
            flex-direction: column;
            margin-right: auto;
            font-size: 12px;
            line-height: 16px;
            color: #babcc2;

            .total {
                height: 32px;
                font-size: 24px;
                font-weight: bold;
                line-height: 32px;
                color: #63656e;
            }
        }
    }
</style>
