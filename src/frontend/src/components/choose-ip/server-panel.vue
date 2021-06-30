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
        class="server-panel"
        :class="classes">
        <div
            v-if="needRender"
            class="server-result-wraper"
            :class="{ loading: isLoading }"
            v-bkloading="{ isLoading }">
            <bk-collapse v-model="activePanel">
                <server-host
                    v-if="hostList.length > 0 || renderWithEmpty"
                    ref="host"
                    :data="hostList"
                    :editable="editable"
                    :diff="hostDiff"
                    @on-loading="handleLoading"
                    @on-change="handleHostChange" />
                <server-node
                    v-if="nodeInfo.length > 0 || renderWithEmpty"
                    ref="node"
                    :data="nodeInfo"
                    :editable="editable"
                    :diff="nodeDiff"
                    @on-loading="handleLoading"
                    @on-view="handleView"
                    @on-change="handleNodeChange" />
                <server-group
                    v-if="groupList.length > 0 || renderWithEmpty"
                    ref="group"
                    :data="groupList"
                    :editable="editable"
                    :diff="groupDiff"
                    @on-loading="handleLoading"
                    @on-view="handleView"
                    @on-change="handleGroupChange" />
            </bk-collapse>
        </div>
        <lower-component level="custom" :custom="showDetail">
            <host-detail
                v-model="showDetail"
                :data="viewInfo"
                :append="hostDetailAppend" />
        </lower-component>
        <lower-component level="custom" :custom="searchMode">
            <host-search
                v-show="searchMode"
                :data="searchData"
                :editable="editable"
                @on-change="handleSearchChange" />
        </lower-component>
    </div>
</template>
<script>
    import {
        encodeRegexp,
    } from '@utils/assist';
    import TaskHostNodeModel from '@model/task-host-node';
    import ServerNode from './view/node';
    import ServerHost from './view/host';
    import ServerGroup from './view/group';
    import HostDetail from './view/host-detail';
    import HostSearch from './view/host-search';
    import {
        generateHostRealId,
    } from './components/utils';

    const addCollapsePanel = (target, name) => {
        if (target.length > 0) {
            return;
        }
        if (!target.includes(name)) {
            target.push(name);
        }
    };

    export default {
        name: 'ServerPanel',
        components: {
            ServerNode,
            ServerHost,
            ServerGroup,
            HostDetail,
            HostSearch,
        },
        props: {
            hostNodeInfo: {
                type: Object,
                default: () => new TaskHostNodeModel(),
            },
            // 可编辑
            editable: {
                type: Boolean,
                default: false,
            },
            // 数据为空时是否显示
            renderWithEmpty: {
                type: Boolean,
                default: false,
            },
            // 主机详情弹框的渲染位置
            hostDetailAppend: {
                type: Function,
                default: () => document.querySelector('body'),
            },
            // 主机搜索模式
            searchMode: {
                type: Boolean,
                default: false,
            },
            // 主机搜索的筛选值
            searchData: {
                type: Array,
                default: () => [],
            },
            nodeDiff: {
                type: Object,
                default: () => ({}),
            },
            hostDiff: {
                type: Object,
                default: () => ({}),
            },
            groupDiff: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                activePanel: [],
                showDetail: false,
                viewInfo: {},
                nodeInfo: [],
                hostList: [],
                groupList: [],
                requestQueue: [],
            };
        },
        computed: {
            isLoading () {
                return this.requestQueue.length > 0;
            },
            needRender () {
                if (this.renderWithEmpty) {
                    return true;
                }
                return this.nodeInfo.length || this.hostList.length || this.groupList.length;
            },
            classes () {
                const stack = [];
                if (this.searchMode) {
                    stack.push('show-search');
                }
                return stack;
            },
        },
        watch: {
            hostNodeInfo: {
                handler (hostNodeInfo) {
                    const { dynamicGroupList, ipList, topoNodeList } = this.hostNodeInfo;
                    this.hostList = Object.freeze(ipList);
                    if (ipList.length > 0) {
                        addCollapsePanel(this.activePanel, 'host');
                    }
                    this.nodeInfo = Object.freeze(topoNodeList);
                    if (topoNodeList.length > 0) {
                        addCollapsePanel(this.activePanel, 'node');
                    }
                    this.groupList = Object.freeze(dynamicGroupList);
                    if (dynamicGroupList.length > 0) {
                        addCollapsePanel(this.activePanel, 'group');
                    }
                },
                immediate: true,
            },
        },
        methods: {
            /**
             * @desc 供外部调用的api
             * @param {Boolean} returnErrorIP true: 只返回返回异常主机ip；false: 返回所有主机ip
             *
             * 获取面板中的所有ip(主机)，根据ip去重
             */
            getAllIP (returnErrorIP = false) {
                const allHostList = [];
                if (this.$refs.host) {
                    allHostList.push(...this.$refs.host.getAllHost());
                }
                if (this.$refs.node) {
                    allHostList.push(...this.$refs.node.getAllHost());
                }
                if (this.$refs.group) {
                    allHostList.push(...this.$refs.group.getAllHost());
                }
                const ipMap = {};
                const errorIPMap = {};
                allHostList.forEach((host) => {
                    ipMap[host.ip] = 1;
                    if (host.alive !== 1) {
                        errorIPMap[host.ip] = 1;
                    }
                });
                
                // 异常主机包含无效主机
                if (returnErrorIP) {
                    return Object.keys(errorIPMap);
                }
                return Object.keys(ipMap);
            },
            /**
             * @desc 供外部调用的api，支持搜索（操作的对象是已选的主机，不包含分组和节点）
             * @param {String} filter 过滤项
             *
             * 获取主机面板中的所有主机信息，根据（ip + 云区域）去重
             */
            getAllHost (filter = '') {
                if (filter.trim() === '') {
                    return this.$refs.host.getAllHost();
                }
                if (!/^[0-9.]+$/g.test(filter)) {
                    return [];
                }
                
                const filterReg = new RegExp(`${encodeRegexp(filter.trim())}`);
                const hostMap = {};
                
                if (this.$refs.host) {
                    this.$refs.host.getAllHost().forEach((host) => {
                        if (filterReg.test(host.ip)) {
                            hostMap[generateHostRealId(host)] = host;
                        }
                    });
                }
                
                return Object.values(hostMap);
            },
            /**
             * @desc 外部刷新主机状态接口
             *
             * 获取主机面板中的所有主机（主机），根据（ip + 云区域）去重
             */
            refresh () {
                this.$refs.host && this.$refs.host.refresh();
                this.$refs.node && this.$refs.node.refresh();
                this.$refs.group && this.$refs.group.refresh();
            },
            /**
             * @desc 触发值的改变
             */
            trigger () {
                this.$emit('on-change', {
                    ipList: this.hostList,
                    topoNodeList: this.nodeInfo,
                    dynamicGroupList: this.groupList,
                });
            },
            /**
             * @desc 面板的loading状态
             * @param {Boolean} loading 每个面板的loading状态
             */
            handleLoading (loading) {
                if (loading) {
                    this.requestQueue.push(true);
                } else {
                    this.requestQueue.pop();
                }
            },
            /**
             * @desc 查看分组和节点下的主机列表
             * @param {Boolean} payload 查看详情的数据
             */
            handleView (payload) {
                this.showDetail = true;
                this.viewInfo = Object.freeze(payload);
            },
            /**
             * @desc 更新主机
             * @param {Array} hostList 最新的主机列表
             */
            handleHostChange (hostList) {
                this.hostList = Object.freeze(hostList);
                this.trigger();
            },
            /**
             * @desc 更新节点
             * @param {Array} nodeInfo 最新的节点列表
             */
            handleNodeChange (nodeInfo) {
                this.nodeInfo = Object.freeze(nodeInfo);
                this.trigger();
            },
            /**
             * @desc 更新分组
             * @param {Array} groupList 最新的分组列表
             */
            handleGroupChange (groupList) {
                this.groupList = Object.freeze(groupList);
                this.trigger();
            },
            /**
             * @desc 搜索主机面板删除了主机
             * @param {Array} removeHostList 在搜索面板中被删除的主机
             */
            handleSearchChange (removeHostList) {
                const removeHostMap = {};
                removeHostList.forEach((item) => {
                    removeHostMap[item.realId] = true;
                });

                const result = [];
                this.$refs.host.list.forEach((currentHost) => {
                    if (!removeHostMap[currentHost.realId]) {
                        result.push(currentHost);
                    }
                });
                this.hostList = Object.freeze(result);
                this.trigger();
            },
        },
    };
</script>
<style lang='postcss'>
    .server-panel {
        position: relative;
        z-index: 1;
        width: 100%;
        background: #fff;

        &.show-search {
            .server-result-wraper {
                display: none;
            }
        }

        .bk-collapse-item-header {
            display: flex;
            align-items: center;
            padding-left: 23px;

            .panel-title {
                padding-left: 23px;
            }
        }

        .server-result-wraper {
            height: 100%;

            &.loading {
                z-index: 1;
                min-height: 84px;
                overflow: hidden;
            }
        }

        .choose-ip-host-table {
            thead {
                th {
                    background: #fff;
                }
            }

            .invalid-list {
                td {
                    color: #c4c6cc;
                }
            }

            tbody:last-child {
                tr:last-child {
                    td {
                        border-bottom: none;
                    }
                }
            }
        }

        /* diff 样式 */
        .choose-ip-host-table { /* stylelint-disable-line */
            tr.delete {
                * {
                    color: #c4c6cc !important;
                    text-decoration: line-through;
                }
            }

            tr.new {
                td:first-child {
                    position: relative;

                    &::before {
                        position: absolute;
                        top: 50%;
                        width: 24px;
                        height: 16px;
                        margin-left: -32px;
                        font-size: 12px;
                        line-height: 16px;
                        color: #fff;
                        text-align: center;
                        background: #f0c581;
                        border-radius: 2px;
                        content: 'new';
                        transform: translateY(-50%);
                    }
                }
            }
        }
    }
</style>
