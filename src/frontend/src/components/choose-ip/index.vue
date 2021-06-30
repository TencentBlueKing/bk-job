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
    <lower-component level="custom" :custom="isShowDialog">
        <jb-dialog
            v-model="isShowDialog"
            class="choose-ip-dialog"
            :mask-close="false"
            :esc-close="false"
            :draggable="false"
            :width="1240"
            :media="mediaWidth">
            <div class="choose-ip-container" :style="containerStyles">
                <template v-if="isShowDialog">
                    <div class="action-tab">
                        <div class="tab-container">
                            <div class="tab-item" :class="{ active: activeTab === 'static' }" @click="handleTabChange('static')">
                                {{ $t('静态 - IP 选择') }}
                            </div>
                            <div class="tab-item" :class="{ active: activeTab === 'dynamic' }" @click="handleTabChange('dynamic')">
                                {{ $t('动态 - 拓扑选择') }}
                            </div>
                            <div class="tab-item" :class="{ active: activeTab === 'group' }" @click="handleTabChange('group')">
                                {{ $t('动态 - 分组选择') }}
                            </div>
                            <div class="tab-item" :class="{ active: activeTab === 'input' }" @click="handleTabChange('input')">
                                {{ $t('手动输入') }}
                            </div>
                        </div>
                    </div>
                    <div class="action-content" :style="contentStyles">
                        <keep-alive>
                            <component
                                ref="dataTree"
                                :is="panelCom"
                                class="fade-in"
                                :preview-id="previewId"
                                :topology-node-tree="topologyNodeTree"
                                :node="nodeId"
                                :host="hostList"
                                :dynamic-group="dynamicGroupId"
                                :topology-loading="isLoading"
                                :dialog-height="dialogHeight"
                                @on-group-preview="handleGroupPreview"
                                @on-input-change="handleInputChange"
                                @on-input-animate="handleInputAnimate"
                                @on-change="handleChange" />
                        </keep-alive>
                    </div>
                </template>
            </div>
            <template v-if="showGroupPreview">
                <preview-group v-model="showGroupPreview" :data="previewGroup" />
            </template>
            <template v-if="showChoosePreview">
                <preview
                    v-model="showChoosePreview"
                    :host="hostList"
                    :node="nodeId"
                    :group="dynamicGroupId"
                    @on-change="handlePreviewChange">
                    <div slot="desc" v-html="actionResult" />
                </preview>
            </template>
            <template slot="footer">
                <span v-if="error" class="ip-error">{{ error }}</span>
                <div
                    :id="previewId"
                    class="choose-result"
                    :class="currentChangeClass"
                    v-html="actionResult"
                    @click="handleShowChoosePreview" />
                <jb-popover-confirm
                    v-if="ipInputStatus"
                    :title="$t('操作确认')"
                    :content="$t('手动输入框有内容未添加到“已选择”列表，确认结束操作？')"
                    :confirm-handler="handleSubmit">
                    <bk-button class="mr10" theme="primary">{{ $t('确定') }}</bk-button>
                </jb-popover-confirm>
                <bk-button
                    v-if="!ipInputStatus"
                    class="mr10"
                    theme="primary"
                    :disabled="isSubmitDisable"
                    @click="handleSubmit">
                    {{ $t('确定') }}
                </bk-button>
                <bk-button @click="handleCancle">{{ $t('取消') }}</bk-button>
            </template>
        </jb-dialog>
    </lower-component>
</template>
<script>
    import _ from 'lodash';
    import AppService from '@service/app-manage';
    import I18n from '@/i18n';
    import TaskHostNodeModel from '@model/task-host-node';
    import {
        bigTreeTransformTopologyOfTopology,
        mergeInputHost,
        mergeTopologyHost,
        generateHostRealId,
    } from './components/utils';
    import RenderBusinessTopology from './components/render-business-topology';
    import RenderDynamicBusinessTopology from './components/render-dynamic-business-topology';
    import RenderDynamicGroup from './components/render-dynamic-group';
    import RenderIpInput from './components/render-ip-input';
    import PreviewGroup from './components/preview-group';
    import SidesliderBox from './components/sideslider-box';
    import Preview from './preview';

    const DIALOG_FOOTER_HEIGHT = 58;
    const CONTENT_TAB_HEIGHT = 42;

    export default {
        name: 'ChooseIp',
        components: {
            RenderBusinessTopology,
            RenderDynamicBusinessTopology,
            RenderDynamicGroup,
            RenderIpInput,
            PreviewGroup,
            SidesliderBox,
            Preview,
        },
        model: {
            prop: 'show',
            event: 'input',
        },
        props: {
            // 显示弹框
            show: {
                type: Boolean,
                default: false,
            },
            // 传入主机节点值
            hostNodeInfo: {
                type: Object,
                default: () => new TaskHostNodeModel({}).hostNodeInfo,
            },
            // 是否必填
            required: {
                type: Boolean,
                default: false,
            },
            // 错误提示
            errorTips: {
                type: String,
                default: I18n.t('服务器不能为空'),
            },
        },
        data () {
            this.hostListTopolagyLast = [];
            return {
                isShowDialog: false,
                isLoading: true,
                isSubmitDisable: false,
                // 预览结果
                showChoosePreview: false,
                // 预览节点合分组的主机详情
                showGroupPreview: false,
                activeTab: 'static',
                // 弹框的高度
                dialogHeight: 0,
                topologyNodeTree: [],
                nodeId: [],
                hostList: [],
                dynamicGroupId: [],
                previewGroup: {},
                error: '',
                currentChangeClass: '',
                //  手动的输入的内容是否提交
                ipInputStatus: false,
            };
        },
        computed: {
            /**
             * @desc 渲染tab组件
             * @returns {Object}
             */
            panelCom () {
                const comMap = {
                    static: RenderBusinessTopology,
                    dynamic: RenderDynamicBusinessTopology,
                    group: RenderDynamicGroup,
                    input: RenderIpInput,
                };
                return comMap[this.activeTab];
            },
            /**
             * @desc 弹框内容区样式
             * @returns {Object}
             */
            containerStyles () {
                return {
                    height: `${this.dialogHeight - DIALOG_FOOTER_HEIGHT}px`,
                };
            },
            /**
             * @desc tab内容区样式
             * @returns {Object}
             */
            contentStyles () {
                return {
                    height: `${this.dialogHeight - DIALOG_FOOTER_HEIGHT - CONTENT_TAB_HEIGHT}px`,
                };
            },
            /**
             * @desc 主机是否为空
             * @returns {Boolean}
             */
            isEmpty () {
                return !this.nodeId.length && !this.hostList.length && !this.dynamicGroupId.length;
            },
            /**
             * @desc 选择结果的展示
             * @returns {String}
             */
            actionResult () {
                const result = [];
                if (this.hostList.length > 0) {
                    result.push(`<span class="strong number choose-host">${this.hostList.length}</span>${I18n.t('台主机.select')}`);
                }
                if (this.nodeId.length > 0) {
                    result.push(`<span class="strong number choose-node">${this.nodeId.length}</span>${I18n.t('个节点.select')}`);
                }
                if (this.dynamicGroupId.length > 0) {
                    result.push(`<span class="strong number choose-group">${this.dynamicGroupId.length}</span>${I18n.t('个分组.select')}`);
                }
                if (result.length < 1) {
                    return '';
                }
                let resultText = result.join('，');
                if (resultText) {
                    resultText = `<span>${I18n.t('已选择.select')}：</span>${resultText}`;
                }
                return resultText;
            },
        },
        watch: {
            show: {
                /**
                 * @desc 每次打开重新拉取拓扑节点树
                 * @param {Boolean} show 显示 ip 选择器
                 */
                handler (show) {
                    if (show) {
                        this.fetchTopologyWithCount();
                    }
                    this.ipInputStatus = false;
                    this.isShowDialog = show;
                    this.isSubmitDisable = false;
                },
                immediate: true,
            },
            hostNodeInfo: {
                /**
                 * @desc 处理默认值
                 * @param {Object} hostNodeInfo 主机信息
                 */
                handler (hostNodeInfo) {
                    if (this.isSelfChange) {
                        this.isSelfChange = false;
                        return;
                    }
                    const { dynamicGroupList = [], ipList = [], topoNodeList = [] } = hostNodeInfo;

                    this.nodeId = Object.freeze([
                        ...topoNodeList,
                    ]);
                    this.hostList = Object.freeze(ipList.map(host => Object.assign({
                        realId: generateHostRealId(host),
                        ...host,
                    })));
                    this.hostListTopolagyLast = this.hostList;
                    this.dynamicGroupId = Object.freeze(dynamicGroupList);
                },
                immediate: true,
            },
        },
        created () {
            this.previewId = `chooseIPPreview_${_.random(1, 99999999)}`;
            this.isSelfChange = false;
            this.hostInput = [];
            this.mediaWidth = [
                1240, 1400, 1560, 1720,
            ];
        },
        mounted () {
            this.calcDialogHeight();
        },
        methods: {
            /**
             * @desc 获取拓扑节点树
             */
            fetchTopologyWithCount () {
                this.isLoading = true;
                AppService.fetchTopologyWithCount()
                    .then((data) => {
                        this.topologyNodeTree = Object.freeze(bigTreeTransformTopologyOfTopology([
                            data,
                        ]));
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 动态计算弹框高度占浏览器窗口的 80%
             */
            calcDialogHeight () {
                const dialogHeight = window.innerHeight * 0.8;
                this.dialogHeight = dialogHeight < 660 ? 660 : dialogHeight;
            },
            /**
             * @desc tab 面板切换
             */
            handleTabChange (payload) {
                this.activeTab = payload;
            },
            /**
             * @desc 选择值
             * @param {String} field 更新的字段名
             * @param {Any} value 字段值
             *
             * 静态 ip 选择和手动输入的结果需要合并
             */
            handleChange (field, value) {
                this.error = '';
                switch (field) {
                    case 'hostTopology':
                        this.hostList = Object.freeze(mergeTopologyHost(this.hostList, this.hostListTopolagyLast, value));
                        this.hostListTopolagyLast = value;
                        this.currentChangeClass = 'host';
                        break;
                    case 'hostInput':
                        this.hostList = Object.freeze(mergeInputHost(this.hostList, value));
                        this.currentChangeClass = 'host';
                        break;
                    case 'nodeId':
                        this.nodeId = Object.freeze(value);
                        this.currentChangeClass = 'node';
                        break;
                    case 'dynamicGroupId':
                        this.dynamicGroupId = Object.freeze(value);
                        this.currentChangeClass = 'group';
                        break;
                    default:
                        return '';
                }
            },
            /**
             * @desc 手动输入的输入状态
             *
             * 有输入结果但是没有添加需要给出提示结果
             */
            handleInputChange (status) {
                this.ipInputStatus = status;
            },
            /**
             * @desc 手动输入的动画状态
             * @param {Boolean} running 动画是否运行中
             *
             * 动画没结束禁止提交
             */
            handleInputAnimate (running) {
                this.isSubmitDisable = running;
            },
            /**
             * @desc 预览结果
             */
            handleShowChoosePreview () {
                this.showChoosePreview = true;
            },
            /**
             * @desc 手动输入的动画状态
             * @param {Object} hostNodeInfo 主机信息
             *
             */
            handlePreviewChange (hostNodeInfo) {
                const { dynamicGroupList, ipList, topoNodeList } = hostNodeInfo;
                this.nodeId = Object.freeze(topoNodeList);
                this.hostList = Object.freeze(ipList);
                this.dynamicGroupId = Object.freeze(dynamicGroupList);
            },
            /**
             * @desc 查看分组的主机详情
             * @param {Object} groupInfo 分组信息
             *
             */
            handleGroupPreview (groupInfo) {
                this.previewGroup = groupInfo;
                this.showGroupPreview = true;
            },
            /**
             * @desc 外部调用，重置选中状态
             */
            reset () {
                this.nodeId = [];
                this.hostList = [];
                this.dynamicGroupId = [];
            },
            /**
             * @desc 弹框关闭
             */
            close () {
                this.activeTab = 'static';
                this.$emit('on-cancel');
                this.$emit('input', false);
            },
            /**
             * @desc 提交操作结果
             */
            handleSubmit () {
                return Promise.resolve()
                    .then(() => {
                        if (this.required && this.isEmpty) {
                            this.error = I18n.t('源文件的服务器列表不允许为空');
                            return;
                        }
                        
                        // 保证先关闭jb-popover-confirm，再关闭dialog
                        Promise.resolve()
                            .then(() => {
                                this.isSelfChange = true;
                                this.$emit('on-change', {
                                    ipList: this.hostList,
                                    topoNodeList: this.nodeId,
                                    dynamicGroupList: this.dynamicGroupId,
                                });
                                this.close();
                            });
                    });
            },
            /**
             * @desc 取消当前操作
             *
             * 重置默认数据
             */
            handleCancle () {
                const {
                    dynamicGroupList = [],
                    ipList = [],
                    topoNodeList = [],
                } = this.hostNodeInfo;

                this.nodeId = Object.freeze([
                    ...topoNodeList,
                ]);
                this.hostList = Object.freeze(ipList.map(host => ({
                    ...host,
                    realId: generateHostRealId(host),
                })));
                this.dynamicGroupId = Object.freeze(dynamicGroupList);
                this.hostListTopolagyLast = this.hostList;
                this.isShowDialog = false;
                this.close();
            },
        },
    };
</script>
<style lang='postcss'>
    @keyframes fade-in {
        from {
            opacity: 0.1;
        }

        to {
            opacity: 1;
        }
    }

    @keyframes choose-result-ani {
        0% {
            transform: scale(1.5);
        }

        25% {
            transform: scale(1.2);
        }

        50% {
            transform: scale(1.4);
        }

        75% {
            transform: scale(1.1);
        }

        100% {
            transform: scale(0);
        }
    }

    .choose-ip-dialog {
        .bk-dialog-wrapper {
            padding-top: 20px;
            padding-bottom: 20px;
            overflow-y: scroll;
            text-align: center;

            &::-webkit-scrollbar {
                width: 0;
            }

            &::after {
                display: inline-block;
                width: 0;
                height: 100%;
                vertical-align: middle;
                content: '';
            }
        }

        .bk-dialog {
            display: inline-block;
            height: auto;
            text-align: left;
            vertical-align: middle;

            .bk-dialog-tool {
                display: none;
            }

            .bk-dialog-content {
                margin-bottom: 0 !important;
            }

            .bk-dialog-body {
                padding: 0;
            }

            .bk-dialog-footer {
                display: flex;
                justify-content: flex-end;
            }
        }

        .choose-ip-container {
            .action-tab {
                position: relative;
                z-index: 2;
                display: flex;
                flex-direction: column;

                .tab-container {
                    display: flex;
                    background: #fafbfd;
                }

                .tab-item {
                    display: flex;
                    height: 42px;
                    cursor: pointer;
                    border-right: 1px solid #dcdee5;
                    border-bottom: 1px solid #dcdee5;
                    flex: 1;
                    align-items: center;
                    justify-content: center;

                    &.active {
                        background: #fff;
                        border-bottom-color: transparent;
                    }
                }
            }
        }

        .bk-big-tree-node.is-selected {
            .node-box {
                .node-count {
                    color: #fff;
                    background: #a3c5fd;
                }
            }
        }

        .node-box {
            display: flex;
            align-items: center;

            .node-filter {
                height: 1em;
                padding-left: 7px;
                margin-right: 10px;
                margin-left: 10px;
                font-size: 12px;
                line-height: 1em;
                color: #979ba5;
                border-left: 1px solid #dcdee5;
                user-select: none;
            }

            .node-count {
                display: flex;
                height: 17px;
                padding: 0 4px;
                margin-left: auto;
                font-size: 12px;
                color: #979ba5;
                background: #f0f1f5;
                border-radius: 2px;
                align-items: center;
            }
        }

        .line-checkbox {
            position: relative;
            display: inline-block;
            width: 16px;
            height: 16px;
            overflow: hidden;
            font-size: 0;
            line-height: 18px;
            vertical-align: middle;
            cursor: pointer;
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
                transform: rotate(45deg) scaleY(1);
                transform-origin: center;
            }
        }

        .choose-all-tips {
            height: 30px !important;
            font-size: 12px !important;
            text-align: center !important;

            .strong {
                color: inherit;
            }

            .all-change {
                color: #3a84ff;
                cursor: pointer;
            }
        }

        .choose-result {
            display: flex;
            height: 32px;
            padding: 0 4px;
            margin-right: auto;
            font-size: 14px;
            line-height: 32px;
            color: #63656e;
            cursor: pointer;
            border-radius: 2px;

            &:hover {
                background: #ebecf0;
            }

            &.host {
                .choose-host {
                    animation: choose-result-ani 0.35s;
                }
            }

            &.node {
                .choose-node {
                    animation: choose-result-ani 0.35s;
                }
            }

            &.group {
                .choose-group {
                    animation: choose-result-ani 0.35s;
                }
            }
        }

        .ip-error {
            margin-right: 10px;
            font-size: 12px;
            color: #ff5656;
        }

        .fade-in {
            animation: fade-in 0.6s linear;
        }
    }
</style>
