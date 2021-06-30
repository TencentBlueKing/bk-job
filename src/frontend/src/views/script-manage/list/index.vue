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
    <div>
        <list-action-layout>
            <auth-button
                theme="primary"
                auth="script/create"
                @click="handleCreate"
                class="w120">
                {{ $t('script.新建') }}
            </auth-button>
            <template #right>
                <jb-search-select
                    ref="search"
                    @on-change="handleSearch"
                    :data="searchSelect"
                    :placeholder="$t('script.搜索脚本名称，类型，场景标签，更新人...')"
                    style="width: 420px;" />
            </template>
        </list-action-layout>
        <render-list
            ref="list"
            :data-source="getScriptList"
            :size="tableSize"
            :search-control="() => $refs.search">
            <bk-table-column
                v-if="allRenderColumnMap.id"
                label="ID"
                prop="id"
                key="id"
                width="300"
                align="left" />
            <bk-table-column
                :label="$t('script.脚本名称.colHead')"
                prop="name"
                key="name"
                :min-width="300"
                sortable
                align="left">
                <template slot-scope="{ row }">
                    <auth-component
                        :permission="row.canView"
                        auth="script/view"
                        :resource-id="row.id">
                        <jb-edit-input
                            :key="row.id"
                            field="scriptName"
                            :rules="rules.name"
                            :value="row.name"
                            :remote-hander="val => handleUpdateScript(row.id, val)"
                            v-slot="{ value }">
                            <span style="color: #3a84ff; cursor: pointer;" @click="handleVersion(row)">{{ value }}</span>
                        </jb-edit-input>
                        <span slot="forbid">{{ row.name }}</span>
                    </auth-component>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.type"
                :label="$t('script.脚本语言')"
                sortable
                prop="type"
                key="type"
                width="120"
                align="left">
                <template slot-scope="{ row }">
                    <span>{{ row.typeName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('script.场景标签.colHead')"
                sortable
                prop="tags"
                key="tags"
                width="200"
                align="left"
                class-name="edit-tag-column">
                <template slot-scope="{ row }">
                    <auth-component
                        :permission="row.canManage"
                        auth="script/edit"
                        :resource-id="row.id">
                        <jb-edit-tag
                            :key="row.id"
                            field="scriptTags"
                            :value="row.tags"
                            shortcurt
                            :remote-hander="val => handleUpdateScript(row.id, val)" />
                        <div slot="forbid">{{ row.tagsText }}</div>
                    </auth-component>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.related"
                :label="$t('script.被引用.colHead')"
                prop="related"
                key="related"
                width="100"
                :render-header="renderHeader"
                align="left">
                <template slot-scope="{ row }">
                    <a :tippy-tips="$t('script.作业模版引用')" @click="handleShowRelated('template', row)">
                        {{ row.relatedTaskTemplateNum }}
                    </a>
                    <span> / </span>
                    <a :tippy-tips="$t('script.执行方案引用')" @click="handleShowRelated('plan', row)">
                        {{ row.relatedTaskPlanNum }}
                    </a>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.version"
                :label="$t('script.线上版本')"
                prop="version"
                key="version"
                width="140"
                align="left">
                <template slot-scope="{ row }">
                    <span> {{ row.version || '--' }} </span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.creator"
                :label="$t('script.创建人.colHead')"
                sortable
                show-overflow-tooltip
                prop="creator"
                key="creator"
                width="140"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.createTime"
                :label="$t('script.创建时间')"
                prop="createTime"
                key="createTime"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyUser"
                :label="$t('script.更新人.colHead')"
                width="160"
                prop="lastModifyUser"
                key="lastModifyUser"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyTime"
                :label="$t('script.更新时间')"
                width="180"
                prop="lastModifyTime"
                key="lastModifyTime"
                align="left" />
            <bk-table-column
                :label="$t('script.操作')"
                :resizable="false"
                key="action"
                width="170"
                align="left">
                <template slot-scope="{ row }">
                    <auth-button
                        class="mr10"
                        text
                        :permission="row.canView"
                        auth="script/view"
                        :resource-id="row.id"
                        @click="handleVersion(row)">
                        {{ $t('script.版本管理') }}
                    </auth-button>
                    <span class="mr10" :tippy-tips="row.isExecuteDisable ? $t('script.该脚本没有 “线上版本” 可执行，请前往版本管理内设置。') : ''">
                        <auth-button
                            text
                            :permission="row.canView"
                            auth="script/execute"
                            :resource-id="row.id"
                            :disabled="row.isExecuteDisable"
                            @click="handleExec(row)">
                            {{ $t('script.去执行') }}
                        </auth-button>
                    </span>
                    <jb-popover-confirm
                        :title="$t('script.确定删除该脚本？')"
                        :content="$t('script.注意！脚本内的所有版本也将被清除')"
                        :confirm-handler="() => handleDelete(row)">
                        <auth-button
                            :permission="row.canManage"
                            auth="script/delete"
                            :resource-id="row.id"
                            text>
                            {{ $t('script.删除') }}
                        </auth-button>
                    </jb-popover-confirm>
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
            :is-show.sync="showRelated"
            :show-footer="false"
            quick-close
            v-bind="relatedScriptDialogInfo"
            :width="695">
            <related-script
                from="scriptList"
                :mode="showRelateMode"
                :info="relatedScriptInfo" />
        </jb-sideslider>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TagManageService from '@service/tag-manage';
    import PublicTagManageService from '@service/public-tag-manage';
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import NotifyService from '@service/notify';
    import {
        isPublicScript,
    } from '@utils/assist';
    import {
        scriptNameRule,
    } from '@utils/validator';
    import {
        listColumnsCache,
    } from '@utils/cache-helper';
    import ListActionLayout from '@components/list-action-layout';
    import RenderList from '@components/render-list';
    import JbSearchSelect from '@components/jb-search-select';
    import JbEditInput from '@components/jb-edit/input';
    import JbEditTag from '@components/jb-edit/tag';
    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import RelatedScript from '../common/related-script';

    const TABLE_COLUMN_CACHE = 'script_list_columns';

    export default {
        name: '',
        components: {
            ListActionLayout,
            RenderList,
            RelatedScript,
            JbSearchSelect,
            JbEditInput,
            JbEditTag,
            JbPopoverConfirm,
        },
        data () {
            return {
                showRelated: false,
                showRelateMode: '',
                relatedScriptInfo: {
                    id: 0,
                },
                searchParams: {},
                selectedTableColumn: [],
                tableSize: 'small',
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.$refs.list.isLoading;
            },
            relatedScriptDialogInfo () {
                const info = {
                    title: I18n.t('script.引用脚本的作业模版'),
                };
                if (this.showRelateMode === 'plan') {
                    info.title = I18n.t('script.引用脚本的执行方案');
                }
                return info;
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
        },
        created () {
            this.publicScript = isPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
            this.tagSericeHandler = this.publicScript ? PublicTagManageService : TagManageService;
            this.getScriptList = this.serviceHandler.scriptList;

            this.searchSelect = [
                {
                    name: 'ID',
                    id: 'scriptId',
                    description: I18n.t('script.将覆盖其它条件'),
                },
                {
                    name: I18n.t('script.脚本名称.colHead'),
                    id: 'name',
                    default: true,
                },
                {
                    name: I18n.t('script.脚本语言'),
                    id: 'type',
                    remoteMethod: PublicScriptService.scriptTypeList,
                    remoteExecuteImmediate: true,
                },
                {
                    name: I18n.t('script.场景标签.colHead'),
                    id: 'tags',
                    remoteMethod: this.tagSericeHandler.fetchTagOfSearch,
                    remoteExecuteImmediate: true,
                },
                {
                    name: I18n.t('script.创建人.colHead'),
                    id: 'creator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
                {
                    name: I18n.t('script.更新人.colHead'),
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
                    label: I18n.t('script.脚本名称.colHead'),
                    disabled: true,
                },
                {
                    id: 'tags',
                    label: I18n.t('script.场景标签.colHead'),
                    disabled: true,
                },
                {
                    id: 'type',
                    label: I18n.t('script.脚本语言'),
                },
                {
                    id: 'related',
                    label: I18n.t('script.被引用.colHead'),
                },
                {
                    id: 'version',
                    label: I18n.t('script.线上版本'),
                },
                {
                    id: 'creator',
                    label: I18n.t('script.创建人.colHead'),
                },
                {
                    id: 'createTime',
                    label: I18n.t('script.创建时间'),
                },
                {
                    id: 'lastModifyUser',
                    label: I18n.t('script.更新人.colHead'),
                },
                {
                    id: 'lastModifyTime',
                    label: I18n.t('script.更新时间'),
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'name' },
                    { id: 'tags' },
                    { id: 'type' },
                    { id: 'related' },
                    { id: 'version' },
                    { id: 'related' },
                    { id: 'version' },
                    { id: 'lastModifyUser' },
                    { id: 'lastModifyTime' },
                ]);
            }
            
            this.rules = {
                name: [
                    { required: true, message: I18n.t('script.脚本名称必填'), trigger: 'blur' },
                    { validator: scriptNameRule.validator, message: scriptNameRule.message, trigger: 'blur' },
                ],
            };
        },
        methods: {
            /**
             * @desc 获取列表数据
             */
            fetchData () {
                this.$refs.list.$emit('onFetch', {
                    ...this.searchParams,
                });
            },
            /**
             * @desc 通过脚本ID删除脚本
             * @param {Number} id 脚本id
             */
            removeScript (id) {
                return this.serviceHandler.scriptDelete({
                    id,
                }).then(() => {
                    this.fetchData();
                    this.messageSuccess(I18n.t('script.删除成功'));
                    return true;
                });
            },
            /**
             * @desc 列表搜索
             * @param {Object} params 搜索条件
             */
            handleSearch (params) {
                this.searchParams = params;
                this.fetchData();
            },
            /**
             * @desc 新建脚本
             */
            handleCreate () {
                if (this.publicScript) {
                    this.$router.push({
                        name: 'createPublicScript',
                    });
                    return;
                }
                this.$router.push({
                    name: 'createScript',
                });
            },
            /**
             * @desc 更新脚本字段
             * @param {String} id 脚本id
             * @param {Object} payload 字段名和值
             */
            handleUpdateScript (id, payload) {
                return this.serviceHandler.scriptUpdateMeta({
                    id,
                    ...payload,
                    updateField: Object.keys(payload)[0],
                });
            },
            /**
             * @desc 自定义列表配置
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
             * @desc 脚本版本列表
             * @param {Object} scriptData 脚本数据
             */
            handleVersion (scriptData) {
                if (this.publicScript) {
                    this.$router.push({
                        name: 'publicScriptVersion',
                        params: {
                            id: scriptData.id,
                        },
                    });
                    return;
                }
                this.$router.push({
                    name: 'scriptVersion',
                    params: {
                        id: scriptData.id,
                    },
                });
            },
            /**
             * @desc 执行脚本
             * @param {Object} scriptData 脚本数据
             */
            handleExec (scriptData) {
                this.serviceHandler.getOneOnlineScript({
                    id: scriptData.id,
                    publicScript: this.publicScript,
                }).then((script) => {
                    if (!script) {
                        this.$bkMessage({
                            limit: 1,
                            theme: 'error',
                            message: I18n.t('script.操作失败！请前往「版本管理」设置线上版后重试'),
                        });
                        return;
                    }
                    this.$router.push({
                        name: 'fastExecuteScript',
                        params: {
                            taskInstanceId: 0,
                            scriptVersionId: script.scriptVersionId,
                        },
                    });
                });
            },
            /**
             * @desc 删除脚本
             * @param {Object} scriptData 脚本数据
             */
            handleDelete (scriptData) {
                if (!scriptData.isEnableRemove) {
                    this.messageError(I18n.t('script.脚本正被作业引用中，无法删除'));
                    return false;
                }
                return this.serviceHandler.scriptDelete({
                    id: scriptData.id,
                }).then(() => {
                    this.fetchData();
                    this.messageSuccess(I18n.t('script.删除成功'));
                    return true;
                });
            },
            /**
             * @desc 脚本引用数据列表
             * @param {String} mode 引用的模版、执行方案
             * @param {Object} scriptData 脚本数据
             */
            handleShowRelated (mode, scriptData) {
                this.showRelated = true;
                this.showRelateMode = mode;
                this.relatedScriptInfo = scriptData;
            },
            /**
             * @desc 自定义表头
             */
            renderHeader (h, data) {
                /*  eslint-disable vue/script-indent  */
            return h('div', {
                directives: [
                    {
                        name: 'bkTooltips',
                        value: {
                            content: I18n.t('script.显示被执行方案引用的次数'),
                            placement: 'bottom',
                        },
                    },
                ],
            }, [
                data.column.label,
                h('Icon', {
                    props: {
                        type: 'italic-info',
                    },
                    style: {
                        marginLeft: '4px',
                    },
                }),
            ]);
        },
    },
};
</script>
