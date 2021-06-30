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
    <card-layout class="script-cound-card" :title="$t('dashboard.脚本量')">
        <render-trend metric="SCRIPT_COUNT" :date="date" />
        <div slot="extend">
            <Icon type="line-chart-line" v-bk-tooltips="$t('dashboard.查看趋势图')" @click="handleShowTrend" />
            <Icon type="table-line" v-bk-tooltips="$t('dashboard.查看列表')" @click="handleShowList" />
        </div>
        <trend-dialog
            v-model="isShowTrend"
            metric="SCRIPT_COUNT"
            :title="$t('dashboard.脚本量趋势图')"
            :name="$t('dashboard.脚本量')"
            :date="date" />
        <lower-component level="custom" :custom="isShowList">
            <jb-dialog
                v-model="isShowList"
                :title="$t('dashboard.脚本量列表')"
                :width="520"
                :show-footer="false"
                header-position="left">
                <div style="margin-top: 12px;" v-bkloading="{ isLoading: isListLoading }">
                    <bk-table :data="listData" :max-height="420">
                        <bk-table-column
                            :label="$t('dashboard.业务名')"
                            prop="appName"
                            key="appName"
                            align="left" />
                        <bk-table-column
                            :label="$t('dashboard.脚本量')"
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
                isListLoading: false,
                listData: [],
                isShowTrend: false,
                isShowList: false,
            };
        },
        methods: {
            handleShowTrend () {
                this.isShowTrend = true;
            },
            handleShowList () {
                this.isShowList = true;
                this.isListLoading = true;
                StatisticsService.fetchListByPerAppMetrics({
                    metric: 'SCRIPT_COUNT',
                }).then((data) => {
                    this.listData = Object.freeze(data);
                })
                    .finally(() => {
                        this.isListLoading = false;
                    });
            },
        },
    };
</script>
