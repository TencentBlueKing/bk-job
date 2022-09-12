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
    <div class="sync-plan-ip-detail">
        <div
            v-if="variableName"
            class="sync-plan-step-variable"
            @click="handlerView">
            <div class="variable-flag">
                <Icon type="host" />
            </div>
            <div
                class="variable-name"
                :tippy-tips="variableName">
                {{ variableName }}
            </div>
        </div>
        <div
            v-else
            v-bk-overflow-tips
            class="ip-text">
            {{ ipText }}
        </div>
        <Icon
            v-if="isNotEmpty"
            class="look-ip-detail"
            type="audit"
            @click="handlerView" />
        <jb-dialog
            v-model="isShowDetail"
            class="sync-ip-detail-dialog"
            :ok-text="$t('template.关闭')"
            :width="1020">
            <template #header>
                <div>{{ $t('template.执行目标') }}</div>
                <div class="display-diff">
                    <template v-if="diffEnable">
                        <bk-switcher
                            size="large"
                            theme="primary"
                            :value="isShowDiff"
                            @change="handleToggleDiff" />
                    </template>
                    <template v-else>
                        <bk-switcher
                            v-bk-tooltips="$t('template.无差异')"
                            disabled
                            size="large"
                            theme="primary"
                            :value="false" />
                    </template>
                    {{ $t('template.显示差异') }}
                </div>
            </template>
            <div class="content-wraper">
                <scroll-faker>
                    <!-- <server-panel
                        detail-mode="dialog"
                        :host-node-info="hostNodeInfo"
                        :node-diff="nodeDiff"
                        :host-diff="hostDiff"
                        :group-diff="groupDiff" /> -->
                    <ip-selector
                        readonly
                        show-view
                        :value="hostNodeInfo" />
                </scroll-faker>
            </div>
        </jb-dialog>
    </div>
</template>
<script>
    import TaskHostNodeModel from '@model/task-host-node';

    // import ServerPanel from '@components/choose-ip/server-panel';
    import {
        findParent,
    } from '@utils/vdom';

    import ScrollFaker from '@components/scroll-faker';

    import {
        findVariable,
    } from './utils';

    const isHostEmpty = (taskHostNode) => {
        if (!taskHostNode) {
            return true;
        }
        if (taskHostNode.variable) {
            return false;
        }
        if (taskHostNode.hostNodeInfo.nodeList.length > 0) {
            return false;
        }
        if (taskHostNode.hostNodeInfo.hostList.length > 0) {
            return false;
        }
        if (taskHostNode.hostNodeInfo.dynamicGroupList.length > 0) {
            return false;
        }
        return true;
    };

    export default {
        name: '',
        components: {
            ScrollFaker,
            // ServerPanel,
        },
        props: {
            preHost: {
                type: Object,
                required: true,
            },
            lastHost: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                isShowDetail: false,
                isShowDiff: false,
                variableName: '',
                ipText: '',
                hostEqual: true,
                hostNodeInfo: {},
                nodeDiff: {},
                hostDiff: {},
                groupDiff: {},
            };
        },
        computed: {
            isNotEmpty () {
                return this.variableName || this.ipText;
            },
            diffEnable () {
                if (isHostEmpty(this.preHost) || isHostEmpty(this.lastHost)) {
                    return false;
                }
                return !this.hostEqual;
            },
        },
        created () {
            const { hostNodeInfo } = new TaskHostNodeModel({});
            this.originHostNodeInfo = Object.freeze(hostNodeInfo);
            this.hostNodeInfo = Object.freeze(hostNodeInfo);

            this.composeNode = [];
            this.diffNodeMemo = {};
            this.composeHost = [];
            this.diffHostMemo = {};
            this.composeGroup = [];
            this.diffGroupMemo = {};
        },
        mounted () {
            this.stepParent = findParent(this, 'DiffTaskStep');
            this.dataSourceParent = findParent(this, 'SyncPlanStep2');
            this.init();
            this.checkDiff();
        },
        methods: {
            init () {
                let host = this.preHost;
                if (this.stepParent.type === 'sync-after') {
                    host = this.lastHost;
                }
                // 优先判断是否使用全局主机变量，如果是全局变量从全局变量中找到这个变量并显示变量的主机信息
                if (host.variable) {
                    this.variableName = host.variable;
                    const curVariable = findVariable(this.dataSourceParent.planVariableList, this.variableName);

                    this.originHostNodeInfo = Object.freeze(curVariable.defaultTargetValue.hostNodeInfo);
                } else {
                    this.originHostNodeInfo = Object.freeze(host.hostNodeInfo);
                    this.ipText = host.text;
                }
                this.hostNodeInfo = this.originHostNodeInfo;
            },
            checkDiff () {
                let preValue = this.preHost.hostNodeInfo;
                let lastValue = this.lastHost.hostNodeInfo;
                
                // 优先判断是否使用全局主机变量
                if (this.preHost.variable) {
                    const curVariable = findVariable(this.dataSourceParent.planVariableList, this.preHost.variable);
                    if (curVariable) {
                        preValue = curVariable.defaultTargetValue.hostNodeInfo;
                    }
                }
                if (this.lastHost.variable) {
                    const curVariable = findVariable(this.dataSourceParent.templateVariableList, this.lastHost.variable);
                    if (curVariable) {
                        lastValue = curVariable.defaultTargetValue.hostNodeInfo;
                    }
                }
                
                // 对比节点
                const nodeDiffMap = {};
                const nodeList = [];
                const genNodeId = node => `#${node.type}#${node.id}`;
                lastValue.nodeList.forEach((node) => {
                    nodeDiffMap[genNodeId(node)] = 'new';
                    nodeList.push(node);
                });
                preValue.nodeList.forEach((node) => {
                    const realNodeId = genNodeId(node);
                    if (nodeDiffMap[realNodeId]) {
                        nodeDiffMap[realNodeId] = 'same';
                    } else {
                        nodeDiffMap[realNodeId] = 'delete';
                        nodeList.push(node);
                    }
                });
                this.composeNode = Object.freeze(nodeList);
                this.diffNodeMemo = Object.freeze(nodeDiffMap);
                Object.values(this.diffNodeMemo).forEach((value) => {
                    if (value !== 'same') {
                        this.hostEqual = false;
                    }
                });
                
                // 对比主机
                const hostDiffMap = {};
                const hostList = [];
                const genHostId = host => `${host.cloudAreaInfo.id}:${host.ip}`;
                lastValue.hostList.forEach((host) => {
                    hostDiffMap[genHostId(host)] = 'new';
                    hostList.push(host);
                });
                preValue.hostList.forEach((host) => {
                    const realHostId = genHostId(host);
                    if (hostDiffMap[realHostId]) {
                        hostDiffMap[realHostId] = 'same';
                    } else {
                        hostDiffMap[realHostId] = 'delete';
                        hostList.push(host);
                    }
                });
                this.composeHost = Object.freeze(hostList);
                this.diffHostMemo = Object.freeze(hostDiffMap);
                Object.values(this.diffHostMemo).forEach((value) => {
                    if (value !== 'same') {
                        this.hostEqual = false;
                    }
                });

                // 对比分组
                const groupDiffMap = {};
                const dynamicGroupList = [];
                lastValue.dynamicGroupList.forEach((group) => {
                    groupDiffMap[group] = 'new';
                    dynamicGroupList.push(group);
                });
                preValue.dynamicGroupList.forEach((group) => {
                    if (groupDiffMap[group]) {
                        groupDiffMap[group] = 'same';
                    } else {
                        groupDiffMap[group] = 'delete';
                        dynamicGroupList.push(group);
                    }
                });
                
                this.composeGroup = Object.freeze(dynamicGroupList);
                this.diffGroupMemo = Object.freeze(groupDiffMap);
                Object.values(this.diffGroupMemo).forEach((value) => {
                    if (value !== 'same') {
                        this.hostEqual = false;
                    }
                });
            },
            handlerView () {
                this.hostNodeInfo = this.originHostNodeInfo;
                this.nodeDiff = {};
                this.hostDiff = {};
                this.groupDiff = {};
                this.isShowDetail = true;
                this.isShowDiff = false;
            },
            handleToggleDiff (value) {
                if (value) {
                    this.hostNodeInfo = Object.freeze({
                        dynamicGroupList: this.composeGroup,
                        hostList: this.composeHost,
                        nodeList: this.composeNode,
                    });
                    this.nodeDiff = this.diffNodeMemo;
                    this.hostDiff = this.diffHostMemo;
                    this.groupDiff = this.diffGroupMemo;
                } else {
                    this.handlerView();
                }
            },
        },
    };
</script>
<style lang="postcss">
    .sync-ip-detail-dialog {
        .bk-dialog-tool {
            display: none;
        }

        .bk-dialog-header,
        .bk-dialog-footer {
            position: relative;
            z-index: 99999;
            background: #fff;
        }

        .bk-dialog-header {
            display: flex;
            align-items: center;
            height: 68px;
            padding: 0 24px;
            font-size: 20px;
            color: #000;
            border-bottom: 1px solid #dcdee5;

            .display-diff {
                display: flex;
                align-items: center;
                margin-left: auto;
                font-size: 14px;
                color: #63656e;

                .bk-switcher {
                    margin-right: 10px;
                }
            }
        }

        .bk-dialog-wrapper .bk-dialog-header .bk-dialog-header-inner {
            font-size: 20px;
            color: #000;
            text-align: left;
        }

        .bk-dialog-wrapper .bk-dialog-body {
            padding: 0;
        }

        .content-wraper {
            height: 450px;
            max-height: 450px;
            min-height: 450px;
            margin-top: -1px;
        }

        button[name="cancel"] {
            display: none;
        }
    }
</style>
<style lang='postcss' scoped>
    .sync-plan-ip-detail {
        position: relative;
        display: flex;
        align-items: center;

        .sync-plan-step-variable {
            display: flex;
            overflow: hidden;
            cursor: pointer;

            .variable-flag {
                display: flex;
                width: 24px;
                height: 24px;
                font-size: 13px;
                color: #fff !important;
                background: #c4c6cc;
                border-bottom-left-radius: 2px;
                border-top-left-radius: 2px;
                align-items: center;
                justify-content: center;
            }

            .variable-name {
                display: flex;
                height: 24px;
                padding: 0 10px;
                font-size: 12px;
                color: #63656e;
                background: #fff;
                border: 1px solid #dcdee5;
                border-left: none;
                border-top-right-radius: 2px;
                border-bottom-right-radius: 2px;
                align-items: center;
                justify-content: center;
            }
        }

        .ip-text {
            height: 24px;
            overflow: hidden;
            line-height: 24px;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .look-ip-detail {
            padding: 4px 5px;
            font-size: 17px;
            color: #3a84ff;
            cursor: pointer;
        }
    }
</style>
