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
        v-bkloading="{
            isLoading: topologyLoading,
        }"
        class="choose-ip-dinamic-business-topology">
        <div class="node-search">
            <bk-input
                :placeholder="$t('搜索拓扑节点')"
                right-icon="bk-icon icon-search"
                @input="handleSearch" />
        </div>
        <empty v-if="emptyTopologyOfAllBusiness" />
        <template v-else>
            <empty
                v-show="isSearchEmpty"
                class="topology-empty" />
            <div class="topology-node-tree">
                <scroll-faker>
                    <bk-big-tree
                        ref="tree"
                        :expand-on-click="false"
                        show-checkbox
                        show-link-line
                        @check-change="handleCheckChange">
                        <div
                            slot-scope="{ node: nodeItem, data }"
                            class="node-box">
                            <div class="node-name">
                                {{ data.name }}
                            </div>
                            <div
                                v-if="nodeItem.level === 0"
                                class="node-filter"
                                @click="handleFilterEmptyToggle">
                                <template v-if="isRenderEmptyTopoNode">
                                    <Icon type="eye-slash-shape" />
                                    <span>{{ $t('隐藏空节点') }}</span>
                                </template>
                                <template v-else>
                                    <Icon type="eye-shape" />
                                    <span>{{ $t('恢复完整拓扑') }}</span>
                                </template>
                            </div>
                            <div class="node-count">
                                {{ data.payload.count }}
                            </div>
                        </div>
                    </bk-big-tree>
                </scroll-faker>
            </div>
        </template>
    </div>
</template>
<script>
    import _ from 'lodash';

    import UserService from '@service/user';

    import { topoNodeCache } from '@utils/cache-helper';

    import Empty from '@components/empty';

    import {
        filterTopology,
        findAllChildNodeId,
        parseIdInfo,
        resetTree,
    } from './utils';

    export default {
        name: '',
        components: {
            Empty,
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
            topoNodeList: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                currentUser: {},
                isRenderEmptyTopoNode: false,
                isSearchEmpty: false,
                // 业务集下没有动态拓扑
                emptyTopologyOfAllBusiness: window.PROJECT_CONFIG.SCOPE_TYPE === 'biz_set',
            };
        },
        watch: {
            /**
             * @desc 更新 topo 树节点的选中状态
             */
            topoNodeList (newTopoNodeList, oldTopoNodeList) {
                if (this.isSelfChange) {
                    this.isSelfChange = false;
                    return;
                }
                // 没有选中的节点时重置 topo 树节点的所有状态
                if (newTopoNodeList.length < 1) {
                    resetTree(this.topologyNodeTree, (node) => {
                        this.$refs.tree.setChecked(node.id, {
                            checked: false,
                        });
                        this.$refs.tree.setDisabled(node.id, {
                            disabled: false,
                        });
                    });
                    return;
                }
                this.recoverTreeState(newTopoNodeList, oldTopoNodeList);
            },
        },
        created () {
            this.isSelfChange = false;
            this.isTreeRefresh = false;
            this.fetchUserInfo();
        },
        mounted () {
            if (this.emptyTopologyOfAllBusiness) {
                return;
            }
            this.$nextTick(() => {
                this.$refs.tree.setData(this.topologyNodeTree);
                this.$refs.tree.setExpanded(this.topologyNodeTree[0].id, {
                    expanded: true,
                });
                this.recoverTreeState(this.topoNodeList);
            });
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
             * @desc 节点的选中状态改变
             * @param {Array} newNode 最新选中的节点值
             * @param {Array} oldNode 上次选中的节点值
             *
             * 外部传入值还原树的选中状态
             */
            recoverTreeState (newTopoNodeList, oldTopoNodeList = []) {
                this.isSearchEmpty = this.topologyNodeTree.length < 1;
                const isRemove = newTopoNodeList.length < oldTopoNodeList.length;
                const oldNodeId = oldTopoNodeList.map(item => `#${item.type}#${item.id}`);
                const newNodeId = newTopoNodeList.map(item => `#${item.type}#${item.id}`);
                
                if (isRemove) {
                    // 外部删除操作
                    const needRemoveCheckNode = _.difference(oldNodeId, newNodeId);
                    this.isTreeRefresh = true;
                    this.$refs.tree.setChecked(needRemoveCheckNode, {
                        emitEvent: true,
                        checked: false,
                    });
                } else {
                    // 新增，只能出现在初始化的时候
                    const needSelectNode = newNodeId;
                    if (needSelectNode.length > 0) {
                        this.isTreeRefresh = true;
                        this.$refs.tree.setChecked(needSelectNode, {
                            emitEvent: true,
                            checked: true,
                        });
                    }
                }
            },
            
            handleFilterEmptyToggle (event) {
                this.isRenderEmptyTopoNode = !this.isRenderEmptyTopoNode;
                if (this.isRenderEmptyTopoNode) {
                    // 显示所有节点，节点的选中状态不变
                    event.stopPropagation();
                    topoNodeCache.clearItem();
                } else {
                    topoNodeCache.setItem(this.currentUser.username);
                }
                // 更新节点树时保留树中节点的展开状态
                const expandIdListMemo = this.$refs.tree.nodes.reduce((result, node) => {
                    if (node.expanded) {
                        result.push(node.id);
                    }
                    return result;
                }, []);
                // 过滤 topo 树
                const topologyNodeTree = filterTopology(this.topologyNodeTree, this.isRenderEmptyTopoNode);
                // 重新渲染 topo 树
                this.$refs.tree.setData(topologyNodeTree);
                this.$nextTick(() => {
                    if (topologyNodeTree.length < 1) {
                        return;
                    }
                    // 还原选中状态
                    this.$refs.tree.setChecked(this.topoNodeList.map(item => `#${item.type}#${item.id}`), {
                        emitEvent: false,
                    });
                    // 还原展开状态
                    expandIdListMemo.forEach((nodeId) => {
                        this.$refs.tree.setExpanded(nodeId);
                    });
                });
            },
            /**
             * @desc 节点的选中状态改变
             * @param {Array} allCheckNode 所有选中的节点
             * @param {Object} node 当前操作的节点
             */
            handleCheckChange (allCheckNode, node) {
                const filterCheckNode = (nodeList) => {
                    const expireNodeMap = {};
                    if (nodeList.length < 1) {
                        return [];
                    }
                    const search = (list) => {
                        let currentNode = null;
                        let index = 0;
                        while (index < list.length) {
                            const currentNodeId = list[index];
                            currentNode = this.$refs.tree.getNodeById(currentNodeId);
                            if (!expireNodeMap[currentNodeId]) {
                                break;
                            }
                            // eslint-disable-next-line no-plusplus
                            index++;
                        }
                        if (index >= list.length) {
                            return list;
                        }
                        
                        const currentNodeChildIds = findAllChildNodeId(currentNode);
                        const validList = _.difference(list, currentNodeChildIds);
                        expireNodeMap[currentNode.id] = true;
                        return search(validList);
                    };
                    return search(nodeList);
                };
                // 如果父级节点和子节点同时被添加过滤掉子节点
                const checkNodeIds = filterCheckNode(allCheckNode);

                if (this.isTreeRefresh) {
                    this.isSelfChange = false;
                    this.isTreeRefresh = false;
                    return;
                }
                this.isSelfChange = true;
                this.$emit('on-change', 'topoNodeList', checkNodeIds.map((item) => {
                    const [type, id] = parseIdInfo(item);
                    return {
                        type,
                        id,
                    };
                }));
            },
            /**
             * @desc 拓扑节点搜索
             * @param {String} value 筛选值
             */
            handleSearch: _.debounce(function (value) {
                if (this.emptyTopologyOfAllBusiness) {
                    return;
                }
                const data = this.$refs.tree.filter(value);
                this.isSearchEmpty = data.length < 1;
            }, 300),
        },
    };
</script>
<style lang="postcss">
    .choose-ip-dinamic-business-topology {
        position: relative;
        height: 100%;
        padding: 0 24px;

        .node-search {
            position: relative;
            z-index: 1;
            padding-top: 20px;
        }

        .topology-node-tree {
            height: calc(100% - 92px);
            margin-top: 20px;
        }

        .node-box {
            .node-count {
                margin-left: 8px !important;
            }
        }

        .topology-empty {
            position: absolute;
            right: 0;
            left: 0;
            width: 100%;
            margin-top: 90px;
        }
    }
</style>
