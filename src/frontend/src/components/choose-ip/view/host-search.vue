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
    <div class="server-search-panel">
        <div class="search-header">
            <span>
                <span>{{ $t('已筛选出') }}<span class="strong number">{{ list.length }}</span>{{ $t('个IP') }}</span>
                <span v-if="statisticsData.fail">（<span class="error">{{ statisticsData.fail }}</span>{{ $t('台Agent异常') }}）</span>
            </span>
            <action-extend :list="list" copyable>
                <template v-if="editable">
                    <div class="action-item" @click="handleRemoveAll">{{ $t('移除全部') }}</div>
                    <div class="action-item" @click="handleRemoveFail">{{ $t('移除异常') }}</div>
                </template>
            </action-extend>
        </div>
        <host-table :list="list" editable is-search @on-change="handleRemoveOne" />
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import HostTable from '../components/host-table';
    import ActionExtend from '../components/action-extend';
    import {
        sortHost,
        statisticsHost,
    } from '../components/utils';
    
    export default {
        name: '',
        components: {
            HostTable,
            ActionExtend,
        },
        props: {
            data: {
                type: Array,
                required: true,
            },
            editable: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                list: [],
                statisticsData: {},
            };
        },
        computed: {
            classes () {
                return {
                    scroll: this.data.length > 10,
                };
            },
        },
        watch: {
            data: {
                handler (host) {
                    if (this.isInnerChange) {
                        this.isInnerChange = false;
                        return;
                    }
                    this.page = 1;
                    this.list = Object.freeze(sortHost(host));
                    this.statisticsData = statisticsHost(host);
                },
                immediate: true,
            },
        },

        methods: {
            /**
             * @desc 删除所有筛选得到的主机
             */
            handleRemoveAll () {
                if (this.list.length < 1) {
                    this.messageSuccess(I18n.t('没有可移除主机'));
                    return;
                }
                const allList = this.list;
                this.list = [];
                this.isInnerChange = true;
                this.$emit('on-change', allList);
                this.messageSuccess(I18n.t('移除全部主机成功'));
            },
            /**
             * @desc 删除筛选结果中的异常主机
             *
             * 抛出change事件通知那些主机被删除
             */
            handleRemoveFail () {
                const effectiveIp = [];
                const failIp = [];
                this.list.forEach((currentHost) => {
                    if (currentHost.alive) {
                        effectiveIp.push(currentHost);
                    } else {
                        failIp.push(currentHost);
                    }
                });
                if (effectiveIp.length === this.list.length) {
                    this.messageSuccess(I18n.t('没有可移除主机'));
                    return;
                }
                this.list = Object.freeze(effectiveIp);
                this.isInnerChange = true;
                this.$emit('on-change', failIp);
                this.messageSuccess(I18n.t('移除异常主机成功'));
            },
            /**
             * @desc 删除单个主机
             * @param {Number} hostRealId 主机的唯一标识
             *
             * 抛出change事件通知那些主机被删除
             */
            handleRemoveOne (hostRealId) {
                // 内部显示删除
                const newList = [];
                let removeHost = null;
                this.list.forEach((currentHost) => {
                    if (currentHost.realId !== hostRealId) {
                        newList.push(currentHost);
                    } else {
                        removeHost = currentHost;
                    }
                });
                this.list = Object.freeze(newList);
                if (!removeHost) {
                    return;
                }
                
                this.isInnerChange = true;
                this.$emit('on-change', [
                    removeHost,
                ]);
            },
        },
    };
</script>
<style lang='postcss'>
    .server-search-panel {
        color: #63656e;
        border: 1px solid #dcdee5;

        .search-header {
            position: relative;
            height: 41px;
            padding-left: 60px;
            font-size: 12px;
            font-weight: bold;
            line-height: 41px;
            color: #63656e;
            background: #fafbfd;
            border-bottom: 1px solid #dcdee5;
        }

        .list-more {
            border-top: 1px solid #dcdee5;
        }

        .search-empty {
            height: 240px;
        }
    }
</style>
