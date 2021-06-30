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
    <div class="ticket-manage-page" v-bkloading="{ isLoading }">
        <list-action-layout>
            <auth-button
                theme="primary"
                auth="ticket/create"
                @click="handleCreate"
                class="w120">
                {{ $t('ticket.新建') }}
            </auth-button>
            <template #right>
                <jb-search-select
                    ref="search"
                    @on-change="handleSearch"
                    :data="searchSelect"
                    :placeholder="$t('ticket.搜索 ID、名称、描述、创建人、更新人...')"
                    style="width: 480px;" />
            </template>
        </list-action-layout>
        <render-list
            ref="list"
            :data-source="getTicketList"
            :size="tableSize"
            :search-control="() => $refs.search">
            <bk-table-column
                v-if="allRenderColumnMap.id"
                :label="$t('ticket.凭证ID')"
                width="300"
                prop="id"
                key="id"
                align="left">
                <template slot-scope="{ row }">
                    <span>{{ row.id }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('ticket.凭证名称')"
                sortable
                prop="name"
                key="name"
                align="left"
                show-overflow-tooltip>
                <template slot-scope="{ row }">
                    <span class="ticket-name">{{ row.name }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('ticket.类型.colHead')"
                prop="type"
                key="type"
                align="left"
                :filters="sourceFilters"
                :filter-method="handelFilterType">
                <template slot-scope="{ row }">
                    <span>{{ typeMap[row.type] }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.description"
                :label="$t('ticket.描述')"
                prop="description"
                key="description"
                align="left">
                <template slot-scope="{ row }">
                    <span>{{ row.description }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.related"
                :label="$t('ticket.被引用.colHead')"
                prop="related"
                key="related"
                width="100"
                align="left">
                <template slot-scope="{ row }">
                    <div v-if="row.isRelatedLoading" class="sync-fetch-relate-nums">
                        <div class="related-nums-loading">
                            <Icon type="sync-pending" svg style="color: #3a84ff;" />
                        </div>
                    </div>
                    <bk-button
                        v-else
                        text
                        @click="handleShowRelated(row.id)">
                        {{ row.relatedNums }}
                    </bk-button>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.creator"
                :label="$t('ticket.创建人')"
                width="120"
                prop="creator"
                key="creator"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.createTime"
                :label="$t('ticket.创建时间')"
                prop="createTime"
                key="createTime"
                width="180"
                align="left" />
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyUser"
                :label="$t('ticket.更新人')"
                prop="lastModifyUser"
                key="lastModifyUser"
                align="left">
                <template slot-scope="{ row }">
                    <span>{{ row.lastModifyUser }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allRenderColumnMap.lastModifyTime"
                :label="$t('ticket.更新时间')"
                prop="lastModifyTime"
                key="lastModifyTime"
                align="left">
                <template slot-scope="{ row }">
                    <span style="display: block;">{{ row.lastModifyTime }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('ticket.操作')"
                :resizable="false"
                key="action"
                width="80"
                align="left">
                <template slot-scope="{ row }">
                    <auth-button
                        auth="ticket/edit"
                        :resource-id="row.id"
                        :permission="row.canManage"
                        text
                        class="mr10"
                        @click="handleEdit(row)">
                        {{ $t('ticket.编辑') }}
                    </auth-button>
                    <jb-popover-confirm
                        :title="$t('ticket.确定删除该凭证？')"
                        :content="$t('ticket.正在被文件源使用的凭证无法删除，如需删除请先解除引用关系。')"
                        :confirm-handler="() => handleDelete(row.id)">
                        <auth-button
                            auth="ticket/edit"
                            :permission="row.canManage"
                            :resource-id="row.id"
                            text>
                            {{ $t('ticket.删除') }}
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
            :is-show.sync="showRelatedSideslider"
            :show-footer="false"
            :width="900"
            :title="$t('ticket.被引用.label')">
            <related-ticket
                :credential-id="credentialId" />
        </jb-sideslider>
        <jb-sideslider
            :is-show.sync="showOpertionSideslider"
            :width="600"
            v-bind="operationSidesliderInfo">
            <ticket-opertion
                :data="ticketDetailInfo"
                @on-change="handleChange" />
        </jb-sideslider>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TicketService from '@service/ticket-manage';
    import NotifyService from '@service/notify';
    import {
        listColumnsCache,
    } from '@utils/cache-helper';
    import RenderList from '@components/render-list';
    import JbSearchSelect from '@components/jb-search-select';
    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import ListActionLayout from '@components/list-action-layout';
    import RelatedTicket from './components/related-ticket';
    import TicketOpertion from './components/opertion';

    const TABLE_COLUMN_CACHE = 'ticket_list_columns';

    export default {
        name: 'TicketList',
        components: {
            ListActionLayout,
            JbSearchSelect,
            RenderList,
            TicketOpertion,
            JbPopoverConfirm,
            RelatedTicket,
        },
        data () {
            return {
                isLoading: false,
                tableSize: 'small',
                selectedTableColumn: [],
                searchParams: {},
                showRelatedSideslider: false,
                showOpertionSideslider: false,
                ticketDetailInfo: {},
                credentialId: 0,
                relatedNum: [],
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
            operationSidesliderInfo () {
                if (!this.ticketDetailInfo.id) {
                    return {
                        title: I18n.t('ticket.新建凭证'),
                        okText: I18n.t('ticket.保存'),
                    };
                }
                return {
                    title: I18n.t('ticket.编辑凭证'),
                    okText: I18n.t('ticket.提交'),
                };
            },
        },
        created () {
            this.getTicketList = TicketService.fetchListWithRelate;
            this.sourceFilters = [
                { value: 'APP_ID_SECRET_KEY', text: I18n.t('AppID+SecretKey') },
                { value: 'PASSWORD', text: I18n.t('单一密码') },
                { value: 'USERNAME_PASSWORD', text: I18n.t('用户名+密码') },
                { value: 'SECRET_KEY', text: I18n.t('单一SecretKey') },
            ];
            this.searchSelect = [
                {
                    id: 'id',
                    name: 'ID',
                    description: '将覆盖其它条件',
                },
                {
                    id: 'name',
                    name: I18n.t('ticket.名称'),
                    default: true,
                },
                {
                    name: I18n.t('ticket.描述'),
                    id: 'description',
                },
                {
                    name: I18n.t('ticket.创建人'),
                    id: 'creator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
                {
                    name: I18n.t('ticket.更新人'),
                    id: 'lastModifyUser',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
            ];
            this.tableColumn = [
                {
                    id: 'id',
                    label: I18n.t('ticket.凭证ID'),
                },
                {
                    id: 'name',
                    label: I18n.t('ticket.名称'),
                    disabled: true,
                },
                {
                    id: 'type',
                    label: I18n.t('ticket.类型.colHead'),
                    disabled: true,
                },
                {
                    id: 'description',
                    label: I18n.t('ticket.描述'),
                },
                {
                    id: 'related',
                    label: I18n.t('ticket.被引用.colHead'),
                },
                {
                    id: 'creator',
                    label: I18n.t('ticket.创建人'),
                },
                {
                    id: 'createTime',
                    label: I18n.t('ticket.创建时间'),
                },
                {
                    id: 'lastModifyUser',
                    label: I18n.t('ticket.更新人'),
                },
                {
                    id: 'lastModifyTime',
                    label: I18n.t('ticket.更新时间'),
                },
            ];
            this.typeMap = {
                APP_ID_SECRET_KEY: I18n.t('AppID+SecretKey'),
                PASSWORD: I18n.t('单一密码'),
                USERNAME_PASSWORD: I18n.t('用户名+密码'),
                SECRET_KEY: I18n.t('单一SecretKey'),
            };

            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'id' },
                    { id: 'name' },
                    { id: 'type' },
                    { id: 'description' },
                    { id: 'related' },
                    { id: 'lastModifyUser' },
                    { id: 'lastModifyTime' },
                ]);
            }
        },
        methods: {

            /**
             * @desc 获取凭证数据列表
             */
            fetchData () {
                this.$refs.list.$emit('onFetch', {
                    ...this.searchParams,
                });
            },

            /**
             * @desc 新建凭证
             *
             * 显示新建凭证模板
             */
            handleCreate () {
                this.showOpertionSideslider = true;
                this.ticketDetailInfo = {};
            },

            /**
             * @desc 查看被引用模板
             * @param {Number} id 凭证id
             *
             * 显示被引用模板详情
             */
            handleShowRelated (id) {
                this.credentialId = id;
                this.showRelatedSideslider = true;
            },

            /**
             * @desc 过滤表格数据
             * @param {Object} searchParams 用户输入的过滤数据
             *
             * 重新拉取数据
             */
            handleSearch (searchParams) {
                this.searchParams = searchParams;
                this.fetchData();
            },

            /**
             * @desc 设置表格显示列/表格size
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
             * @desc 新建、编辑凭证成功
             *
             * 重新拉取列表数据
             */
            handleChange () {
                this.fetchData();
                this.relatedNum = [];
                this.fetchCitedNum();
            },

            handelFilterType (value, row, column) {
                const { property } = column;
                return row[property] === value;
            },

            /**
             * @desc 编辑凭证数据
             * @param {Object} payload 选中凭证的详细数据
             *
             * 显示编辑凭证数据模板
             */
            handleEdit (payload) {
                this.ticketDetailInfo = payload;
                this.showOpertionSideslider = true;
            },

            /**
             * @desc 删除凭证
             * @param {Number} id 凭证id
             *
             * 删除成功重新拉取列表数据
             */
            handleDelete (id) {
                TicketService.remove({
                    id,
                }).then(() => {
                    this.messageSuccess(I18n.t('ticket.删除成功'));
                    this.fetchData();
                    this.relatedNum = [];
                    this.fetchCitedNum();
                });
            },
        },
    };
</script>
<style lang="postcss">
    @keyframes related-nums-loading {
        0% {
            transform: rotateZ(0);
        }

        100% {
            transform: rotateZ(360deg);
        }
    }

    .ticket-manage-page {
        .sync-fetch-relate-nums {
            height: 13px;

            .related-nums-loading {
                position: absolute;
                display: flex;
                width: 13px;
                height: 13px;
                animation: related-nums-loading 1s linear infinite;
            }
        }
    }
</style>
