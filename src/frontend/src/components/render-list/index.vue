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
    <div
        ref="renderList"
        v-bkloading="{ isLoading }"
        v-test="{ type: 'data', value: 'table' }"
        class="jb-render-list">
        <bk-table
            v-if="isRendered"
            v-bind="$attrs"
            :data="data"
            :default-sort="defaultSort"
            :max-height="tableMaxHeight"
            :pagination="realPagination"
            width="100%"
            v-on="$listeners"
            @page-change="handlePageChange"
            @page-limit-change="handleLimitChange"
            @sort-change="handleSort">
            <template slot="prepend">
                <div
                    v-if="selectNums > 0"
                    class="jb-table-select-tips">
                    <!-- eslint-disable-next-line max-len -->
                    <span>{{ $t('已选择.select') }}<span class="number strong">{{ selectNums }}</span>{{ $t('条.total') }}</span>
                    <span
                        class="action-clear"
                        @click="handleClearAllSelect">，{{ $t('清除所有勾选') }}</span>
                </div>
                <slot name="prepend" />
            </template>
            <bk-table-column
                v-if="selectable"
                key="listSelect"
                :render-header="renderHeader"
                width="70">
                <template slot-scope="{ row }">
                    <bk-checkbox
                        :checked="!!rowSelectMemo[row[primaryKey]]"
                        @change="value => handleRowSelect(row, value)" />
                </template>
            </bk-table-column>
            <slot />
            <div
                v-if="isRequesting"
                slot="empty"
                style="height: 200px;" />
            <Empty
                v-else-if="isSearching"
                slot="empty"
                type="search">
                <div>
                    <div style="font-size: 14px; color: #63656e;">
                        {{ $t('搜索结果为空') }}
                    </div>
                    <div style="margin-top: 8px; font-size: 12px; line-height: 16px; color: #979ba5;">
                        <span>{{ $t('可以尝试调整关键词') }}</span>
                        <template v-if="searchControl">
                            <span>{{ $t('或') }}</span>
                            <bk-button
                                text
                                @click="handleClearSearch">
                                {{ $t('清空搜索条件') }}
                            </bk-button>
                        </template>
                    </div>
                </div>
            </Empty>
        </bk-table>
    </div>
</template>
<script>
    import _ from 'lodash';

    import {
        buildURLParams,
        getOffset,
    } from '@utils/assist';
    import { routerCache } from '@utils/cache-helper';
    import EventBus from '@utils/event-bus';

    import Empty from '@components/empty';

    import I18n from '@/i18n';

    export default {
        components: {
            Empty,
        },
        inheritAttrs: false,
        props: {
            // 数据源
            dataSource: {
                type: Function,
                required: true,
            },
            // 指定列表页面的name
            name: String,
            // 是否解析 URL 的 query
            ignoreUrl: {
                type: Boolean,
                default: false,
            },
            // 是否可以选择行
            selectable: {
                type: Boolean,
                default: false,
            },
            // 使用小型分页选择器
            paginationSmall: {
                type: Boolean,
                default: false,
            },
            // data 数据的主键
            primaryKey: {
                type: String,
                default: 'id',
            },
            // 获取列表的联动搜索控件
            searchControl: {
                type: Function,
            },
        },
        data () {
            return {
                isRendered: false, // 组件是否已渲染
                isRequesting: false, // api 数据请求中
                isLoading: true, // api 数据加载中 (api 数据请求超过 300ms 才会显示 loading 效果)
                isSearching: false, // 是否有搜索条件
                data: [],
                tableMaxHeight: '',
                rowSelectMemo: Object.create(null),
                selectNums: 0,
                params: {},
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 10,
                },
            };
        },
        computed: {
            /**
             * @desc 列表当前页是否全选
             * @returns {Boolean}
             */
            isPageChecked () {
                if (Object.keys(this.rowSelectMemo).length < 1) {
                    return false;
                }
                let status = true;
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.data.length; i++) {
                    if (!this.rowSelectMemo[this.data[i][this.primaryKey]]) {
                        status = false;
                        break;
                    }
                }
                return status;
            },
            /**
             * @desc 是否全选
             * @returns {Boolean}
             */
            isWholeChecked () {
                return this.selectNums > 0 && this.selectNums >= this.pagination.count;
            },
            /**
             * @desc 列表分页的状态
             * @returns {Object}
             */
            realPagination () {
                return {
                    ...this.pagination,
                    small: this.paginationSmall,
                };
            },
        },
        watch: {
            rowSelectMemo () {
                this.selectNums = Object.keys(this.rowSelectMemo).length;
                this.data = [
                    ...this.data,
                ];
            },
        },
        created () {
            // 页面回退时返回列表选择（url标记listSelectIds）
            const { listSelectIds } = this.$route.query;
            if (listSelectIds) {
                listSelectIds.split(',').forEach((id) => {
                    this.rowSelectMemo[this.primaryKey] = true;
                });
                setTimeout(() => {
                    this.triggerSelectChange();
                });
            }
            // render list内部的筛选参数排序、过滤
            this.defaultSort = {};
            this.requestParamsMemo = {};
            if (!this.ignoreUrl) {
                this.parseURL();
            }
        },
        mounted () {
            this.init();
            this.$on('onFetch', (params) => {
                if (params) {
                    this.params = Object.freeze(params);
                    if (!this.waitingInit) {
                        this.pagination.current = 1;
                    }
                    this.waitingInit = false;
                }
                const pageSize = this.pagination.limit;
                const start = parseInt(this.pagination.current - 1, 10) * pageSize;
                
                const requestParams = {
                    ...this.defaultSortParams,
                    ...this.params,
                    pageSize,
                    start,
                };
                if (!this.ignoreUrl) {
                    // 缓存路由参数在回退时还原
                    routerCache.setItem(this.$route.name, requestParams);
                    window.history.replaceState({}, '', `?${buildURLParams(requestParams)}`);
                }
                
                // 跨页全选时同样需要过滤
                this.requestParamsMemo = requestParams;
                this.isRequesting = true;
                this.$request(this.dataSource(requestParams, {
                    permission: 'page',
                }), () => {
                    this.isLoading = true;
                }).then((data) => {
                    this.pagination = {
                        ...this.pagination,
                        count: data.total || 0,
                    };
                    this.$emit('on-refresh', data);
                    // 延后表格数据的更新，保证 on-refresh 事件逻辑优先执行
                    setTimeout(() => {
                        this.data = data.data;
                        this.isSearching = !this.checkSearchEmpty();
                    });

                    // 重要！！！（列表 api 返回数据说明）
                    // existAny 表示资源总数是否为 0
                    // total 表示本次筛选结果数
                    if (!this.ignoreUrl
                        && Object.prototype.hasOwnProperty.call(data, 'existAny')
                        && !data.existAny) {
                        EventBus.$emit('page-empty');
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                        this.isRequesting = false;
                    });
            });
        },
        methods: {
            /**
             * @desc 计算默认分页、表格高度
             */
            init () {
                const { top } = getOffset(this.$refs.renderList);
                const windowHeight = window.innerHeight;
                const tableHeadHeight = 42;
                const paginationHeight = 63;
                const windownOffsetBottom = 20;
                const listTotalHeight = windowHeight - top - tableHeadHeight - paginationHeight - windownOffsetBottom;
                const tableRowHeight = 42;
                const limit = Math.floor(listTotalHeight / tableRowHeight);
                const pageLimit = new Set([
                    10, 20, 50, 100, limit,
                ]);
                if (!pageLimit.has(this.pagination.limit)) {
                    pageLimit.add(this.pagination.limit);
                }
                this.pagination.limitList = [
                    ...pageLimit,
                ].sort((a, b) => a - b);
                if (!this.waitingInit) {
                    this.pagination.limit = limit;
                }
                this.tableMaxHeight = tableHeadHeight + tableRowHeight * limit + paginationHeight;
                this.isRendered = true;
            },
            /**
             * @desc 解析url
             */
            parseURL () {
                this.URLQuery = this.$route.query;
                const pageSize = ~~this.URLQuery.pageSize;
                const start = ~~this.URLQuery.start;
                this.waitingInit = false;
                // 解析url的分页信息
                if (pageSize) {
                    this.pagination.current = parseInt(start / pageSize, 10) + 1;
                    this.pagination.limit = pageSize;
                    this.waitingInit = true;
                }
                // 解析url的排序信息
                const { orderField, order } = this.URLQuery;
                
                if (orderField) {
                    // table默认排序设置
                    this.defaultSort.prop = orderField;
                    this.defaultSort.order = ~~order ? 'ascending' : 'descending';
                }
                this.defaultSortParams = {};
                
                if (Object.prototype.hasOwnProperty.call(this.URLQuery, 'orderField')
                    && Object.prototype.hasOwnProperty.call(this.URLQuery, 'order')) {
                    // api默认排序参数
                    this.defaultSortParams = {
                        orderField,
                        order,
                    };
                }
            },
            /**
             * @desc 外部调用，重置 table 的选择状态
             */
            resetSelect () {
                this.handleClearAllSelect();
            },
            /**
             * @desc 跨页全选
             */
            fetchWhole () {
                if (this.isWholeChecked) {
                    return;
                }
                this.$request(this.dataSource({
                    ...this.requestParamsMemo,
                    pageSize: -1,
                    start: -1,
                }), () => {
                    this.isLoading = true;
                }).then((data) => {
                    const rowSelectMemo = {};
                    data.data.forEach((item) => {
                        rowSelectMemo[item[this.primaryKey]] = item;
                    });
                    this.rowSelectMemo = Object.freeze(rowSelectMemo);
                    this.triggerSelectChange();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 自定义表头
             */
            renderHeader (h) {
                const renderCheckbox = () => {
                    if (this.isWholeChecked) {
                        return (
                        <div class="jb-whole-check" onClick={this.handleClearAllSelect} />
                        );
                    }
                    return (
                    <bk-checkbox checked={this.isPageChecked} nativeOnClick={this.handlePageSelectToggle} />
                    );
                };
                return (
                <div class="select-cell">
                    {renderCheckbox()}
                    <bk-popover
                        placement="bottom-start"
                        theme="light jb-table-select-menu"
                        arrow={ false }
                        size="regular">
                        <icon class="select-menu-flag" type="down-small" />
                        <div slot="content" class="jb-table-select-plan">
                            <div class="item" onClick={this.handlePageSelect}>{I18n.t('本页全选')}</div>
                            <div class="item" onClick={this.fetchWhole}>{I18n.t('跨页全选')}</div>
                        </div>
                    </bk-popover>
                </div>
                );
            },
            /**
             * @desc 触发行选中
             */
            triggerSelectChange () {
                const result = [];
                Object.keys(this.rowSelectMemo).forEach((idKey) => {
                    if (_.isObject(this.rowSelectMemo[idKey])) {
                        result.push(this.rowSelectMemo[idKey]);
                    } else {
                        // 如果是通过 url的查询参数listSelectIds还原选中状态
                        // 这个时候没法获取到每一条的详细数据，只能返回基本数据{ id: 1 }
                        result.push({
                            [this.primaryKey]: idKey,
                        });
                    }
                });
                this.$emit('on-selection-change', result);
            },
            /**
             * @desc 触发排序
             * @param {Object} payload 表格行的数据
             */
            handleSort (payload) {
                const sort = {
                    descending: 0,
                    ascending: 1,
                };
                if (payload.prop) {
                    this.params = Object.freeze({
                        ...this.params,
                        orderField: payload.prop,
                        order: sort[payload.order],
                    });
                } else {
                    const params = {
                        ...this.params,
                    };
                    delete params.orderField;
                    delete params.order;
                    this.params = Object.freeze(params);
                }
                
                this.$emit('onFetch');
            },
            /**
             * @desc 翻页
             * @param {Number} value 最新展示页
             */
            handlePageChange (value) {
                this.pagination.current = value;
                this.$emit('onFetch');
            },
            /**
             * @desc 每页条数
             * @param {Number} value 最新每页展示条数
             */
            handleLimitChange (value) {
                this.pagination.current = 1;
                this.pagination.limit = value;
                this.$emit('onFetch');
            },
            /**
             * @desc 列表全选切换
             */
            handlePageSelectToggle () {
                const rowSelectMemo = { ...this.rowSelectMemo };
                this.data.forEach((item) => {
                    if (this.isPageChecked) {
                        delete rowSelectMemo[item[this.primaryKey]];
                    } else {
                        rowSelectMemo[item[this.primaryKey]] = item;
                    }
                });
                this.rowSelectMemo = Object.freeze(rowSelectMemo);
                this.triggerSelectChange();
            },
            /**
             * @desc 列表单页全选切换
             */
            handlePageSelect () {
                if (this.isPageChecked) {
                    return;
                }
                const rowSelectMemo = { ...this.rowSelectMemo };
                this.data.forEach((item) => {
                    rowSelectMemo[item[this.primaryKey]] = item;
                });
                this.rowSelectMemo = Object.freeze(rowSelectMemo);

                this.triggerSelectChange();
            },
            /**
             * @desc 选择某一行
             * @param {Object} row 表格行数据
             * @param {Boolean} value 行的选中状态
             */
            handleRowSelect (row, value) {
                const rowSelectMemo = { ...this.rowSelectMemo };
                if (value) {
                    rowSelectMemo[row[this.primaryKey]] = row;
                } else {
                    delete rowSelectMemo[row[this.primaryKey]];
                }
                this.rowSelectMemo = Object.freeze(rowSelectMemo);
                this.triggerSelectChange();
            },
            /**
             * @desc 清除选中状态
             */
            handleClearAllSelect () {
                this.rowSelectMemo = Object.create(null);
                this.triggerSelectChange();
            },
            /**
             * @desc 检测搜索条件是否为空
             * @returns {Boolean}
             */
            checkSearchEmpty () {
                if (!this.searchControl || typeof this.searchControl !== 'function') {
                    return false;
                }
                const searchControl = this.searchControl();
                if (!searchControl || typeof searchControl.checkEmpty !== 'function') {
                    return false;
                }
                return searchControl.checkEmpty();
            },
            /**
             * @desc 清除列表筛选参数
             */
            handleClearSearch () {
                const searchControl = this.searchControl();
                if (typeof searchControl.reset === 'function') {
                    searchControl.reset();
                }
            },
        },
    };
</script>
<style lang="postcss">
    .jb-render-list {
        .bk-table-pagination-wrapper {
            background: #fff;
        }

        .bk-table-body-wrapper {
            td {
                .bk-table-setting-content {
                    display: none;
                }
            }
        }

        .select-cell {
            position: relative;
            display: flex;
            align-items: center;

            .jb-whole-check {
                position: relative;
                display: inline-block;
                width: 16px;
                height: 16px;
                vertical-align: middle;
                cursor: pointer;
                background-color: #fff;
                border: 1px solid #3a84ff;
                border-radius: 2px;

                &::after {
                    position: absolute;
                    top: 1px;
                    left: 4px;
                    width: 4px;
                    height: 8px;
                    border: 2px solid #3a84ff;
                    border-top: 0;
                    border-left: 0;
                    content: "";
                    transform: rotate(45deg);
                }
            }

            .select-menu-flag {
                margin-left: 4px;
                font-size: 18px;
            }
        }

        .jb-table-select-tips {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 30px;
            background: #ebecf0;

            .strong {
                color: inherit;
            }

            .action-clear {
                color: #3a84ff;
                cursor: pointer;
            }
        }

        .bk-table-empty-block {
            height: auto;
            background: #fff;
        }
    }

    .tippy-tooltip {
        &.jb-table-select-menu-theme {
            padding: 0;

            .jb-table-select-plan {
                padding: 5px 0;

                .item {
                    padding: 0 10px;
                    font-size: 12px;
                    line-height: 26px;
                    cursor: pointer;

                    &:hover {
                        color: #3a84ff;
                        background-color: #eaf3ff;
                    }

                    &.is-selected {
                        color: #3a84ff;
                        background-color: #f4f6fa;
                    }
                }
            }
        }
    }
</style>
