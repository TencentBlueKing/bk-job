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
    <div class="select-bucket">
        <list-action-layout>
            <jb-breadcrumb
                :key="`${fileSourceInfo.alias}_${path}`"
                :width="645"
                @on-last="handleBackLast">
                <jb-breadcrumb-item>
                    <Icon
                        style="font-size: 20px;"
                        type="folder-open" />
                    <span @click="handleGoSourceList">{{ $t('文件源列表') }}</span>
                </jb-breadcrumb-item>
                <jb-breadcrumb-item>
                    <span @click="handlePathLocation('')">{{ fileSourceInfo.alias }}</span>
                </jb-breadcrumb-item>
                <jb-breadcrumb-item
                    v-for="(item) in pathStack"
                    :key="item.path">
                    <span @click="handlePathLocation(item.path)">{{ item.name }}</span>
                </jb-breadcrumb-item>
            </jb-breadcrumb>
            <template #right>
                <jb-input
                    enter-trigger
                    :placeholder="$t('搜索关键字')"
                    right-icon="bk-icon icon-search"
                    style="width: 480px;"
                    @submit="handleSearch" />
            </template>
        </list-action-layout>
        <div v-bkloading="{ isLoading }">
            <bk-table
                :data="tableData"
                :pagination="pagination"
                @page-change="handlePageChange">
                <render-file-list-column
                    v-for="(column, index) in renderColumns"
                    :key="`${path}_${index}_${isLoading}_${wholeTableRowSelect}`"
                    :column="column"
                    :link-handler="handleLink"
                    :render-header="renderHeader"
                    :row-selection="rowSelectMemo"
                    :select-handler="handleRowSelect" />
            </bk-table>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import FileService from '@service/file';

    import ListActionLayout from '@components/list-action-layout';
    import RenderFileListColumn, {
        checkIsCheckboxColumn,
        parseCondition,
    } from '@components/render-file-list-column';

    export default {
        components: {
            ListActionLayout,
            RenderFileListColumn,
        },
        props: {
            fileSourceId: {
                type: Number,
                required: true,
            },
            fileLocation: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                isLoading: false,
                renderColumns: {},
                tableData: [],
                path: '',
                name: '',
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 10,
                    'limit-list': [10],
                    small: true,
                    'show-total-count': true,
                    'show-limit': false,
                },
                rowSelectMemo: {},
                fileSourceInfo: {},
            };
        },
        computed: {
            /**
             * @desc 面包屑路径
             * @return {Array}
             */
            pathStack () {
                return this.path.split('/').reduce((result, item) => {
                    if (item) {
                        const last = result.length > 0 ? result[result.length - 1].path : '';
                         
                        result.push({
                            path: `${last}${item}/`,
                            name: item,
                        });
                    }
                    return result;
                }, []);
            },
            /**
             * @desc 列表选择状态
             * @return {Object}
             */
            isPageCheckedInfo () {
                let checkNums = 0;
                let dirNums = 0;
                const listNums = this.tableData.length;
                if (Object.keys(this.rowSelectMemo).length > 0) {
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < listNums; i++) {
                        const currentRow = this.tableData[i];
                        if (currentRow.dir) {
                            dirNums += 1;
                            continue;
                        }
                        if (this.rowSelectMemo[currentRow.completePath]) {
                            checkNums += 1;
                        }
                    }
                }
                return {
                    checked: listNums > 0 && checkNums + dirNums === listNums,
                    indeterminate: listNums > 0 && checkNums > 0,
                };
            },
        },
        watch: {
            fileLocation: {
                /**
                 * @desc 编辑状态处理默认显示路径
                 */
                handler (fileLocation) {
                    if (this.isInnerChange) {
                        this.isInnerChange = false;
                        return;
                    }

                    this.rowSelectMemo = Object.freeze(fileLocation.reduce((result, item) => {
                        result[item] = true;
                        return result;
                    }, {}));
                    const getLength = path => path.split('/').length;
                    // 查找最短文件路径
                    if (fileLocation.length > 0) {
                        let [shortestPath] = fileLocation;
                        fileLocation.forEach((currentFileLocation) => {
                            if (getLength(shortestPath) > getLength(currentFileLocation)) {
                                shortestPath = currentFileLocation;
                            }
                        });
                        this.path = shortestPath.split('/').slice(0, -1)
                            .join('/');
                    }
                    setTimeout(() => {
                        this.fetchData();
                    });
                },
                immediate: true,
            },
        },
        created () {
            this.wholeTableRowSelect = false;
            this.isInnerChange = false;
        },
        methods: {
            /**
             * @desc 获取bucket存储桶数据列表
             */
            fetchData () {
                this.isLoading = true;
                FileService.fetchgetListFileNode({
                    fileSourceId: this.fileSourceId,
                    path: this.path,
                    name: this.name,
                    pageSize: this.pagination.limit,
                    start: parseInt(this.pagination.current - 1, 10) * this.pagination.limit,
                }).then((data) => {
                    this.tableData = Object.freeze(data.data);
                    // 不显示文件操作列
                    this.renderColumns = Object.freeze(data.metaData.properties.filter(_ => _.type !== 'buttonGroup'));
                    this.fileSourceInfo = Object.freeze(data.fileSourceInfo);
                    this.pagination = {
                        ...this.pagination,
                        count: data.total,
                    };
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            renderHeader (h) {
                return (
                <bk-checkbox value={this.wholeTableRowSelect} onChange={this.handlePageSelectToggle} />
                );
            },
            /**
             * @desc 重置文件筛选条件
             */
            resetSearchParams () {
                this.wholeTableRowSelect = false;
                this.name = '';
                this.pagination.current = 1;
            },
            triggerChange () {
                this.isInnerChange = true;
                this.$emit('on-file-change', Object.keys(this.rowSelectMemo));
            },
            /**
             * @desc 文件路径返回上一级
             */
            handleBackLast () {
                const lastPath = this.pathStack[this.pathStack.length - 2];
                this.handlePathLocation(lastPath.path);
            },
            /**
             * @desc 对话框页面显示跳转到文件源列表模板
             *
             * 重置已选的文件源
             */
            handleGoSourceList () {
                this.$emit('on-source-change', {
                    id: '',
                });
            },
            /**
             * @desc 面包屑切换列表
             * @param {String} path 文件路径
             *
             * 重置 name 筛选
             * 重置翻页
             */
            handlePathLocation (path) {
                if (_.trim(path, '/') === _.trim(this.path, '/')) {
                    return;
                }
                this.resetSearchParams();
                this.path = path;
                this.fetchData();
            },
            /**
             * @desc 行数据跳转链接
             * @param {String} path 文件路径
             *
             * 重置 name 筛选
             * 重置翻页
             */
            handleLink (path) {
                this.resetSearchParams();
                this.path = path;
                this.fetchData();
            },
            /**
             * @desc 文件搜索
             * @param {String} name 文件名
             *
             * 重置翻页
             */
            handleSearch (name) {
                this.resetSearchParams();
                this.name = name;
                this.fetchData();
            },
            /**
             * @desc 切换表格的全选状态
             * @param {Boolean} isChecked 最新选中状态
             */
            handlePageSelectToggle (isChecked) {
                this.isLoading = true;
                FileService.fetchgetListFileNode({
                    fileSourceId: this.fileSourceId,
                    path: this.path,
                    name: this.name,
                    pageSize: -1,
                    start: 0,
                }).then((data) => {
                    const tableData = data.data;
                    const renderColumns = data.metaData.properties;

                    const selectColumn = renderColumns.find(({ type }) => checkIsCheckboxColumn(type));
                    if (!selectColumn) {
                        return;
                    }

                    const rowSelectMemo = Object.assign({}, this.rowSelectMemo);

                    tableData.forEach((rowData) => {
                        if (!isChecked) {
                            delete rowSelectMemo[rowData.completePath];
                        } else if (parseCondition(selectColumn.enable, rowData)) {
                            rowSelectMemo[rowData.completePath] = true;
                        }
                    });
                    this.rowSelectMemo = Object.freeze(rowSelectMemo);
                    this.triggerChange();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 单行数据切换
             * @param {String} completePath 最新展示页
             * @param {Boolean} isChecked 最新选中状态
             */
            handleRowSelect (completePath, isChecked) {
                const rowSelectMemo = Object.assign({}, this.rowSelectMemo);
                if (isChecked) {
                    rowSelectMemo[completePath] = true;
                } else {
                    delete rowSelectMemo[completePath];
                }
                this.rowSelectMemo = Object.freeze(rowSelectMemo);
                this.triggerChange();
            },
            /**
             * @desc 翻页
             * @param {Number} current 最新展示页
             */
            handlePageChange (current) {
                this.pagination.current = current;
                this.fetchData();
            },
        },
    };
</script>
<style lang="postcss">
    .select-bucket {
        padding-top: 14px;

        .list-action-layout {
            margin-bottom: 14px;
        }
    }
</style>
