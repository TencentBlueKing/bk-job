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
        class="script-cound-card"
        :title="$t('dashboard.脚本量')">
        <render-trend
            :date="date"
            metric="SCRIPT_COUNT" />
        <div slot="extend">
            <Icon
                v-bk-tooltips="$t('dashboard.查看趋势图')"
                type="line-chart-line"
                @click="handleShowTrend" />
            <Icon
                v-bk-tooltips="$t('dashboard.查看列表')"
                type="table-line"
                @click="handleShowList" />
        </div>
        <trend-dialog
            v-model="isShowTrend"
            :date="date"
            metric="SCRIPT_COUNT"
            :name="$t('dashboard.脚本量')"
            :title="$t('dashboard.脚本量趋势图')" />
        <lower-component
            :custom="isShowList"
            level="custom">
            <jb-dialog
                v-model="isShowList"
                header-position="left"
                :show-footer="false"
                :title="$t('dashboard.脚本量列表')"
                :width="520">
                <div
                    v-bkloading="{ isLoading: isListLoading }"
                    style="margin-top: 12px;">
                    <bk-table
                        :data="listData"
                        :max-height="420">
                        <bk-table-column
                            key="scopeName"
                            align="left"
                            :label="$t('dashboard.业务名')"
                            prop="scopeName" />
                        <bk-table-column
                            key="value"
                            align="left"
                            :label="$t('dashboard.脚本量')"
                            prop="value" />
                        <bk-table-column
                            key="ratio"
                            align="left"
                            :label="$t('dashboard.占比')"
                            prop="ratio" />
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
