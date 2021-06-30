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
    <div class="choose-ip-dinamic-business-topology" v-bkloading="{ isLoading: topologyLoading || isLoading }">
        <div class="node-search">
            <bk-input :placeholder="$t('搜索拓扑节点')" right-icon="bk-icon icon-search" @input="handleSearch" />
        </div>
        <empty v-if="emptyTopologyOfAllBusiness" />
        <template v-else>
            <empty v-show="isSearchEmpty" class="topology-empty" />
            <div class="topology-node-tree">
                <scroll-faker>
                    <bk-big-tree
                        ref="tree"
                        show-link-line
                        show-checkbox
                        @check-change="handleCheckChange">
                        <div class="node-box" slot-scope="{ data }">
                            <div class="node-name">{{ data.name }}</div>
                            <div class="node-count">{{ data.payload.count }}</div>
                        </div>
                    </bk-big-tree>
                </scroll-faker>
            </div>
            
        </template>
    </div>
</template>
<script>
    import _ from 'lodash';
    import AppService from '@service/app-manage';
    import { ALL_APP_TYPE } from '@utils/constants';
    import Empty from '@components/empty';
    import {
        findAllChildNodeId,
        resetTree,
        parseIdInfo,
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
            node: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                isSearchEmpty: false,
                emptyTopologyOfAllBusiness: true,
            };
        },
        watch: {
            node (newNode, oldNode) {
                if (this.isSelfChange) {
                    this.isSelfChange = false;
                    return;
                }
                if (newNode.length < 1) {
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
                this.init(newNode, oldNode);
            },
        },
        created () {
            this.isSelfChange = false;
            this.isTreeRefresh = false;
        },
        mounted () {
            this.fetchAppList();
        },
        methods: {
            /**
             * @desc 获取业务列表
             *
             * 判断是否是全业务，全业务下没有动态拓扑
             */
            fetchAppList () {
                this.isLoading = true;
                AppService.fetchAppList()
                    .then((data) => {
                        const currentApp = _.find(data, _ => _.id === window.PROJECT_CONFIG.APP_ID);
                        this.emptyTopologyOfAllBusiness = currentApp.type === ALL_APP_TYPE;
                        if (this.emptyTopologyOfAllBusiness) {
                            return;
                        }
                        this.$nextTick(() => {
                            this.$refs.tree.setData(this.topologyNodeTree);
                            this.$refs.tree.setExpanded(this.topologyNodeTree[0].id, {
                                expanded: true,
                            });
                            this.init(this.node);
                        });
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 节点的选中状态改变
             * @param {Array} newNode 最新选中的节点值
             * @param {Array} oldNode 上次选中的节点值
             *
             * 外部传入值还原树的选中状态
             */
            init (newNode, oldNode = []) {
                this.isSearchEmpty = this.topologyNodeTree.length < 1;
                const isRemove = newNode.length < oldNode.length;
                const oldNodeId = oldNode.map(item => `#${item.type}#${item.id}`);
                const newNodeId = newNode.map(item => `#${item.type}#${item.id}`);
                
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
            handleFilterEmptyToggle () {
                this.$emit('on-topo-empty-filter');
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
                        list.forEach(() => {
                            
                        });
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
                this.$emit('on-change', 'nodeId', checkNodeIds.map((item) => {
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
