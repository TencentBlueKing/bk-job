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
    <div class="choose-ip-business-topology">
        <div class="topology-data" v-bkloading="{ isLoading: topologyLoading }">
            <div class="topology-node-search">
                <bk-input
                    :placeholder="$t('搜索拓扑节点')"
                    right-icon="bk-icon icon-search"
                    @input="handleNodeSearch" />
            </div>
            <div class="topology-node-tree">
                <scroll-faker>
                    <div class="wraper">
                        <bk-big-tree
                            ref="tree"
                            show-link-line
                            selectable
                            :expand-on-click="false"
                            @select-change="handleNodeChange">
                            <div class="node-box" slot-scope="{ node, data }">
                                <div class="node-name">{{ node.name }}</div>
                                <div
                                    v-if="node.level === 0"
                                    class="node-filter"
                                    @click="handleFilterEmptyToggle">
                                    <template v-if="isRenderEmptyTopoNode">
                                        <Icon type="eye-slash-shape" />
                                        <span>隐藏空节点</span>
                                    </template>
                                    <template v-else>
                                        <Icon type="eye-shape" />
                                        <span>恢复完整拓扑</span>
                                    </template>
                                </div>
                                <div class="node-count">{{ data.payload.count }}</div>
                            </div>
                        </bk-big-tree>
                    </div>
                </scroll-faker>
            </div>
            <empty v-if="isNodeEmpty" class="topology-empty" />
        </div>
        <div class="host-list">
            <mult-input
                :placeholder="$t('输入 主机 IP / 主机名 / 操作系统 / 云区域 进行搜索...')"
                class="host-search"
                @input="handleHostSearch" />
            <host-table
                :list="renderList"
                :is-search="isSearch"
                v-bkloading="{ isLoading }">
                <thead>
                    <tr>
                        <th style="width: 10.2%;">
                            <bk-checkbox
                                :value="isCheckedAll"
                                @click.native="handleToggleWholeAll" />
                        </th>
                        <th style="width: 18.9%;">{{ $t('主机IP') }}</th>
                        <th style="width: 12.8%;">{{ $t('云区域') }}</th>
                        <th style="width: 13.4%;">
                            <div class="head-cell" :class="{ 'is-filtered': agentFilter !== '' }">
                                <span>{{ $t('Agent 状态') }}</span>
                                <dropdown-menu>
                                    <Icon type="filter-fill" class="filer-flag" />
                                    <div slot="menu">
                                        <div
                                            class="dropdown-menu-item"
                                            :class="{ active: agentFilter === '' }"
                                            @click="handleAgentFiler('')">
                                            {{ $t('全部') }}
                                        </div>
                                        <div
                                            class="dropdown-menu-item"
                                            :class="{ active: agentFilter === 0 }"
                                            @click="handleAgentFiler(0)">
                                            {{ $t('异常') }}
                                        </div>
                                        <div
                                            class="dropdown-menu-item"
                                            :class="{ active: agentFilter === 1 }"
                                            @click="handleAgentFiler(1)">
                                            {{ $t('正常') }}
                                        </div>
                                    </div>
                                </dropdown-menu>
                            </div>
                        </th>
                        <th>{{ $t('主机名') }}</th>
                        <th style="width: 14.7%;">{{ $t('操作系统名称') }}</th>
                    </tr>
                </thead>
                <tbody v-if="renderList.length > 0">
                    <template v-for="(row, index) in renderList">
                        <tr class="host-row" :key="`${row.realId}_${index}`" @click="handleHostCheck(row)">
                            <td>
                                <bk-checkbox :checked="!!checkedMap[row.realId]" />
                            </td>
                            <td>
                                <div class="cell-text">{{ row.displayIp }}</div>
                            </td>
                            <td>
                                <div class="cell-text">{{ row.cloudAreaInfo.name || '--' }}</div>
                            </td>
                            <td>
                                <span v-if="row.alive">
                                    <icon svg type="normal" style="vertical-align: middle;" />
                                    <span style="vertical-align: middle;">{{ $t('正常') }}</span>
                                </span>
                                <span v-else>
                                    <icon svg type="abnormal" style="vertical-align: middle;" />
                                    <span style="vertical-align: middle;">{{ $t('异常') }}</span>
                                </span>
                            </td>
                            <td>
                                <div class="cell-text">{{ row.ipDesc || '--' }}</div>
                            </td>
                            <td>
                                <div class="cell-text">{{ row.os || '--' }}</div>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </host-table>
            <div v-if="pagination.pageSize > 0" style="padding: 16px 0;">
                <bk-pagination
                    align="right"
                    show-total-count
                    :show-limit="false"
                    :count="pagination.total"
                    :limit="pagination.pageSize"
                    :current.sync="pagination.page"
                    :limit-list="[pagination.pageSize]"
                    @change="handlePageChange"
                    small />
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import Cookie from 'js-cookie';
    import AppManageService from '@service/app-manage';
    import UserService from '@service/user';
    import { topoNodeCache } from '@utils/cache-helper';
    import Empty from '@components/empty';
    import {
        generateHostRealId,
        parseIdInfo,
        filterTopology,
    } from './utils';
    import MultInput from './mult-input';
    import HostTable from './host-table';
    import DropdownMenu from './dropdown-menu';

    const RENDER_EMPTY_TOPO_NODE_CACHE_KEY = 'ip_selector_render_empty_topo_node';
    const ROOT_NODE_ID = `#biz#${window.PROJECT_CONFIG.APP_ID}`;

    export default {
        name: '',
        components: {
            Empty,
            MultInput,
            HostTable,
            DropdownMenu,
        },
        inheritAttrs: false,
        props: {
            topologyNodeTree: {
                type: Array,
                default: () => [],
            },
            topologyLoading: {
                type: Boolean,
                default: true,
            },
            host: {
                type: Array,
                default: () => [],
            },
            dialogHeight: {
                type: Number,
                required: true,
            },
        },
        data () {
            this.selfChange = false;
            return {
                isLoading: true,
                isNodeEmpty: false,
                // 展示空的拓扑节点
                isRenderEmptyTopoNode: Cookie.get(RENDER_EMPTY_TOPO_NODE_CACHE_KEY) !== undefined,
                // 登录用户
                currentUser: {},
                selectedNodeId: ROOT_NODE_ID,
                renderList: [],
                checkedMap: {},
                isCheckedAll: false,
                searchContent: '',
                agentFilter: '',
                pagination: {
                    total: 0,
                    page: 1,
                    pageSize: 0,
                },
            };
        },
        computed: {
            /**
             * @desc 表格的全选状态
             */
            pageCheckInfo () {
                const info = {
                    indeterminate: false,
                    checked: false,
                };
                const checkedNums = Object.keys(this.checkedMap).length;
                info.indeterminate = checkedNums > 0;
                info.checked = checkedNums > 0 && checkedNums >= this.pagination.total;
                
                return info;
            },
            isSearch () {
                return [
                    0, 1,
                ].includes(this.agentFilter) || !!this.searchContent;
            },
        },
        watch: {
            /**
             * @desc 选中的主机更新
             */
            host: {
                handler (newHost) {
                    if (this.selfChange) {
                        this.selfChange = false;
                        return;
                    }
                    const checkedMap = {};
                    newHost.forEach((host) => {
                        checkedMap[host.realId] = host;
                    });
                    this.pagination.page = 1;
                    this.checkedMap = Object.freeze(checkedMap);
                },
                immediate: true,
            },
            topologyLoading (topologyLoading) {
                if (!topologyLoading) {
                    this.resetTopoTree();
                }
            },
        },
        created () {
            this.fetchUserInfo();
        },
        mounted () {
            this.calcPageSize();
            // 根节点 ID 默认已知，主动拉取节点下面的主机
            this.fetchTopologyHost();
        },
        methods: {
            /**
             * @desc 获取登陆用户信息
             */
            fetchUserInfo () {
                UserService.fetchUserInfo()
                    .then((data) => {
                        this.currentUser = Object.freeze(data);
                        this.isRenderEmptyTopoNode = topoNodeCache.getItem(data.username);
                    });
            },
            /**
             * @desc 获取拓扑节点下的主机列表
             */
            fetchTopologyHost: _.throttle(function () {
                if (!this.selectedNodeId) {
                    return;
                }
                const [objectId, instanceId] = parseIdInfo(this.selectedNodeId);
                const { page, pageSize } = this.pagination;
                this.isLoading = true;
                AppManageService.fetchTopologyHost({
                    appTopoNodeList: [{
                        objectId,
                        instanceId,
                    },
                    ],
                    agentStatus: this.agentFilter,
                    searchContent: this.searchContent,
                    pageSize,
                    start: (page - 1) * pageSize,
                }).then((data) => {
                    this.renderList = Object.freeze(data.data.map(host => Object.assign({
                        realId: generateHostRealId(host),
                    }, host)));
                    this.pagination.total = data.total;
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            }, 300),
            /**
             * @desc 权限时获取拓扑节点下的所有主机
             * @param {Boolean} check 列表全选状态
             */
            fetchTopologyHostWhole: _.throttle(function (check) {
                if (!this.selectedNodeId) {
                    return;
                }
                const [
                    objectId,
                    instanceId,
                ] = parseIdInfo(this.selectedNodeId);
                const checkedMap = {
                    ...this.checkedMap,
                };
                this.isLoading = true;
                AppManageService.fetchTopogyIPs({
                    appTopoNodeList: [{
                        objectId,
                        instanceId,
                    },
                    ],
                    agentStatus: this.agentFilter,
                    searchContent: this.searchContent,
                }).then((data) => {
                    const list = data.data;
                    list.forEach((realId) => {
                        const [
                            cloudAreaId, ip,
                        ] = realId.split(':');
                        if (check) {
                            checkedMap[realId] = {
                                ip,
                                cloudAreaInfo: {
                                    id: cloudAreaId,
                                },
                                realId,
                            };
                        } else {
                            delete checkedMap[realId];
                        }
                    });
                    this.checkedMap = Object.freeze(checkedMap);
                    this.triggerChange();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            }, 300),
            /**
             * @desc 初始化拓扑树
             * @param {Array} expandIds 需要展开的节点
             * @param {Boolean} emitEvent 选中节点时触发选中事件
             */
            resetTopoTree (expandIds, emitEvent = false) {
                const topologyNodeTree = filterTopology(this.topologyNodeTree, this.isRenderEmptyTopoNode);
                const expandIdArr = expandIds && expandIds.length > 0 ? expandIds : [this.selectedNodeId];
                this.$refs.tree.setData(topologyNodeTree);
                this.$nextTick(() => {
                    if (topologyNodeTree.length < 1) {
                        return;
                    }
                    this.$refs.tree.setSelected(this.selectedNodeId, {
                        emitEvent,
                    });
                    expandIdArr.forEach((nodeId) => {
                        this.$refs.tree.setExpanded(nodeId);
                    });
                });
            },
            /**
             * @desc 计算PageSize
             */
            calcPageSize () {
                const topOffset = 154;
                const bottomOffset = 116;
                const pageSize = Math.floor((this.dialogHeight - topOffset - bottomOffset) / 41);
                this.pagination.pageSize = pageSize;
            },
            /**
             * @desc 更新静态ip值
             */
            triggerChange () {
                this.selfChange = true;
                this.$emit('on-change', 'hostTopology', Object.values(this.checkedMap));
            },
            /**
             * @desc 切换拓扑树中主机为空的节点
             *
             * 缓存状态，切换过程中保持节点的展开状态
             */
            handleFilterEmptyToggle (event) {
                this.isRenderEmptyTopoNode = !this.isRenderEmptyTopoNode;
                const selectNode = this.$refs.tree.getNodeById(this.selectedNodeId);
                if (this.isRenderEmptyTopoNode) {
                    // 显示所有节点，节点的选中状态不变
                    event.stopPropagation();
                    topoNodeCache.clearItem();
                    // Cookie.set(RENDER_EMPTY_TOPO_NODE_CACHE_KEY, true);
                } else {
                    // 隐藏空节点时，如果已选的节点将被隐藏自动切换为选中根节点
                    if (selectNode.data.payload.count < 1) {
                        this.selectedNodeId = ROOT_NODE_ID;
                    } else {
                        event.stopPropagation();
                    }
                    topoNodeCache.setItem(this.currentUser.username);
                }
                // 更新节点树时保留树中节点的展开状态
                const expandIdArr = this.$refs.tree.nodes.reduce((result, node) => {
                    if (node.expanded) {
                        result.push(node.id);
                    }
                    return result;
                }, []);
                
                this.resetTopoTree(expandIdArr, true);
            },
            /**
             * @desc 搜索拓扑节点
             * @param {String} value 筛选字符
             */
            handleNodeSearch: _.debounce(function (value) {
                const data = this.$refs.tree.filter(value);
                if (data.length > 0) {
                    this.$nextTick(() => {
                        this.$refs.tree.setSelected(data[0].id, {
                            emitEvent: true,
                        });
                    });
                }
                this.isNodeEmpty = data.length < 1;
            }, 300),
            /**
             * @desc 拓扑节点改变
             * @param {Object} node 最新选中的拓扑节点
             *
             * 节点切换时重新拉取主机列表
             */
            handleNodeChange (node) {
                this.selectedNodeId = node.id;
                this.pagination.page = 1;
                if (!node.expanded) {
                    this.$refs.tree.setExpanded(this.selectedNodeId, {
                        expanded: true,
                    });
                }
                this.fetchTopologyHost();
            },
            /**
             * @desc 搜索主机
             * @param {String} str 筛选字符
             */
            handleHostSearch: _.debounce(function (str) {
                const value = _.trim(str);
                
                // 前后两次搜索内容相同直接返回
                if (this.searchContent === value) {
                    return;
                }
                
                // 搜索框内容为空格换行等空白符直接用空字符串搜索
                const realValue = value.replace(/\s/g, '');
                this.searchContent = realValue ? value : '';
                
                this.pagination.page = 1;
                this.fetchTopologyHost();
            }, 300),
            /**
             * @desc Agent状态筛选
             * @param {String} type 筛选字符
             */
            handleAgentFiler (type) {
                this.pagination.page = 1;
                this.agentFilter = type;
                this.fetchTopologyHost();
            },
            /**
             * @desc 全选节点下的所有主机
             */
            handleToggleWholeAll () {
                this.isCheckedAll = !this.isCheckedAll;
                this.fetchTopologyHostWhole(this.isCheckedAll);
            },
            /**
             * @desc 选择单台主机
             * @param {Object} host 主机信息
             */
            handleHostCheck (host) {
                const checkedMap = Object.assign({}, this.checkedMap);
                if (checkedMap[host.realId]) {
                    delete checkedMap[host.realId];
                } else {
                    checkedMap[host.realId] = {
                        ip: host.ip,
                        cloudAreaInfo: {
                            id: host.cloudAreaInfo.id,
                        },
                        realId: host.realId,
                    };
                }
                this.checkedMap = Object.freeze(checkedMap);
                this.triggerChange();
            },
            /**
             * @desc 切换分页
             * @param {Number} page 页码
             */
            handlePageChange (page) {
                this.pagination.page = page;
                this.fetchTopologyHost();
            },
        },
    };
</script>
<style lang="postcss">
    .choose-ip-business-topology {
        display: flex;
        height: 100%;
        padding-top: 20px;

        .topology-data {
            position: relative;
            height: 100%;
            overflow: hidden;
            border-right: 1px solid #dcdee5;
            flex: 0 0 33%;

            .topology-node-search {
                position: relative;
                z-index: 1;
                padding: 0 24px 20px;
            }

            .topology-node-tree {
                height: calc(100% - 72px);

                .wraper {
                    padding-right: 24px;
                    margin-left: 24px;
                }

                .bk-big-tree {
                    overflow: unset;

                    .bk-big-tree-node .node-content {
                        overflow: unset;
                    }

                    .node-name {
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                        flex: 0 1 auto;
                    }
                }
            }

            .topology-empty {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                left: 0;
            }
        }

        .host-list {
            flex: 2;
            padding: 0 24px;

            table {
                th,
                td {
                    &:first-child {
                        padding-left: 15px;
                    }
                }
            }

            .head-cell {
                &.is-filtered {
                    .filer-flag {
                        color: #63656e;
                    }
                }
            }

            .host-search {
                margin-bottom: 20px;
            }

            .check-flag {
                font-size: 20px;
                color: #63656e;
            }

            .filer-flag {
                font-size: 12px;
                color: #c4c6cc;
            }

            .host-row {
                cursor: pointer;
            }
        }
    }
</style>
