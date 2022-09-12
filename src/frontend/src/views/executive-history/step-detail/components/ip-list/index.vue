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
        class="step-execute-host-list"
        :style="styles">
        <list-head
            class="ip-list-head"
            :columns="columnList"
            :show-columns="allShowColumn"
            @on-copy="handleCopyIP"
            @on-show-setting="handleShowSetting"
            @on-sort="handleSort" />
        <div
            ref="list"
            class="ip-list-body">
            <scroll-faker @on-scroll="handleScroll">
                <list-body
                    :columns="columnList"
                    :data="list"
                    :show-columns="allShowColumn"
                    @on-row-select="handleSelect" />
                <div
                    v-if="hasMore"
                    ref="loading"
                    class="list-loading">
                    <div class="loading-flag">
                        <Icon type="loading-circle" />
                    </div>
                    <div>{{ $t('history.加载中') }}</div>
                </div>
                <template v-if="list.length < 1 && !listLoading">
                    <Empty
                        v-if="!searchValue"
                        style="height: 100%;" />
                    <Empty
                        v-else
                        style="height: 100%;"
                        type="search">
                        <div style="font-size: 14px; color: #63656e;">
                            {{ $t('搜索结果为空') }}
                        </div>
                        <div style="margin-top: 8px; font-size: 12px; line-height: 16px; color: #979ba5;">
                            <span>{{ $t('可以尝试调整关键词') }}</span>
                            <span>{{ $t('或') }}</span>
                            <bk-button
                                text
                                @click="handleClearSearch">
                                {{ $t('清空搜索条件') }}
                            </bk-button>
                        </div>
                    </Empty>
                </template>
            </scroll-faker>
        </div>
        <div
            v-show="isSetting"
            class="list-column-select">
            <div class="select-body">
                <div class="title">
                    {{ $t('history.字段显示设置') }}
                </div>
                <bk-checkbox
                    :checked="isAllColumn"
                    :indeterminate="isIndeterminate"
                    @click.native="handleToggleAll">
                    {{ $t('history.全选') }}
                </bk-checkbox>
                <bk-checkbox-group v-model="tempAllShowColumn">
                    <bk-checkbox
                        v-for="item in columnList"
                        :key="item.name"
                        :checked="item.checked"
                        class="select-column"
                        :disabled="item.disabled"
                        :value="item.name">
                        {{ item.label }}
                    </bk-checkbox>
                </bk-checkbox-group>
            </div>
            <div class="select-footer">
                <bk-button
                    theme="primary"
                    @click="handleSubmitSetting">
                    {{ $t('history.确定') }}
                </bk-button>
                <bk-button @click="handleHideSetting">
                    {{ $t('history.取消') }}
                </bk-button>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import {
        getOffset,
    } from '@utils/assist';

    import Empty from '@components/empty';

    import ListBody from './list-body';
    import ListHead from './list-head';

    import I18n from '@/i18n';

    const COLUMN_CACHE_KEY = 'STEP_EXECUTE_IP_COLUMN';
    const LIST_ROW_HEIGHT = 40; // 每列高度

    export default {
        name: '',
        components: {
            Empty,
            ListHead,
            ListBody,
        },
        props: {
            name: {
                type: [
                    String,
                    Number,
                ],
                required: true,
            },
            data: {
                type: Array,
                default: () => [],
            },
            listLoading: {
                type: Boolean,
                default: false,
            },
            paginationLoading: {
                type: Boolean,
                default: false,
            },
            total: {
                type: Number,
                default: 0,
            },
            searchValue: String,
        },
        data () {
            let allShowColumn = [
                'displayIp',
                'totalTime',
                'cloudAreaName',
                'exitCode',
            ];
            if (localStorage.getItem(COLUMN_CACHE_KEY)) {
                allShowColumn = JSON.parse(localStorage.getItem(COLUMN_CACHE_KEY));
            }
            return {
                list: [],
                columnList: Object.freeze([
                    {
                        label: I18n.t('history.IP'),
                        name: 'displayIp',
                        width: '140',
                        checked: true,
                        disabled: true,
                    },
                    {
                        label: I18n.t('history.耗时(s)'),
                        name: 'totalTime',
                        orderField: 'totalTime',
                        order: '',
                        width: '120',
                        checked: true,
                    },
                    {
                        label: I18n.t('history.云区域'),
                        name: 'cloudAreaName',
                        orderField: 'cloudAreaId',
                        order: '',
                        width: '',
                        checked: true,
                    },
                    {
                        label: I18n.t('history.返回码'),
                        name: 'exitCode',
                        orderField: 'exitCode',
                        order: '',
                        width: '114',
                        checked: true,
                    },
                ]),
                page: 1,
                pageSize: 0,
                isSetting: false,
                allShowColumn,
                tempAllShowColumn: allShowColumn,
            };
        },
        computed: {
            /**
             * @desc 列选择是否半选状态
             * @return {Boolean}
             */
            isIndeterminate () {
                return this.tempAllShowColumn.length !== this.columnList.length;
            },
            /**
             * @desc 列选择是否全选状态
             * @return {Boolean}
             */
            isAllColumn () {
                return this.tempAllShowColumn.length === this.columnList.length;
            },
            /**
             * @desc IP 列表样式判断
             * @return {Object}
             */
            styles () {
                const width = 217 + (this.allShowColumn.length - 1) * 94;
                return {
                    width: `${width}px`,
                };
            },
            /**
             * @desc 列选择是否半选状态
             * @return {Boolean}
             */
            hasMore () {
                return this.page * this.pageSize < this.total;
            },
        },
        
        watch: {
            /**
             * @desc IP 列表名称变化时重置翻页
             */
            name () {
                this.page = 1;
            },
            data: {
                handler (data) {
                    // 切换分组时最新的分组数据一定来自API返回数据
                    // listLoading为false说明是本地切换不更新列表
                    if (!this.listLoading) {
                        return;
                    }
                    this.list = Object.freeze(data);
                },
                immediate: true,
            },
        },
        mounted () {
            this.calcPageSize();
            window.addEventListener('resize', this.handleScroll);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.handleScroll);
            });
        },
        methods: {
            /**
             * @desc 根据屏幕高度计算单页 pageSize
             */
            calcPageSize () {
                const { top } = getOffset(this.$refs.list);
                const windowHeight = window.innerHeight;
                const listHeight = windowHeight - top - 20;
                this.pageSize = parseInt(listHeight / LIST_ROW_HEIGHT + 6, 10);
                this.$emit('on-pagination-change', this.pageSize);
            },
            /**
             * @desc 滚动加载
             */
            handleScroll: _.throttle(function () {
                if (!this.hasMore) {
                    return;
                }
                const windowHeight = window.innerHeight;
                const { top } = this.$refs.loading.getBoundingClientRect();
                
                if (top - 80 < windowHeight) {
                    // 增加分页
                    this.page += 1;
                    this.$emit('on-pagination-change', this.page * this.pageSize);
                }
            }, 80),
            /**
             * @desc 复制ip
             */
            handleCopyIP () {
                this.$emit('on-copy');
            },
            /**
             * @desc 显示列配置面板
             */
            handleShowSetting () {
                this.isSetting = true;
            },
            /**
             * @desc 隐藏列配置面板
             */
            handleHideSetting () {
                this.isSetting = false;
            },
            /**
             * @desc 列配置面板全选状态切换
             */
            handleToggleAll () {
                if (this.isAllColumn) {
                    this.tempAllShowColumn = this.columnList.reduce((result, item) => {
                        if (item.disabled) {
                            result.push(item.name);
                        }
                        return result;
                    }, []);
                } else {
                    this.tempAllShowColumn = this.columnList.map(item => item.name);
                }
            },
            /**
             * @desc 保存列配置
             */
            handleSubmitSetting () {
                this.allShowColumn = [
                    ...this.tempAllShowColumn,
                ];
                this.isSetting = false;
                localStorage.setItem(COLUMN_CACHE_KEY, JSON.stringify(this.allShowColumn));
            },
            /**
             * @desc 表格排序
             * @param {Object} column 操作列数据
             */
            handleSort (column) {
                const {
                    orderField,
                    order,
                } = column;
                const newOrder = order === 1 ? 0 : 1;
                column.order = newOrder;
                
                this.columnList = Object.freeze(this.columnList.map((item) => {
                    item.order = '';
                    if (item.orderField === orderField) {
                        item.order = newOrder;
                    }
                    return { ...item };
                }));
                this.$emit('on-sort', {
                    orderField,
                    order: newOrder,
                });
                this.$emit('on-pagination-change', this.pageSize);
            },
            /**
             * @desc 选择表格一行数据
             * @param {Object} row 选择数据
             */
            handleSelect (row) {
                this.$emit('on-change', row);
            },
            /**
             * @desc 清空搜索
             */
            handleClearSearch () {
                this.$emit('on-clear-search');
            },
        },
    };
</script>
<style lang='postcss'>
    @keyframes list-loading-ani {
        0% {
            transform: rotateZ(0);
        }

        100% {
            transform: rotateZ(360deg);
        }
    }

    .step-execute-host-list {
        position: relative;
        width: 287px;
        height: 100%;
        max-height: 100%;
        min-height: 100%;
        transition: all 0.3s;

        .ip-list-body {
            height: calc(100% - 41px);
        }

        .ip-table {
            width: 100%;

            th,
            td {
                height: 40px;
                padding-left: 26px;
                line-height: 40px;
                text-align: left;
                white-space: nowrap;
                border-bottom: 1px solid #dcdee5;
            }

            th {
                position: relative;
                font-weight: normal;
                color: #313238;

                &.sort {
                    cursor: pointer;
                }

                .sort-box {
                    position: absolute;
                    top: 0;
                    display: inline-flex;
                    height: 100%;
                    margin-left: 9px;
                    font-size: 6px;
                    color: #c4c6cc;
                    justify-content: center;
                    flex-direction: column;

                    .top,
                    .bottom {
                        width: 0;
                        height: 0;
                        margin: 1px 0;
                        border: 5px solid transparent;

                        &.active {
                            color: #3a84ff;
                        }
                    }

                    .top {
                        border-bottom-color: currentcolor;
                    }

                    .bottom {
                        border-top-color: currentcolor;
                    }
                }
            }

            td {
                color: #63656e;
                cursor: pointer;
            }

            tbody {
                tr {
                    &.active {
                        background: #f0f1f5;

                        .active-flag {
                            font-size: 14px;
                        }
                    }

                    .active-flag {
                        padding: 0;
                        font-size: 0;
                        color: #979ba5;
                        text-align: center;
                    }
                }
            }

            .copy-ip-btn {
                margin-left: 8px;
                font-size: 12px;
                font-weight: normal;
                color: #3a84ff;
                cursor: pointer;
            }

            .list-action {
                width: 40px;
                height: 40px;
                padding: 0;
                font-size: 14px;
                color: #979ba5;
                text-align: center;
                cursor: pointer;
                border-left: 1px solid #dcdee5;
            }

            .success,
            .fail,
            .running,
            .waiting {
                &::before {
                    display: inline-block;
                    width: 3px;
                    height: 12px;
                    margin-right: 1em;
                    margin-left: -3px;
                    background: #2dc89d;
                    content: "";
                }
            }

            .fail {
                &::before {
                    background: #ea3636;
                }
            }

            .running {
                &::before {
                    background: #699df4;
                }
            }

            .waiting {
                &::before {
                    background: #dcdee5;
                }
            }
        }

        .list-loading {
            display: flex;
            height: 40px;
            font-size: 12px;
            color: #979ba5;
            text-align: center;
            align-items: center;
            justify-content: center;

            .loading-flag {
                display: flex;
                width: 20px;
                height: 20px;
                animation: list-loading-ani 1s linear infinite;
                align-items: center;
                justify-content: center;
                transform-origin: center center;
            }
        }

        .list-column-select {
            position: absolute;
            top: 45px;
            left: 0;
            z-index: 1;
            width: 100%;
            background: #fff;
            box-shadow: 1px 1px 5px 0 #dcdee5;
        }

        .select-body {
            padding: 15px 22px 30px;

            .title {
                margin-bottom: 22px;
                font-size: 16px;
                color: #313238;
            }
        }

        .select-column {
            margin-top: 20px;
            margin-right: 36px;

            &:last-child {
                margin-right: 0;
            }
        }

        .select-footer {
            display: flex;
            height: 50px;
            background: #fafbfd;
            border-top: 1px solid #dbdde4;
            align-items: center;
            justify-content: center;

            .bk-button {
                margin: 0 5px;
            }
        }
    }
</style>
