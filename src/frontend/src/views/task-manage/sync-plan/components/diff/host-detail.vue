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
        class="step-view-global-variable"
        @click="handlerView">
        <Icon
            class="type-flag"
            type="audit" />
        <jb-dialog
            v-model="isShowDetail"
            class="host-variable-detail-dialog"
            :ok-text="$t('template.关闭')"
            :title="title"
            :width="1020">
            <template #header>
                <div>{{ title }}</div>
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
    import _ from 'lodash';

    import TaskHostNodeModel from '@model/task-host-node';

    import {
        findParent,
    } from '@utils/vdom';

    import ScrollFaker from '@components/scroll-faker';

    import I18n from '@/i18n';
    // import ServerPanel from '@components/choose-ip/server-panel';

    export default {
        name: 'StepViewGlobalVariable',
        components: {
            ScrollFaker,
            // ServerPanel,
        },
        props: {
            type: {
                type: String,
                default: '',
            },
            name: {
                type: String,
                required: true,
            },
            data: {
                type: Object,
                default: () => ({}),
            },
            diffEnable: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                isShowDetail: false,
                isShowDiff: false,
                hostNodeInfo: {
                    dynamicGroupList: [],
                    hostList: [],
                    nodeList: [],
                },
                nodeDiff: {},
                hostDiff: {},
                groupDiff: {},
            };
        },
        computed: {
            title () {
                if (this.type) {
                    return this.type;
                }
                return `${I18n.t('template.全局变量.label')} - ${this.name}`;
            },
        },
        mounted () {
            this.composeNode = [];
            this.diffNodeMemo = {};
            this.composeHost = [];
            this.diffHostMemo = {};
            this.composeGroup = [];
            this.diffGroupMemo = {};
            this.checkDiff();
        },
        methods: {
            checkDiff () {
                const createVariable = () => {
                    const {
                        hostNodeInfo,
                    } = new TaskHostNodeModel({});
                    return {
                        defaultTargetValue: {
                            hostNodeInfo,
                        },
                    };
                };
                const dataSourceParent = findParent(this, 'SyncPlanStep2');
                let currentPlanVariable = _.find(dataSourceParent.planVariableList, _ => _.name === this.name);
                if (!currentPlanVariable) {
                    currentPlanVariable = createVariable();
                }
                let currentTemplateVariable = _.find(dataSourceParent.templateVariableList, _ => _.name === this.name);
                if (!currentTemplateVariable) {
                    currentTemplateVariable = createVariable();
                }

                const planValue = currentPlanVariable.defaultTargetValue.hostNodeInfo;
                const templateValue = currentTemplateVariable.defaultTargetValue.hostNodeInfo;
                
                // 对比节点
                const nodeDiffMap = {};
                const nodeList = [];
                const genNodeId = node => `${node.type}_${node.id}`;
                templateValue.nodeList.forEach((node) => {
                    nodeDiffMap[genNodeId(node)] = 'new';
                    nodeList.push(node);
                });
                planValue.nodeList.forEach((node) => {
                    if (nodeDiffMap[genNodeId(node)]) {
                        nodeDiffMap[genNodeId(node)] = 'normal';
                    } else {
                        nodeDiffMap[genNodeId(node)] = 'delete';
                        nodeList.push(node);
                    }
                });
                this.composeNode = Object.freeze(nodeList);
                this.diffNodeMemo = Object.freeze(nodeDiffMap);
                
                // 对比主机
                const hostDiffMap = {};
                const hostList = [];
                const genHostId = host => `${host.cloudAreaInfo.id}_${host.ip}`;
                templateValue.hostList.forEach((host) => {
                    hostDiffMap[genHostId(host)] = 'new';
                    hostList.push(host);
                });
                planValue.hostList.forEach((host) => {
                    if (hostDiffMap[genHostId(host)]) {
                        hostDiffMap[genHostId(host)] = 'normal';
                    } else {
                        hostDiffMap[genHostId(host)] = 'delete';
                        hostList.push(host);
                    }
                });
                this.composeHost = Object.freeze(hostList);
                this.diffHostMemo = Object.freeze(hostDiffMap);

                // 对比分组
                const groupDiffMap = {};
                const dynamicGroupList = [];
                templateValue.dynamicGroupList.forEach((group) => {
                    groupDiffMap[group] = 'new';
                    dynamicGroupList.push(group);
                });
                planValue.dynamicGroupList.forEach((group) => {
                    if (groupDiffMap[group]) {
                        groupDiffMap[group] = 'normal';
                    } else {
                        groupDiffMap[group] = 'delete';
                        dynamicGroupList.push(group);
                    }
                });
                
                this.composeGroup = Object.freeze(dynamicGroupList);
                this.diffGroupMemo = Object.freeze(groupDiffMap);
            },
            handlerView () {
                // const {
                //     dynamicGroupList,
                //     hostList,
                //     nodeList
                // } = this.data.hostNodeInfo
                // this.node = Object.freeze(nodeList)
                // this.host = Object.freeze(hostList)
                // this.dynamicGroup = Object.freeze(dynamicGroupList)
                this.hostNodeInfo = Object.freeze(this.data.hostNodeInfo);
                this.nodeDiff = {};
                this.hostDiff = {};
                this.groupDiff = {};
                this.isShowDetail = true;
            },
            handleToggleDiff (value) {
                if (value) {
                    this.hostNodeInfo = Object.freeze({
                        dynamicGroupList: this.composeGroup,
                        hostList: this.composeHost,
                        nodeList: this.composeNode,
                    });
                    // this.node = this.composeNode
                    this.nodeDiff = this.diffNodeMemo;
                    // this.host = this.composeHost
                    this.hostDiff = this.diffHostMemo;
                    // this.dynamicGroup = this.composeGroup
                    this.groupDiff = this.diffGroupMemo;
                } else {
                    this.handlerView();
                }
            },
        },
    };
</script>
<style lang="postcss">
    .host-variable-detail-dialog {
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
    .step-view-global-variable {
        display: inline-flex;
        align-items: center;
        padding-right: 10px;
        line-height: 1;
        cursor: pointer;

        .type-flag {
            font-size: 17px;
            color: #3a84ff;
            cursor: pointer;
        }
    }
</style>
