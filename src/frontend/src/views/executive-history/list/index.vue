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
    <div class="executive-history-page">
        <list-action-layout>
            <jb-search-select
                ref="search"
                @on-change="handleSearch"
                :data="searchSelect"
                :placeholder="$t('history.搜索任务ID，任务名称，执行方式，任务类型，任务状态，执行人...')"
                style="width: 600px;" />
            <template #right>
                <bk-date-picker
                    ref="datePicker"
                    :value="defaultDateTime"
                    :placeholder="$t('history.选择日期')"
                    :shortcuts="shortcuts"
                    type="datetimerange"
                    :shortcut-close="true"
                    :use-shortcut-text="true"
                    :clearable="false"
                    up-to-now
                    @change="handleDateChange" />
            </template>
        </list-action-layout>
        <render-list
            ref="list"
            :data-source="fetchExecutionHistoryList"
            :search-control="() => $refs.search"
            class="executive-history-table"
            v-test="{ type: 'list', value: 'execHistory' }">
            <bk-table-column
                v-if="allRenderColumnMap.id"
                label="ID"
                prop="id"
                key="id"
                width="130"
                align="left">
                <template slot-scope="{ row }">
                    <auth-button
                        :permission="row.canView"
                        auth="task_instance/view"
                        :resource-id="row.id"
                        text
                        @click="handleGoDetail(row)">
                        {{ row.id }}
                    </auth-button>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.name"
                :label="$t('history.任务名称.colHead')"
                prop="name"
                key="name"
                min-width="200"
                align="left"
                show-overflow-tooltip />
            <bk-table-column
                v-if="allRenderColumnMap.startupModeDesc"
                :label="$t('history.执行方式.colHead')"
                prop="startupModeDesc"
                key="startupModeDesc"
                width="120"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.typeDesc"
                :label="$t('history.任务类型.colHead')"
                prop="typeDesc"
                key="typeDesc"
                width="140"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.statusDesc"
                :label="$t('history.任务状态.colHead')"
                prop="statusDesc"
                key="statusDesc"
                width="140"
                align="left">
                <template slot-scope="{ row }">
                    <Icon
                        svg
                        :type="row.statusIconType"
                        :class="{
                            'rotate-loading': row.isDoing,
                        }"
                        style="font-size: 16px; color: #3a84ff; vertical-align: middle;" />
                    <span style="vertical-align: middle;">{{ row.statusDesc }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.operator"
                :label="$t('history.执行人.colHead')"
                prop="operator"
                key="operator"
                width="140"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.createTime"
                :label="$t('history.开始时间.colHead')"
                prop="createTime"
                key="createTime"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.totalTimeText"
                :label="$t('history.耗时时长')"
                prop="totalTimeText"
                key="totalTimeText"
                width="130"
                align="right" />
            <bk-table-column
                :label="$t('history.操作')"
                width="150"
                key="action"
                fixed="right"
                align="left">
                <template slot-scope="{ row }">
                    <auth-button
                        :permission="row.canView"
                        auth="task_instance/view"
                        :resource-id="row.id"
                        text
                        @click="handleGoDetail(row)">
                        {{ $t('history.查看详情') }}
                    </auth-button>
                    <auth-button
                        v-if="!redoRequestMap[row.id]"
                        :permission="row.canExecute"
                        auth="task_instance/redo"
                        :resource-id="row.id"
                        text
                        @click="handleGoRetry(row)">
                        {{ $t('history.去重做') }}
                    </auth-button>
                    <span
                        v-else
                        class="task-redo-loading ml10"
                        :data-text="$t('history.去重做')">
                        <Icon
                            svg
                            type="sync-pending"
                            class="rotate-loading" />
                    </span>
                </template>
            </bk-table-column>
            <bk-table-column type="setting">
                <bk-table-setting-content
                    :fields="tableColumn"
                    :selected="selectedTableColumn"
                    :size="tableSize"
                    @setting-change="handleSettingChange" />
            </bk-table-column>
        </render-list>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TaskExecuteService from '@service/task-execute';
    import NotifyService from '@service/notify';
    import { prettyDateTimeFormat } from '@utils/assist';
    import { listColumnsCache } from '@utils/cache-helper';
    import { IPRule } from '@utils/validator';
    import ListActionLayout from '@components/list-action-layout';
    import RenderList from '@components/render-list';
    import JbSearchSelect from '@components/jb-search-select';

    const TABLE_COLUMN_CACHE = 'execute_history_list_columns';

    export default {
        name: '',
        components: {
            ListActionLayout,
            RenderList,
            JbSearchSelect,
        },
        data () {
            return {
                showOperation: false,
                searchParams: {
                    startTime: '',
                    endTime: '',
                },
                defaultDateTime: [
                    '', '',
                ],
                selectedTableColumn: [],
                tableSize: 'small',
                redoRequestMap: {},
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.$refs.list.isLoading;
            },
            searchInfoEnable () {
                return !!this.searchParams.taskInstanceId;
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
        },
        created () {
            this.parseDefaultDateTime();
            this.fetchExecutionHistoryList = TaskExecuteService.fetchExecutionHistoryList;
            this.searchSelect = [
                {
                    name: 'ID',
                    id: 'taskInstanceId',
                    description: I18n.t('history.将覆盖其它条件'),
                    validate (values, item) {
                        const validate = values.every(_ => /^(\d*)$/.test(_.name));
                        return !validate ? I18n.t('history.ID只支持数字') : true;
                    },
                },
                {
                    name: I18n.t('history.任务名称.colHead'),
                    id: 'taskName',
                    default: true,
                },
                {
                    name: I18n.t('history.目标 IP'),
                    id: 'ip',
                    validate (values, item) {
                        const validate = values.every(_ => IPRule.validator(_.name));
                        return !validate ? IPRule.message : true;
                    },
                },
                {
                    name: I18n.t('history.执行耗时'),
                    id: 'totalTimeType',
                    children: [
                        {
                            name: '≤ 60s',
                            id: 'LESS_THAN_ONE_MINUTE',
                        },
                        {
                            name: '＞60s and ≤ 600s',
                            id: 'ONE_MINUTE_TO_TEN_MINUTES',
                        },
                        {
                            name: '＞ 600s',
                            id: 'MORE_THAN_TEN_MINUTES',
                        },
                    ],
                },
                {
                    name: I18n.t('history.执行方式.colHead'),
                    id: 'startupModes',
                    children: [
                        {
                            name: I18n.t('history.页面执行'),
                            id: 1,
                        },
                        {
                            name: I18n.t('history.定时执行'),
                            id: 3,
                        },
                        {
                            name: I18n.t('history.API调用'),
                            id: 2,
                        },
                    ],
                },
                {
                    name: I18n.t('history.任务类型.colHead'),
                    id: 'taskType',
                    children: [
                        {
                            name: I18n.t('history.作业执行'),
                            id: 0,
                        },
                        {
                            name: I18n.t('history.脚本执行'),
                            id: 1,
                        },
                        {
                            name: I18n.t('history.文件分发'),
                            id: 2,
                        },
                    ],
                },
                {
                    name: I18n.t('history.任务状态.colHead'),
                    id: 'status',
                    children: [
                        {
                            name: I18n.t('history.等待执行'),
                            id: 1,
                        },
                        {
                            name: I18n.t('history.正在执行'),
                            id: 2,
                        },
                        {
                            name: I18n.t('history.执行成功'),
                            id: 3,
                        },
                        {
                            name: I18n.t('history.执行失败'),
                            id: 4,
                        },
                        {
                            name: I18n.t('history.等待确认'),
                            id: 7,
                        },
                        {
                            name: I18n.t('history.强制终止中'),
                            id: 10,
                        },
                        {
                            name: I18n.t('history.强制终止成功'),
                            id: 11,
                        },
                        {
                            name: I18n.t('history.确认终止'),
                            id: 13,
                        },
                    ],
                },
                {
                    name: I18n.t('history.执行人.colHead'),
                    id: 'operator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
            ];
            this.shortcuts = [
                {
                    text: I18n.t('history.近1小时'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 3600000);
                        return [
                            start, end,
                        ];
                    },
                },
                {
                    text: I18n.t('history.近12小时'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 43200000);
                        return [
                            start, end,
                        ];
                    },
                },
                {
                    text: I18n.t('history.近1天'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 86400000);
                        return [
                            start, end,
                        ];
                    },
                },
                {
                    text: I18n.t('history.近7天'),
                    value () {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 604800000);
                        return [
                            start, end,
                        ];
                    },
                },
            ];
            this.tableColumn = [
                {
                    id: 'id',
                    label: I18n.t('history.任务 ID'),
                    disabled: true,
                },
                {
                    id: 'name',
                    label: I18n.t('history.任务名称.colHead'),
                    disabled: true,
                },
                {
                    id: 'startupModeDesc',
                    label: I18n.t('history.执行方式.colHead'),
                    disabled: true,
                },
                {
                    id: 'typeDesc',
                    label: I18n.t('history.任务类型.colHead'),
                },
                {
                    id: 'statusDesc',
                    label: I18n.t('history.任务状态.colHead'),
                    disabled: true,
                },
                {
                    id: 'operator',
                    label: I18n.t('history.执行人.colHead'),
                },
                {
                    id: 'createTime',
                    label: I18n.t('history.开始时间.colHead'),
                },
                {
                    id: 'totalTimeText',
                    label: I18n.t('history.耗时时长'),
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'id' },
                    { id: 'name' },
                    { id: 'startupModeDesc' },
                    { id: 'typeDesc' },
                    { id: 'statusDesc' },
                    { id: 'operator' },
                    { id: 'createTime' },
                    { id: 'totalTimeText' },
                ]);
            }
        },
        methods: {
            /**
             * @desc 获取列表数据
             */
            fetchData () {
                this.$refs.list.$emit('onFetch', this.searchParams);
            },
            /**
             * @desc 重做任务
             */
            redoTask (taskInstanceId) {
                TaskExecuteService.redoTask({
                    taskInstanceId,
                    taskVariables: [],
                }).then(({ taskInstanceId }) => {
                    this.$bkMessage({
                        theme: 'success',
                        message: I18n.t('history.执行成功'),
                    });
                    this.$router.push({
                        name: 'historyTask',
                        params: {
                            id: taskInstanceId,
                        },
                    });
                });
            },
            /**
             * @desc 列表默认的执行时间筛选值
             */
            parseDefaultDateTime () {
                const defaultDateTime = [
                    '', '',
                ];
                const searchParams = {
                    startTime: '',
                    endTime: '',
                };
                
                const currentTime = new Date().getTime();
                
                if (Object.prototype.hasOwnProperty.call(this.$route.query, 'startTime')) {
                    defaultDateTime[0] = this.$route.query.startTime;
                } else {
                    defaultDateTime[0] = prettyDateTimeFormat(currentTime - 86400000);
                }
                
                searchParams.startTime = defaultDateTime[0]; // eslint-disable-line prefer-destructuring

                if (Object.prototype.hasOwnProperty.call(this.$route.query, 'endTime')) {
                    defaultDateTime[1] = this.$route.query.endTime;
                    searchParams.endTime = this.$route.query.endTime;
                } else {
                    defaultDateTime[1] = prettyDateTimeFormat(currentTime);
                    searchParams.endTime = '';
                }
                this.defaultDateTime = defaultDateTime;
                this.searchParams = searchParams;
                if (!searchParams.endTime) {
                    setTimeout(() => {
                        this.setToNowText(this.defaultDateTime);
                    });
                }
            },
            /**
             * @desc 自定义表格显示
             */
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields);
                this.tableSize = size;
                listColumnsCache.setItem(TABLE_COLUMN_CACHE, {
                    columns: fields,
                    size,
                });
            },
            /**
             * @desc 自定义表格显示
             * @param {Object} params 筛选值
             */
            handleSearch (params) {
                const { startTime, endTime } = this.searchParams;
                this.searchParams = {
                    ...params,
                    startTime,
                    endTime,
                };
                this.fetchData();
            },
            /**
             * @desc 筛选时间
             * @param {Array} date 时间值
             * @param {String} type 选择类型
             */
            handleDateChange (date, type) {
                if (type === 'upToNow') {
                    this.setToNowText(date);
                }
                this.searchParams.startTime = date[0];// eslint-disable-line prefer-destructuring
                this.searchParams.endTime = type === 'upToNow' ? '' : date[1];
                this.fetchData();
            },
            /**
             * @desc 日期值显示为至今
             * @param {Array} date 日期值
             */
            setToNowText (date) {
                this.$refs.datePicker.shortcut = {
                    text: `${date[0]} ${I18n.t('history.至今')}`,
                };
            },
            /**
             * @desc 调整执行详情页面
             * @param {Object} taskInstance 任务详情
             *
             * 如果作业类型的跳转到作业执行详情，如果不是则跳到步骤执行详情
             */
            handleGoDetail (taskInstance) {
                if (taskInstance.isTask) {
                    this.$router.push({
                        name: 'historyTask',
                        params: {
                            id: taskInstance.id,
                        },
                    });
                    return;
                }
                this.$router.push({
                    name: 'historyStep',
                    params: {
                        taskInstanceId: taskInstance.id,
                    },
                    query: {
                        from: 'historyList',
                    },
                });
            },
            /**
             * @desc 重做执行任务
             * @param {Object} taskInstance 任务详情
             *
             * 1，作业执行
             *  —— 有变量需要先去变量设置页面设置变量值
             *  —— 没有变量直接重做
             * 2，快速执行脚本
             *  —— 跳转到快速执行脚本页面
             * 3，快速分发文件
             *  —— 跳转到快速分发文件页面
             */
            handleGoRetry (taskInstance) {
                // 作业执行
                if (taskInstance.isTask) {
                    // 当重做接口比较慢时页面可能存在多个重做请求，避免重复操作需要禁用正在重做的任务操作
                    this.redoRequestMap = {
                        ...this.redoRequestMap,
                        [taskInstance.id]: true,
                    };

                    // 历史作业任务详情
                    TaskExecuteService.fetchTaskInstance({
                        id: taskInstance.id,
                    }).then(({ variables }) => {
                        // 有变量，去设置变量
                        if (variables.length > 0) {
                            this.redoRequestMap[taskInstance.id] = false;
                            this.$router.push({
                                name: 'redoTask',
                                params: {
                                    taskInstanceId: taskInstance.id,
                                },
                            });
                            return;
                        }
                        // 没有变量直接执行
                        this.$bkInfo({
                            title: I18n.t('history.确认执行？'),
                            subTitle: I18n.t('history.该方案未设置全局变量，点击确认将直接执行。'),
                            confirmFn: () => {
                                TaskExecuteService.redoTask({
                                    taskInstanceId: taskInstance.id,
                                    taskVariables: [],
                                }).then(({ taskInstanceId }) => {
                                    this.$bkMessage({
                                        theme: 'success',
                                        message: I18n.t('history.执行成功'),
                                    });
                                    this.$router.push({
                                        name: 'historyTask',
                                        params: {
                                            id: taskInstanceId,
                                        },
                                    });
                                })
                                    .finally(() => {
                                        this.redoRequestMap[taskInstance.id] = false;
                                    });
                            },
                        });
                    })
                        .catch(() => {
                            this.redoRequestMap[taskInstance.id] = false;
                        });
                }
                // 快速执行脚本
                // 去快速执行脚本页面重做
                if (taskInstance.isScript) {
                    this.$router.push({
                        name: 'fastExecuteScript',
                        params: {
                            taskInstanceId: taskInstance.id,
                        },
                        query: {
                            from: 'executiveHistory',
                        },
                    });
                    return;
                }
                // 快速分发文件
                // 去快速执行分发文件页面重做
                if (taskInstance.isFile) {
                    this.$router.push({
                        name: 'fastPushFile',
                        params: {
                            taskInstanceId: taskInstance.id,
                        },
                        query: {
                            from: 'executiveHistory',
                        },
                    });
                }
            },
        },
    };
</script>
<style lang="postcss">
    .executive-history-page {
        .task-redo-loading {
            position: relative;
            font-size: 14px;
            color: #3a84ff;

            &::after {
                z-index: -1;
                text-align: center;
                word-break: keep-all;
                content: attr(data-text);
                opacity: 0;
            }

            .rotate-loading {
                position: absolute;
                top: 2px;
                left: 50%;
                margin-left: -9px;
            }
        }
    }
</style>
