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
    <div class="cron-job-manage">
        <list-action-layout>
            <auth-button
                ref="create"
                theme="primary"
                auth="cron/create"
                @click="handleCreate"
                class="w120">
                {{ $t('cron.新建') }}
            </auth-button>
            <template #right>
                <jb-search-select
                    ref="search"
                    @on-change="handleSearch"
                    :data="searchSelect"
                    :placeholder="$t('cron.搜索任务ID，任务名称，更新人...')"
                    style="width: 480px;" />
            </template>
        </list-action-layout>
        <render-list
            ref="list"
            :data-source="getCronJobList"
            :size="tableSize"
            :search-control="() => $refs.search">
            <bk-table-column
                v-if="allRenderColumnMap.id"
                label="ID"
                prop="id"
                key="id"
                width="100"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.name"
                :label="$t('cron.任务名称.colHead')"
                sortable
                prop="name"
                key="name"
                align="left"
                show-overflow-tooltip>
                <template slot-scope="{ row }">
                    <auth-component :permission="row.canManage" :resource-id="row.id" auth="cron/view">
                        <span class="time-task-name" @click="handleViewDetail(row)">
                            {{ row.name }}
                        </span>
                        <span slot="forbid">{{ row.name }}</span>
                    </auth-component>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.planName"
                :label="$t('cron.执行方案名称')"
                key="planName"
                align="left"
                show-overflow-tooltip>
                <template slot-scope="{ row }">
                    <div v-if="row.isPlanLoading" class="sync-fetch">
                        <div class="sync-fetch-loading">
                            <Icon type="sync-pending" svg style="color: #3a84ff;" />
                        </div>
                    </div>
                    <router-link
                        v-else
                        class="task-plan-text"
                        :to="{
                            name: 'viewPlan',
                            params: {
                                templateId: row.taskTemplateId,
                            },
                            query: {
                                from: 'cronJob',
                                viewPlanId: row.taskPlanId,
                            },
                        }">
                        {{ row.taskPlanName }}
                    </router-link>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.policeText"
                :label="$t('cron.执行策略.colHead')"
                prop="policeText"
                key="policeText"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.creator"
                :label="$t('cron.创建人')"
                prop="creator"
                key="creator"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.createTime"
                :label="$t('cron.创建时间')"
                prop="createTime"
                key="createTime"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyUser"
                :label="$t('cron.更新人.colHead')"
                sortable
                prop="lastModifyUser"
                key="lastModifyUser"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyTime"
                :label="$t('cron.更新时间')"
                prop="lastModifyTime"
                key="lastModifyTime"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastExecuteStatus"
                :label="$t('cron.最新执行结果')"
                sortable
                prop="lastExecuteStatus"
                key="lastExecuteStatus"
                align="left">
                <template slot-scope="{ row }">
                    <Icon svg :type="row.statusIconType" style="font-size: 16px; vertical-align: middle;" />
                    <span style="vertical-align: middle;">{{ row.statusText }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.successRateText"
                :label="$t('cron.周期成功率')"
                :render-header="renderHeader"
                key="successRateText"
                align="left">
                <template slot-scope="{ row }">
                    <div v-if="row.isStatictisLoading" class="sync-fetch">
                        <div class="sync-fetch-loading">
                            <Icon type="sync-pending" svg style="color: #3a84ff;" />
                        </div>
                    </div>
                    <template v-else>
                        <template v-if="row.isRateEmpty">
                            <p v-html="row.successRateText" />
                        </template>
                        <template v-else>
                            <bk-popover placement="right" theme="light">
                                <p style="padding-right: 10px;" v-html="row.successRateText" />
                                <div slot="content" style="color: #63656e;">
                                    <div v-html="row.successRateTips" />
                                    <div v-if="row.showMoreFailAcion" class="more-fail-action">
                                        <bk-button
                                            text
                                            @click="handleHistoryRecord(row, true)">
                                            {{ $t('cron.更多失败记录') }}
                                        </bk-button>
                                    </div>
                                </div>
                            </bk-popover>
                        </template>
                    </template>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('cron.操作')"
                :resizable="false"
                width="200"
                key="action"
                align="left">
                <template slot-scope="{ row }">
                    <bk-switcher
                        :value="row.enable"
                        size="small"
                        theme="primary"
                        class="mr10"
                        @change="value => handleStatusChange(value, row)" />
                    <auth-button
                        auth="cron/edit"
                        :resource-id="row.id"
                        :permission="row.canManage"
                        class="time-task-edit mr10"
                        text
                        @click="handleEdit(row)">
                        {{ $t('cron.编辑') }}
                    </auth-button>
                    <jb-popover-confirm
                        :title="$t('cron.确定删除该定时任务？')"
                        :content="$t('cron.删除后不可恢复，请谨慎操作！')"
                        :confirm-handler="instance => handleDelete(row)">
                        <auth-button
                            auth="cron/delete"
                            :resource-id="row.id"
                            :permission="row.canManage"
                            text>
                            {{ $t('cron.删除') }}
                        </auth-button>
                    </jb-popover-confirm>
                    <bk-button text @click="handleHistoryRecord(row)">{{ $t('cron.执行记录') }}</bk-button>
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
        <jb-sideslider :is-show.sync="showOperation" v-bind="operationSidesliderInfo" :width="960">
            <task-operation
                v-if="showOperation"
                :id="editTaskId"
                :data="cronJobDetailInfo"
                @on-change="handleCronChange" />
        </jb-sideslider>
        <jb-sideslider :is-show.sync="showDetail" :title="$t('cron.定时任务详情')" :width="960">
            <task-detail :data="cronJobDetailInfo" />
            <template #footer>
                <bk-button theme="primary" @click="handleTriggerEdit">{{ $t('cron.编辑') }}</bk-button>
            </template>
        </jb-sideslider>
        <jb-sideslider
            :is-show.sync="showHistoryRecord"
            :show-footer="false"
            quick-close
            transfer
            :width="960">
            <div slot="header">
                <span>{{ $t('cron.定时执行记录') }}</span>
                <span style="font-size: 12px; color: #313238;">（{{ cronJobDetailInfo.name }}）</span>
            </div>
            <history-record
                v-if="showHistoryRecord"
                :show-faild="showHistoryFailedRecord"
                :data="cronJobDetailInfo"
                @on-change="handleCronChange" />
        </jb-sideslider>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TimeTaskService from '@service/time-task';
    import NotifyService from '@service/notify';
    import {
        listColumnsCache,
    } from '@utils/cache-helper';
    import ListActionLayout from '@components/list-action-layout';
    import RenderList from '@components/render-list';
    import JbSearchSelect from '@components/jb-search-select';
    import JbSideslider from '@components/jb-sideslider';
    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import TaskOperation from './components/operation';
    import TaskDetail from './components/detail';
    import HistoryRecord from './components/history-record';

    const TABLE_COLUMN_CACHE = 'cron_list_columns';

    export default {
        name: '',
        components: {
            ListActionLayout,
            RenderList,
            JbSearchSelect,
            JbSideslider,
            JbPopoverConfirm,
            TaskOperation,
            TaskDetail,
            HistoryRecord,
        },
        data () {
            return {
                showOperation: false,
                showDetail: false,
                showHistoryRecord: false,
                showHistoryFailedRecord: false,
                searchParams: [],
                cronData: {},
                loading: false,
                editTaskId: '',
                cronJobDetailInfo: {},
                currentOperate: 'create',
                historyRecordDialogTitle: '',
                selectedTableColumn: [],
                tableSize: 'small',
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.$refs.list.isLoading;
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
            operationSidesliderInfo () {
                if (this.cronJobDetailInfo.id) {
                    return {
                        title: I18n.t('cron.编辑定时任务'),
                        okText: I18n.t('cron.保存'),
                    };
                }
                return {
                    title: I18n.t('cron.新建定时任务'),
                    okText: I18n.t('cron.提交'),
                };
            },
        },
        watch: {
            '$route' () {
                this.initParseURL();
            },
        },
        created () {
            this.getCronJobList = TimeTaskService.timeTaskList;
            this.searchSelect = [
                {
                    name: 'ID',
                    id: 'cronJobId',
                    description: I18n.t('cron.将覆盖其它条件'),
                    validate (values, item) {
                        const validate = (values || []).every(_ => /^(\d*)$/.test(_.name));
                        return !validate ? I18n.t('cron.ID只支持数字') : true;
                    },
                },
                {
                    name: I18n.t('cron.任务名称.colHead'),
                    id: 'name',
                    default: true,
                },
                {
                    name: I18n.t('cron.执行方案ID'),
                    id: 'planId',
                    default: true,
                },
                {
                    name: I18n.t('cron.创建人'),
                    id: 'creator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
                {
                    name: I18n.t('cron.更新人.colHead'),
                    id: 'lastModifyUser',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
            ];
            this.tableColumn = [
                {
                    id: 'id',
                    label: 'ID',
                },
                {
                    id: 'name',
                    label: I18n.t('cron.任务名称.colHead'),
                    disabled: true,
                },
                {
                    id: 'planName',
                    label: I18n.t('cron.执行方案名称'),
                },
                {
                    id: 'policeText',
                    label: I18n.t('cron.执行策略.colHead'),
                    disabled: true,
                },
                {
                    id: 'creator',
                    label: I18n.t('cron.创建人'),
                },
                {
                    id: 'createTime',
                    label: I18n.t('cron.创建时间'),
                },
                {
                    id: 'lastModifyUser',
                    label: I18n.t('cron.更新人.colHead'),
                },
                {
                    id: 'lastModifyTime',
                    label: I18n.t('cron.更新时间'),
                },
                {
                    id: 'lastExecuteStatus',
                    label: I18n.t('cron.最新执行结果'),
                    disabled: true,
                },
                {
                    id: 'successRateText',
                    label: I18n.t('cron.周期成功率'),
                    disabled: true,
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'name' },
                    { id: 'planName' },
                    { id: 'policeText' },
                    { id: 'lastModifyUser' },
                    { id: 'lastModifyTime' },
                    { id: 'lastExecuteStatus' },
                    { id: 'successRateText' },
                ]);
            }
        },
        mounted () {
            this.initParseURL();
        },
        methods: {
            fetchData () {
                this.$refs.list.$emit('onFetch', this.searchParams);
            },
            initParseURL () {
                // 在列表通过url指定查看定时任务详情
                const { name, cronJobId, mode } = this.$route.query;
                if (mode === 'create') {
                    this.handleCreate();
                    return;
                }
            
                if (!name && !cronJobId) {
                    return;
                }
            
                const unWatch = this.$watch(() => this.$refs.list.isLoading, (isLoading) => {
                    if (!isLoading) {
                        if (mode === 'detail') {
                            setTimeout(() => {
                                // 通过url默认打开定时任务详情
                                const $firstTimeTaskName = this.$refs.list.$el.querySelector('.time-task-name');
                                if ($firstTimeTaskName) {
                                    $firstTimeTaskName.click();
                                }
                            });
                        } else if (mode === 'edit') {
                            setTimeout(() => {
                                // // 通过url默认打开定时任务编辑
                                const $firstTimeTask = this.$refs.list.$el.querySelector('.time-task-edit');
                                if ($firstTimeTask) {
                                    $firstTimeTask.click();
                                }
                            });
                        }
                    
                        unWatch();
                    }
                });
            },
            renderHeader (h, data) {
                return (
                <span>
                    <span>{ data.column.label }</span>
                    <bk-popover>
                        <icon
                            type="circle-italics-info"
                            style="margin-left: 8px; font-size: 12px;" />
                        <div slot="content">
                            <div style="font-weight: bold">{ I18n.t('cron.「周期成功率」采样规则和计算公式') }</div>
                            <div style="margin-top: 8px; font-weight: bold">{ I18n.t('cron.采样规则：') }</div>
                            <div>{ I18n.t('cron.近 24小时执行次数 ＞10，则 “分母” 为近 24 小时执行总数') }</div>
                            <div>{ I18n.t('cron.近 24小时执行次数 ≤ 10，则 “分母” 为近 10 次执行任务') }</div>
                            <div style="margin-top: 6px; font-weight: bold">{ I18n.t('cron.计算公式：') }</div>
                            <div>{ I18n.t('cron.成功次数(分子) / 分母 * 100 = 周期成功率（%）') }</div>
                        </div>
                    </bk-popover>
                </span>
                );
            },
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields);
                this.tableSize = size;
                listColumnsCache.setItem(TABLE_COLUMN_CACHE, {
                    columns: fields,
                    size,
                });
            },
            handleSearch (payload) {
                this.searchParams = payload;
                this.fetchData();
            },
            handleHistoryRecord (payload, showFailed = false) {
                this.cronJobDetailInfo = payload;
                this.showHistoryFailedRecord = showFailed;
                this.historyRecordDialogTitle = `定时执行记录${payload.name}`;
                this.showHistoryRecord = true;
            },
            handleViewDetail (payload) {
                this.cronJobDetailInfo = payload;
                this.showDetail = true;
            },
            handleCreate () {
                this.cronJobDetailInfo = {};
                this.showOperation = true;
            },
            handleEdit (payload) {
                this.cronJobDetailInfo = payload;
                this.showOperation = true;
            },
            handleTriggerEdit () {
                this.showDetail = false;
                this.showOperation = true;
            },
            handleCronChange (payload) {
                this.fetchData();
            },
            handleStatusChange (value, payload) {
                const enableMemo = payload.enable;
                payload.enable = value;
                TimeTaskService.timeTaskStatusUpdate({
                    id: payload.id,
                    enable: value,
                }).then(() => {
                    this.messageSuccess(value ? I18n.t('cron.开启成功') : I18n.t('cron.关闭成功'));
                })
                    .catch(() => {
                        payload.enable = enableMemo;
                    });
            },
            handleDelete (payload) {
                return TimeTaskService.timeTaskDelete({
                    id: payload.id,
                }).then(() => {
                    this.messageSuccess(I18n.t('cron.删除定时任务成功'));
                    this.fetchData();
                    return true;
                });
            },
        },
    };
</script>
<style lang="postcss">
    @keyframes sync-fetch-loading {
        0% {
            transform: rotateZ(0);
        }

        100% {
            transform: rotateZ(360deg);
        }
    }

    .cron-job-manage {
        .expression {
            font-size: 14px;
            color: #c4c6cc;
        }

        .more-fail-action {
            text-align: right;

            .bk-button-text {
                font-size: 12px;
            }
        }

        .time-task-name {
            display: inline-block;
            height: 18px;
            max-width: 100%;
            overflow: hidden;
            color: #3a84ff;
            text-overflow: ellipsis;
            white-space: nowrap;
            vertical-align: bottom;
            cursor: pointer;
        }

        .execute-result-text {
            &.success {
                &::before {
                    background: #2dcb56;
                }
            }

            &.fail {
                &::before {
                    background: #ea3636;
                }
            }

            &.waiting {
                &::before {
                    background: #dcdee5;
                }
            }

            &::before {
                display: inline-block;
                width: 8px;
                height: 8px;
                margin-right: 10px;
                border-radius: 50%;
                content: '';
            }
        }

        .sync-fetch {
            height: 13px;
        }

        .sync-fetch-loading {
            position: absolute;
            display: flex;
            width: 13px;
            height: 13px;
            animation: sync-fetch-loading 1s linear infinite;
        }

        .task-plan-text {
            display: inline-block;
            height: 18px;
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            vertical-align: bottom;
        }
    }

</style>
