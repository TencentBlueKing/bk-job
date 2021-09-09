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
                class="w120"
                auth="tag/create"
                theme="primary"
                @click="handleCreate">
                {{ $t('tag.新建') }}
            </auth-button>
            <template #right>
                <jb-search-select
                    ref="search"
                    :data="searchSelect"
                    :placeholder="$t('tag.请输入')"
                    style="width: 420px;"
                    @on-change="handleSearch" />
            </template>
        </list-action-layout>
        <render-list
            ref="list"
            :data-source="dataSource"
            :size="tableSize"
            :search-control="() => $refs.search"
            @on-refresh="handleListChange">
            <bk-table-column
                v-if="allRenderColumnMap.id"
                label="ID"
                width="60"
                prop="id"
                key="id"
                align="left" />
            <bk-table-column
                :label="$t('tag.标签名.colHead')"
                prop="name"
                key="name"
                sortable
                min-width="200"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.description"
                :label="$t('tag.描述.colHead')"
                prop="descriptionText"
                key="descriptionText"
                min-width="200"
                show-overflow-tooltip
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.relatedTaskTemplateNum"
                :label="$t('tag.关联作业量.colHead')"
                prop="relatedTaskTemplateNum"
                key="relatedTaskTemplateNum"
                width="120"
                align="right">
                <template slot-scope="{ row }">
                    <router-link
                        target="_blank"
                        :to="{
                            name: 'taskList',
                            query: {
                                tags: row.id,
                            },
                        }">
                        {{ row.relatedTaskTemplateNum }}
                    </router-link>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.relatedScriptNum"
                :label="$t('tag.关联脚本量.colHead')"
                prop="relatedScriptNum"
                key="relatedScriptNum"
                width="120"
                align="right">
                <template slot-scope="{ row }">
                    <router-link
                        target="_blank"
                        :to="{
                            name: 'scriptList',
                            query: {
                                tags: row.id,
                            },
                        }">
                        {{ row.relatedScriptNum }}
                    </router-link>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.creator"
                :label="$t('tag.创建人')"
                prop="creator"
                key="creator"
                width="120"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.createTime"
                :label="$t('tag.创建时间')"
                prop="createTime"
                key="createTime"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyUser"
                :label="$t('tag.更新人.colHead')"
                prop="lastModifyUser"
                key="lastModifyUser"
                width="120"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyTime"
                :label="$t('tag.更新时间')"
                prop="lastModifyTime"
                key="lastModifyTime"
                width="180"
                align="left" />
            <bk-table-column
                :label="$t('tag.操作')"
                :resizable="false"
                key="action"
                fixed="right"
                width="200">
                <template slot-scope="{ row }">
                    <auth-button
                        class="mr10"
                        theme="primary"
                        :permission="row.canManage"
                        auth="tag/edit"
                        :resource-id="row.id"
                        text
                        @click="handleEdit(row)">
                        {{ $t('tag.编辑') }}
                    </auth-button>
                    <auth-button
                        class="mr10"
                        theme="primary"
                        :permission="row.canManage"
                        auth="tag/edit"
                        :resource-id="row.id"
                        text
                        @click="handleEditRelate(row)">
                        {{ $t('tag.批量流转关联项') }}
                    </auth-button>
                    <jb-popover-confirm
                        :title="$t('tag.确认删除该标签？')"
                        :content="$t('tag.关联的作业、脚本，将同时移除本标签')"
                        :confirm-handler="() => handleDelete(row.id)">
                        <auth-button
                            text
                            :permission="row.canManage"
                            auth="tag/delete"
                            :resource-id="row.id">
                            {{ $t('tag.删除') }}
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
        <lower-component level="custom" :custom="isShowOperation">
            <operation-tag
                v-model="isShowOperation"
                :data="editData"
                @on-change="handleOperationChange" />
        </lower-component>
        <jb-dialog
            v-model="isShowEditRelate"
            :width="480"
            title="批量流转关联项"
            header-position="left">
            <batch-edit-relate
                ref="batchEditRelate"
                :data="editData"
                @on-create="handleCreate" />
        </jb-dialog>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import NotifyService from '@service/notify';
    import TagManageService from '@service/tag-manage';
    import { listColumnsCache } from '@utils/cache-helper';
    import ListActionLayout from '@components/list-action-layout';
    import RenderList from '@components/render-list';
    import JbSearchSelect from '@components/jb-search-select';
    import OperationTag from '@components/operation-tag';
    import BatchEditRelate from './components/batch-edit-relate';

    const TABLE_COLUMN_CACHE = 'tag_manage_columns';

    export default {
        components: {
            ListActionLayout,
            RenderList,
            JbSearchSelect,
            OperationTag,
            BatchEditRelate,
        },
        data () {
            return {
                isShowOperation: false,
                isShowEditRelate: false,
                editData: {},
                tagList: [],
                searchParams: [],
                searchValue: [],
                selectedTableColumn: [],
                tableSize: 'small',
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.$refs.list.isLoading;
            },
            operationDialogInfo () {
                return {};
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
        },
        created () {
            this.dataSource = TagManageService.fetchTagList;
            this.searchSelect = [
                {
                    name: I18n.t('tag.标签名.colHead'),
                    id: 'name',
                    default: true,
                },
                {
                    name: I18n.t('tag.创建人'),
                    id: 'creator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
                {
                    name: I18n.t('tag.更新人.colHead'),
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
                    label: I18n.t('tag.标签名.colHead'),
                    disabled: true,
                },
                {
                    id: 'description',
                    label: I18n.t('tag.描述.colHead'),
                    disabled: true,
                },
                {
                    id: 'relatedTaskTemplateNum',
                    label: I18n.t('tag.关联作业量.colHead'),
                    disabled: true,
                },
                {
                    id: 'relatedScriptNum',
                    label: I18n.t('tag.关联脚本量.colHead'),
                    disabled: true,
                },
                {
                    id: 'creator',
                    label: I18n.t('tag.创建人'),
                },
                {
                    id: 'createTime',
                    label: I18n.t('tag.创建时间'),
                },
                {
                    id: 'lastModifyUser',
                    label: I18n.t('tag.更新人.colHead'),
                },
                {
                    id: 'lastModifyTime',
                    label: I18n.t('tag.更新时间'),
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'name' },
                    { id: 'description' },
                    { id: 'relatedTaskTemplateNum' },
                    { id: 'relatedScriptNum' },
                    { id: 'lastModifyUser' },
                    { id: 'lastModifyTime' },
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
             * @desc 列表数据刷新
             * @param { Object } data tag列表数据
             */
            handleListChange (data) {
                this.tagList = Object.freeze(data.data);
            },
            /**
             * @desc 表格自定时设置
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
             * @desc 表格自定时设置
             * @param {Object} params 搜索条件
             */
            handleSearch (params) {
                this.searchParams = params;
                this.fetchData();
            },
            /**
             * @desc 显示新建账号弹层
             */
            handleCreate () {
                this.editData = {};
                this.isShowOperation = true;
            },
            /**
             * @desc 标签有更新重新获取列表数据
             */
            handleOperationChange () {
                this.fetchData();
                if (this.$refs.batchEditRelate) {
                    this.$refs.batchEditRelate.refresh();
                }
            },
            /**
             * @desc 编辑 账号
             * @param {Object} data 某一行tag数据
             */
            handleEdit (data) {
                this.editData = Object.freeze(data);
                this.isShowOperation = true;
            },
            /**
             * @desc 操作的 tag 数据
             * @param { Object } tag
             */
            handleEditRelate (tag) {
                this.editData = tag;
                this.isShowEditRelate = true;
            },
            /**
             * @desc 删除
             * @param { Number } id tag id
             *
             *  删除成功后刷新列表数据
             */
            handleDelete (id) {
                return TagManageService.remove({
                    id,
                }).then(() => {
                    this.fetchData();
                    this.messageSuccess(I18n.t('tag.删除成功'));
                });
            },
        },
    };
</script>
