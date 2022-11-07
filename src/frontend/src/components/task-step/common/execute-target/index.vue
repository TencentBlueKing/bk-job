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
        class="task-step-execute-target"
        :class="{
            'only-host': mode === 'onlyHost',
        }">
        <jb-form-item
            ref="targetServerRef"
            :label="$t('目标服务器')"
            :property="property"
            required
            :rules="rules">
            <div
                ref="actionBox"
                style="display: flex;">
                <template v-if="mode === 'onlyHost'">
                    <!-- 快速执行场景只能操作主机列表 -->
                    <bk-button
                        class="mr10"
                        @click="handleShowChooseIp">
                        <Icon type="plus" />
                        {{ $t('添加服务器') }}
                    </bk-button>
                </template>
                <template v-else>
                    <!-- 作业步骤场景可以切换主机列表和主机变量 -->
                    <compose-form-item>
                        <bk-select
                            :clearable="false"
                            :style="targetSelectorStyle"
                            :value="targetType"
                            @change="handleTargetTypeChange">
                            <bk-option
                                id="variable"
                                :name="$t('全局变量')" />
                            <bk-option
                                id="hostNodeInfo"
                                :name="$t('手动添加')" />
                        </bk-select>
                        <template v-if="isGolbalVariableType">
                            <bk-select
                                class="server-global-variable-select"
                                :clearable="false"
                                :placeholder="$t('请选择主机列表变量')"
                                :value="localVariable"
                                @change="handleVariableChange">
                                <bk-option
                                    v-for="(item, index) in variable"
                                    :id="item.name"
                                    :key="index"
                                    :name="item.name" />
                            </bk-select>
                        </template>
                        <template v-else>
                            <bk-button
                                class="w120 mr10"
                                @click="handleShowChooseIp">
                                <Icon type="plus" />
                                {{ $t('添加服务器') }}
                            </bk-button>
                        </template>
                    </compose-form-item>
                </template>
                <template v-if="isShowServerAction">
                    <bk-dropdown-menu>
                        <bk-button
                            slot="dropdown-trigger"
                            class="mr10"
                            type="primary">
                            <span>{{ $t('复制IP') }}</span>
                            <Icon
                                class="action-flag"
                                type="down-small" />
                        </bk-button>
                        <ul
                            slot="dropdown-content"
                            class="bk-dropdown-list">
                            <li><a @click="handleCopyAll">{{ $t('全部IP') }}</a></li>
                            <li><a @click="handleCopyFail">{{ $t('异常IP') }}</a></li>
                        </ul>
                    </bk-dropdown-menu>
                    <bk-button
                        class="mr10"
                        @click="handleClearAll">
                        <span>{{ $t('清空') }}</span>
                    </bk-button>
                    <bk-button
                        type="primary"
                        @click="handleRefreshHost">
                        {{ $t('刷新状态') }}
                    </bk-button>
                </template>
                <bk-input
                    v-if="isShowHostSearchInput"
                    class="ip-search"
                    :placeholder="$t('筛选主机')"
                    right-icon="bk-icon icon-search"
                    :value="searchText"
                    @change="handleHostSearch" />
            </div>
            
            <!-- <server-panel
                    v-show="isShowServerPanel"
                    ref="serverPanel"
                    class="view-server-panel"
                    :host-node-info="localHost"
                    :search-mode="Boolean(searchText)"
                    :search-data="searchData"
                    editable
                    @on-change="handleHostChange" /> -->
            <ip-selector
                ref="ipSelector"
                :show-dialog="isShowChooseIp"
                show-view
                :value="localHost"
                :view-search-key="searchText"
                @change="handleHostChange"
                @close-dialog="handleCloseIpSelector" />
        </jb-form-item>
        <!-- <choose-ip
            v-model="isShowChooseIp"
            :host-node-info="localHost"
            @on-change="handleHostChange" /> -->
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskHostNodeModel from '@model/task-host-node';

    import { execCopy } from '@utils/assist';

    import ComposeFormItem from '@components/compose-form-item';

    import I18n from '@/i18n';
    // import ChooseIp from '@components/choose-ip';
    // import ServerPanel from '@components/choose-ip/server-panel';

    export default {
        components: {
            ComposeFormItem,
            // ChooseIp,
            // ServerPanel,
        },
        inheritAttrs: false,
        props: {
            taskHostNode: {
                type: Object,
                required: true,
            },
            variable: {
                type: Array,
                default: () => [],
            },
            property: {
                type: String,
            },
            mode: {
                type: String,
                default: '', // onlyHost: 快速执行只可以选择主机列表
            },
        },
        data () {
            return {
                isShowChooseIp: false,
                searchText: '',
                isSearchMode: false,
                searchData: [],
                targetType: 'variable', // variable：主机变量；hostNodeInfo：手动添加
                localVariable: '',
                localHost: {},
            };
        },
        computed: {
            /**
             * @desc 执行目标是否是全局变量
             * @returns {Boolean}
             */
            isGolbalVariableType () {
                return this.targetType === 'variable';
            },
            /**
             * @desc 是否显示主机结果面板
             * @returns {Boolean}
             */
            isShowServerPanel () {
                if (this.isGolbalVariableType) {
                    return false;
                }
                return !TaskHostNodeModel.isHostNodeInfoEmpty(this.localHost);
            },
            /**
             * @desc 是否显示主机结果快捷操作
             * @returns {Boolean}
             */
            isShowServerAction () {
                if (this.isGolbalVariableType) {
                    return false;
                }
                return !TaskHostNodeModel.isHostNodeInfoEmpty(this.localHost);
            },
            /**
             * @desc 清除异常主机是否可用
             * @returns {Boolean}
             */
            isClearFailDisabled () {
                return this.localHost.hostList.length < 1;
            },
            /**
             * @desc 选择的主机才显示主机搜索框
             * @returns {Boolean}
             */
            isShowHostSearchInput () {
                if (this.isGolbalVariableType) {
                    return false;
                }
                return this.localHost.hostList.length > 0;
            },
            /**
             * @desc 切换执行目标选择的展示样式
             * @returns {Object}
             */
            targetSelectorStyle () {
                return {
                    width: this.$i18n.locale === 'en-US' ? '156px' : '120px',
                };
            },
        },
        watch: {
            taskHostNode: {
                handler (taskHostNode) {
                    const {
                        hostNodeInfo,
                        variable,
                    } = taskHostNode;

                    this.localHost = hostNodeInfo;
                    this.localVariable = variable;
                    if (this.mode !== 'onlyHost') {
                        // 编辑态，执行目标为服务器列表
                        if (!TaskHostNodeModel.isHostNodeInfoEmpty(this.localHost)) {
                            this.targetType = 'hostNodeInfo';
                        }
                    } else {
                        this.targetType = 'hostNodeInfo';
                    }
                },
                immediate: true,
            },
        },
        created () {
            // 执行目标是主机变量
            if (this.isGolbalVariableType) {
                if (this.localVariable) {
                    // 编辑态
                    // 如果被引用的主机变量被删除，则将执行目标的值重置为空
                    // 主机变量被删除
                    if (!this.variable.find(_ => _.name === this.localVariable)) {
                        setTimeout(() => {
                            this.handleVariableChange('');
                        });
                    }
                } else {
                    // 主机变量为空，默认选中第一个
                    if (this.variable.length > 0) {
                        setTimeout(() => {
                            this.handleVariableChange(this.variable[0].name);
                        });
                    }
                }
            }
            
            this.rules = [
                {
                    validator: () => {
                        if (this.isGolbalVariableType) {
                            return Boolean(this.localVariable);
                        }
                        return !TaskHostNodeModel.isHostNodeInfoEmpty(this.localHost);
                    },
                    message: I18n.t('目标服务器必填'),
                    trigger: 'blur',
                },
            ];
        },
        methods: {
            /**
             * @desc 执行目标值更新
             */
            triggerChange () {
                const taskHostNode = new TaskHostNodeModel({});
                if (this.isGolbalVariableType) {
                    taskHostNode.variable = this.localVariable;
                } else {
                    taskHostNode.hostNodeInfo = this.localHost;
                }
                if (!taskHostNode.isEmpty) {
                    this.$refs.targetServerRef.clearValidator();
                }
                this.$emit('on-change', Object.freeze(taskHostNode));
            },
            /**
             * @desc 执行目标类型改变
             */
            handleTargetTypeChange (value) {
                this.targetType = value;
                this.triggerChange();
            },
            /**
             * @desc 弹出ip选择器弹层
             */
            handleShowChooseIp () {
                this.isShowChooseIp = true;
                this.searchText = '';
            },
            /**
             * @desc 选择全局变量
             * @param {String} value 全局变量名
             */
            handleVariableChange (value) {
                this.localVariable = value;
                this.triggerChange();
            },
            /**
             * @desc 主机值更新
             * @param {Object} hostNodeInfo 主机信息
             */
            handleHostChange (hostNodeInfo) {
                this.localHost = Object.freeze(hostNodeInfo);
                this.triggerChange();
                setTimeout(() => {
                    this.$refs.actionBox.scrollIntoView();
                }, 500);
            },
            handleCloseIpSelector () {
                this.isShowChooseIp = false;
            },
            /**
             * @desc 复制所有主机
             */
            handleCopyAll () {
                const allIP = this.$refs.ipSelector.getHostIpList();
                if (allIP.length < 1) {
                    this.messageWarn(I18n.t('你还未选择主机'));
                    return;
                }
                
                execCopy(allIP.join('\n'), `${I18n.t('复制成功')}（${allIP.length}${I18n.t('个IP')}）`);
            },
            /**
             * @desc 复制所有异常主机
             */
            handleCopyFail () {
                const abnormalHostIpList = this.$refs.ipSelector.getNotAlivelHostIpList();
                if (abnormalHostIpList.length < 1) {
                    this.messageWarn(I18n.t('暂无异常主机'));
                    return;
                }
                
                execCopy(abnormalHostIpList.join('\n'), `${I18n.t('复制成功')}（${abnormalHostIpList.length}${I18n.t('个IP')}）`);
            },
            /**
             * @desc 复制所有主机数据
             */
            handleClearAll () {
                const { hostNodeInfo } = new TaskHostNodeModel({});
                this.localHost = Object.freeze(hostNodeInfo);
                this.messageSuccess(I18n.t('清空成功'));
                this.triggerChange();
            },
            /**
             * @desc 刷新所有主机的状态信息
             */
            handleRefreshHost () {
                this.$refs.ipSelector.refresh();
            },
            /**
             * @desc 筛选主机
             * @param {String} search 筛选值
             */
            handleHostSearch (search) {
                this.searchText = _.trim(search);
                // this.searchData = Object.freeze(this.$refs.serverPanel.getAllHost(search));
            },
        },
    };
</script>
<style lang='postcss'>
    html[lang="en-US"] {
        .compose-form-item {
            .server-global-variable-select {
                width: 341px;
            }
        }

        .ip-search {
            width: 162px;
        }

        .only-host {
            .ip-search {
                width: 314px;
            }
        }
    }

    .task-step-execute-target {
        &.only-host {
            .ip-search {
                width: 620px;
            }
        }

        .bk-button {
            font-size: 14px !important;
        }

        .action-flag {
            font-size: 18px;
        }

        .view-server-panel {
            margin-top: 14px;
        }

        .ip-search {
            flex: 0 0 auto;
            width: 245px;
            margin-left: auto;
        }

        .compose-form-item {
            .server-global-variable-select {
                width: 376px;
            }
        }
    }

    .execute-target-host-clear {
        user-select: none;

        .disabled {
            a {
                color: #c4c6cc !important;
                cursor: not-allowed;
                background-color: #fafafa !important;
            }
        }
    }
</style>
