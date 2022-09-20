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
    <div class="detect-records-list-page">
        <list-action-layout>
            <jb-search-select
                ref="search"
                :data="searchSelect"
                :placeholder="$t('detectRecords.搜索拦截ID，表达式，业务，执行人，执行方式，调用方，动作…')"
                style="width: 480px;"
                @on-change="handleSearch" />
            <template #right>
                <bk-date-picker
                    ref="datePicker"
                    :clearable="false"
                    :placeholder="$t('detectRecords.选择日期')"
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
            v-test="{ type: 'list', value: 'detectRecord' }"
            :data-source="fetchDetectRecordsList"
            :search-control="() => $refs.search"
            :size="tableSize">
            <bk-table-column
                v-if="allRenderColumnMap.id"
                key="id"
                align="left"
                label="ID"
                prop="id"
                width="80">
                <template slot-scope="{ row }">
                    {{ row.id }}
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.ruleExpression"
                key="ruleExpression"
                align="left"
                :label="$t('detectRecords.表达式.colHead')"
                prop="ruleExpression"
                show-overflow-tooltip />
            <bk-table-column
                v-if="allRenderColumnMap.appId"
                key="scopeName"
                align="left"
                :label="$t('detectRecords.业务.colHead')"
                prop="scopeName"
                width="200">
                <template slot-scope="{ row }">
                    <span>[{{ row.scopeId }}] {{ row.scopeName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.operator"
                key="operator"
                align="left"
                :label="$t('detectRecords.执行人.colHead')"
                prop="operator"
                width="140" />
            <bk-table-column
                v-if="allRenderColumnMap.statusDesc"
                key="createTime"
                align="left"
                :label="$t('detectRecords.执行时间')"
                prop="createTime"
                width="200">
                <template slot-scope="{ row }">
                    {{ row.getCreatTimes }}
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.startupMode"
                key="startupMode"
                align="left"
                :label="$t('detectRecords.执行方式.colHead')"
                prop="startupMode"
                width="140">
                <template slot-scope="{ row }">
                    {{ row.getStartupModeHtml }}
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.client"
                key="client"
                align="left"
                :label="$t('detectRecords.调用方.colHead')"
                prop="client"
                width="150" />
            <bk-table-column
                v-if="allRenderColumnMap.action"
                key="mode"
                :label="$t('detectRecords.动作.colHead')"
                prop="mode"
                :render-header="renderPatternHeader"
                width="150">
                <template slot-scope="{ row }">
                    <span v-html="row.getActionHtml" />
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.scriptLanguage"
                key="scriptLanguage"
                align="left"
                :label="$t('detectRecords.脚本语言.colHead')"
                prop="scriptLanguage"
                width="150">
                <template slot-scope="{ row }">
                    {{ row.getSctiptTypeHtml }}
                </template>
            </bk-table-column>
            <bk-table-column
                key="action"
                align="left"
                fixed="right"
                :label="$t('detectRecords.操作')"
                width="100">
                <template slot-scope="{ row }">
                    <bk-button
                        v-test="{ type: 'button', value: 'viewDetectScript' }"
                        text
                        @click="handleShowScriptContent(row)">
                        {{ $t('detectRecords.查看脚本') }}
                    </bk-button>
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
        <jb-sideslider
            :is-show.sync="isShowScriptContent"
            :show-footer="false"
            :title="$t('detectRecords.脚本内容')"
            :width="900">
            <render-script-content :data="scriptData" />
        </jb-sideslider>
    </div>
</template>

<script>
    import AppManageService from '@service/app-manage';
    import DangerousRecordService from '@service/dangerous-record';
    import NotifyService from '@service/notify';

    import {
        prettyDateTimeFormat,
    } from '@utils/assist';
    import {
        listColumnsCache,
    } from '@utils/cache-helper';

    import JbSearchSelect from '@components/jb-search-select';
    import JbSideslider from '@components/jb-sideslider';
    import ListActionLayout from '@components/list-action-layout';
    import RenderList from '@components/render-list';

    import RenderScriptContent from './components/render-script-content';

    import I18n from '@/i18n';

    const TABLE_COLUMN_CACHE = 'detect_records_list_columns';

    export default {
        name: 'DetectRecordsList',
        components: {
            RenderList,
            JbSideslider,
            JbSearchSelect,
            ListActionLayout,
            RenderScriptContent,
        },
        data () {
            return {
                searchParams: {},
                defaultDateTime: [
                    '', '',
                ],
                selectedTableColumn: [],
                tableSize: 'small',
                scriptData: {},
                isShowScriptContent: false,
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
        },
        created () {
            this.parseDefaultDateTime();
            this.fetchDetectRecordsList = DangerousRecordService.recordList;
            this.searchSelect = [
                {
                    name: I18n.t('detectRecords.拦截ID'),
                    id: 'id',
                    default: true,
                },
                {
                    name: I18n.t('detectRecords.表达式.label'),
                    id: 'ruleExpression',
                },
                {
                    name: I18n.t('detectRecords.业务.label'),
                    id: 'appId',
                    remoteMethod: () => AppManageService.fetchWholeAppList().then(({ data }) => data),
                },
                {
                    name: I18n.t('detectRecords.执行人.label'),
                    id: 'operator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
                {
                    name: I18n.t('detectRecords.执行方式.label'),
                    id: 'startupMode',
                    children: [
                        {
                            name: I18n.t('detectRecords.页面执行'),
                            id: 1,
                        },
                        {
                            name: I18n.t('detectRecords.定时执行'),
                            id: 3,
                        },
                        {
                            name: I18n.t('detectRecords.API调用'),
                            id: 2,
                        },
                    ],
                },
                {
                    name: I18n.t('detectRecords.调用方.label'),
                    id: 'client',
                },
                {
                    name: I18n.t('detectRecords.动作.label'),
                    id: 'action',
                    children: [
                        {
                            name: I18n.t('detectRecords.扫描'),
                            id: 1,
                        },
                        {
                            name: I18n.t('detectRecords.拦截'),
                            id: 2,
                        },
                    ],
                },
            ];
            this.shortcuts = [
                {
                    text: I18n.t('detectRecords.近1小时'),
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
                    text: I18n.t('detectRecords.近12小时'),
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
                    text: I18n.t('detectRecords.近1天'),
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
                    text: I18n.t('detectRecords.近7天'),
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
                    label: 'ID',
                    disabled: true,
                },
                {
                    id: 'ruleExpression',
                    label: I18n.t('detectRecords.表达式.colHead'),
                },
                {
                    id: 'appId',
                    label: I18n.t('detectRecords.业务.colHead'),
                },
                {
                    id: 'operator',
                    label: I18n.t('detectRecords.执行人.colHead'),
                },
                {
                    id: 'statusDesc',
                    label: I18n.t('detectRecords.执行时间'),
                },
                {
                    id: 'startupMode',
                    label: I18n.t('detectRecords.执行方式.colHead'),
                },
                {
                    id: 'client',
                    label: I18n.t('detectRecords.调用方.colHead'),
                },
                {
                    id: 'action',
                    label: I18n.t('detectRecords.动作.colHead'),
                },
                {
                    id: 'scriptLanguage',
                    label: I18n.t('detectRecords.脚本语言.colHead'),
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'id' },
                    { id: 'ruleExpression' },
                    { id: 'appId' },
                    { id: 'operator' },
                    { id: 'statusDesc' },
                    { id: 'startupMode' },
                    { id: 'client' },
                    { id: 'action' },
                    { id: 'scriptLanguage' },
                ]);
            }
        },
        methods: {
            /**
             * @desc 获取列表数据
             */
            fetchData () {
                const searchParams = {
                    ...this.searchParams,
                };
                this.$refs.list.$emit('onFetch', searchParams);
            },

            /**
             * @desc 日期值显示为至今
             * @param {Array} date 日期值
             */
            setToNowText (date) {
                this.$refs.datePicker.shortcut = {
                    text: `${date[0]} ${I18n.t('detectRecords.至今')}`,
                };
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
             * @desc 自定义表头
             */
            renderPatternHeader (h, data) {
                const tips = [
                    {
                        title: this.$t('detectRecords.【扫描】'),
                        content: this.$t('detectRecords.命中规则的脚本执行任务仅会做记录，不会拦截'),
                    },
                    {
                        title: this.$t('detectRecords.【拦截】'),
                        content: this.$t('detectRecords.命中规则的脚本执行任务会被记录，并中止运行'),
                    },
                ];
                return (
                <div class="pattern-head">
                    <span>{data.column.label} </span>
                    <bk-popover placement="right-start" width="290">
                        <Icon type="info" style="color: #C4C6CC" />
                        <div slot="content">
                            <div class="detect-records-tips-content">
                                {
                                    tips.map(item => (
                                        <div class="item">
                                            <p>{ item.title }</p>
                                            <p>{ item.content }</p>
                                        </div>
                                    ))
                                }
                            </div>
                        </div>
                    </bk-popover>
                </div>
                );
            },
            /**
             * @desc 列表搜索
             * @param {Object} params 搜索条件
             */
            handleSearch (payload) {
                const { startTime, endTime } = this.searchParams;
                this.searchParams = {
                    ...payload,
                    startTime,
                    endTime,
                };
                this.fetchData();
            },

            /**
             * @desc 时间选择器改变时间并查询数据
             * @param {Array} date 时间
             */
            handleDateChange (date) {
                this.searchParams.startTime = date[0];// eslint-disable-line prefer-destructuring
                this.searchParams.endTime = date[1];// eslint-disable-line prefer-destructuring
                this.fetchData();
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
             * @desc 查看脚本内容
             * @param {Object} row 用户点击当前行的检测记录数据
             */

            handleShowScriptContent (row) {
                this.isShowScriptContent = true;
                this.scriptData = Object.freeze(row);
            },
            
        },
    };
</script>

<style lang="postcss">
    .detect-records-tips-content {
        font-size: 12px;

        .item {
            margin-bottom: 10px;
        }
    }
</style>
