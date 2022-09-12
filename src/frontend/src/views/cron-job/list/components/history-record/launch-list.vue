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
    <div class="cron-job-history-launch-record">
        <list-action-layout>
            <jb-search-select
                ref="search"
                :append-value="searchAppendValue"
                :data="searchSelect"
                :parse-url="false"
                :placeholder="$t('cron.搜索指定任务ID 或 根据字段筛选结果')"
                style="width: 510px;"
                @on-change="handleSearch" />
            <template #right>
                <bk-date-picker
                    ref="datePicker"
                    :clearable="false"
                    :placeholder="$t('cron.选择日期')"
                    :shortcut-close="true"
                    :shortcuts="shortcuts"
                    type="datetimerange"
                    up-to-now
                    :use-shortcut-text="true"
                    :value="defaultDateTime"
                    @change="handleDateChange" />
            </template>
        </list-action-layout>
        <render-list
            ref="list"
            class="executive-history-table"
            :data-source="fetchExecutionHistoryList"
            ignore-url
            :search-control="() => $refs.search">
            <bk-table-column
                key="id"
                align="left"
                label="ID"
                prop="id"
                width="130">
                <template slot-scope="{ row }">
                    <bk-button
                        text
                        @click="handleGoDetail(row)">
                        {{ row.id }}
                    </bk-button>
                </template>
            </bk-table-column>
            <bk-table-column
                key="statusDesc"
                align="left"
                :label="$t('cron.任务状态.colHead')"
                prop="statusDesc">
                <template slot-scope="{ row }">
                    <span v-html="row.statusDescHtml" />
                </template>
            </bk-table-column>
            <bk-table-column
                key="operator"
                align="left"
                :label="$t('cron.执行人.colHead')"
                prop="operator"
                width="160" />
            <bk-table-column
                key="createTime"
                align="left"
                :label="$t('cron.开始时间.colHead')"
                prop="createTime"
                width="180" />
            <bk-table-column
                key="totalTimeText"
                align="right"
                :label="$t('cron.耗时时长')"
                prop="totalTimeText"
                width="160" />
        </render-list>
    </div>
</template>
<script>
    import NotifyService from '@service/notify';
    import TaskExecuteService from '@service/task-execute';

    import { prettyDateTimeFormat } from '@utils/assist';

    import JbSearchSelect from '@components/jb-search-select';
    import ListActionLayout from '@components/list-action-layout';
    import RenderList from '@components/render-list';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            JbSearchSelect,
            ListActionLayout,
            RenderList,
        },
        props: {
            data: {
                type: Object,
                required: true,
            },
            showFaild: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                searchParams: {},
                defaultDateTime: [prettyDateTimeFormat(Date.now() - 29 * 86400000), ''],
                searchAppendValue: [],
            };
        },
        created () {
            this.fetchExecutionHistoryList = TaskExecuteService.fetchExecutionHistoryList;
            if (this.showFaild) {
                this.searchParams.status = 4;
                this.searchAppendValue = [
                    {
                        name: I18n.t('cron.任务状态.label'),
                        id: 'status',
                        values: [{
                            id: this.searchParams.status,
                            name: I18n.t('cron.执行失败'),
                        }],
                    },
                ];
            }
            const [startTime, endTime] = this.defaultDateTime;
            this.searchParams.startTime = startTime;
            this.searchParams.endTime = endTime;
            
            this.searchSelect = [
                {
                    name: 'ID',
                    id: 'taskInstanceId',
                    description: I18n.t('cron.搜索条件带任务ID时，将自动忽略其他条件'),
                    default: true,
                    validate (values, item) {
                        const validate = (values || []).every(_ => /^(\d*)$/.test(_.name));
                        return !validate ? I18n.t('cron.ID只支持数字') : true;
                    },
                },
                {
                    name: I18n.t('cron.任务状态.colHead'),
                    id: 'status',
                    children: [
                        {
                            name: I18n.t('cron.等待执行'),
                            id: 1,
                        },
                        {
                            name: I18n.t('cron.正在执行'),
                            id: 2,
                        },
                        {
                            name: I18n.t('cron.执行成功'),
                            id: 3,
                        },
                        {
                            name: I18n.t('cron.执行失败'),
                            id: 4,
                        },
                        {
                            name: I18n.t('cron.等待确认'),
                            id: 7,
                        },
                        {
                            name: I18n.t('cron.强制终止中'),
                            id: 10,
                        },
                        {
                            name: I18n.t('cron.强制终止成功'),
                            id: 11,
                        },
                        {
                            name: I18n.t('cron.强制终止失败'),
                            id: 12,
                        },
                        {
                            name: I18n.t('cron.确认终止'),
                            id: 13,
                        },
                    ],
                },
                {
                    name: I18n.t('cron.执行人.colHead'),
                    id: 'operator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
            ];
            
            this.shortcuts = [
                {
                    text: I18n.t('cron.近1小时'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 3600000);
                        return [start, end];
                    },
                },
                {
                    text: I18n.t('cron.近12小时'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 43200000);
                        return [start, end];
                    },
                },
                {
                    text: I18n.t('cron.近1天'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 86400000);
                        return [start, end];
                    },
                },
                {
                    text: I18n.t('cron.近7天'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 604800000);
                        return [start, end];
                    },
                },
            ];
        },
        mounted () {
            this.fetchData();
            this.$refs.datePicker.shortcut = {
                text: `${this.defaultDateTime[0]} ${I18n.t('cron.至今')}`,
            };
        },
        methods: {
            fetchData () {
                const searchParams = {
                    ...this.searchParams,
                    cronTaskId: this.data.id,
                };
                this.$refs.list.$emit('onFetch', searchParams);
            },
            handleSearch (payload) {
                const { startTime, endTime } = this.searchParams;
                this.searchParams = {
                    ...payload,
                    startTime,
                    endTime,
                };
                this.fetchData();
            },
            handleDateChange (date, type) {
                if (type === 'upToNow') {
                    this.setToNowText(date);
                }
                this.searchParams.startTime = date[0];// eslint-disable-line prefer-destructuring
                this.searchParams.endTime = type === 'upToNow' ? '' : date[1];
                this.fetchData();
            },
            handleGoDetail (taskInstance) {
                let router = null;
                if (taskInstance.isTask) {
                    router = this.$router.resolve({
                        name: 'historyTask',
                        params: {
                            id: taskInstance.id,
                        },
                    });
                } else {
                    router = this.$router.resolve({
                        name: 'historyStep',
                        params: {
                            taskInstanceId: taskInstance.id,
                        },
                        query: {
                            from: 'historyList',
                        },
                    });
                }
                window.open(router.href);
            },
        },
    };
</script>
