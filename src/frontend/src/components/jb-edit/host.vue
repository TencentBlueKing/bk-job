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
        class="jb-edit-host"
        :class="mode">
        <div
            class="render-value-box"
            @click.stop="handleBlockShowEdit">
            <div class="value-text">
                <slot :value="localValue">
                    <div
                        style="margin-left: -4px;"
                        v-html="renderHtml" />
                </slot>
            </div>
            <div class="edit-action-box">
                <Icon
                    v-if="!isBlock && !isSubmiting"
                    class="edit-action"
                    type="edit-2"
                    @click.self.stop="handleShowEdit" />
                <Icon
                    v-if="isSubmiting"
                    class="edit-loading"
                    type="loading-circle" />
            </div>
        </div>
        <ip-selector
            :original-value="originalHostNodeInfo"
            :show-dialog="isShowChooseIp"
            :value="localValue.hostNodeInfo"
            @change="handleHostChange"
            @close-dialog="handleCloseIPSelector" />
        <!-- <choose-ip
            v-model="isShowChooseIp"
            :host-node-info="localValue.hostNodeInfo"
            :original-value
            @on-change="handleHostChange" /> -->
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskHostNodeModel from '@model/task-host-node';

    import I18n from '@/i18n';
    // import ChooseIp from '@components/choose-ip';

    export default {
        name: 'JbEditHost',
        // components: {
        //     ChooseIp,
        // },
        props: {
            /**
             * @value block 块级交互
             * @value ‘’ 默认鼠标点击编辑按钮
             */
            mode: {
                type: String,
                default: 'block',
            },
            /**
             * @desc 编辑操作对应的字段名称
             */
            field: {
                type: String,
                required: true,
            },
            /**
             * @desc 默认值
             */
            value: {
                type: Object,
                default: new TaskHostNodeModel({}),
            },
            /**
             * @desc 宽度
             */
            width: {
                type: String,
                default: 'auto',
            },
            remoteHander: {
                type: Function,
                default: () => Promise.resolve(),
            },
            /**
             * @desc 值验证规则
             */
            rules: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                localValue: this.value,
                isShowChooseIp: false,
                error: '',
                isEditing: false,
                isSubmiting: false,
            };
        },
        computed: {
            renderHtml () {
                if (!this.localValue) {
                    return '--';
                }
                const {
                    dynamicGroupList,
                    hostList,
                    nodeList,
                } = this.localValue.hostNodeInfo || {};
                const strs = [];
                if (hostList.length > 0) {
                    strs.push(`<span class="number strong">${hostList.length}</span>${I18n.t('台主机.result')}`);
                }
                if (nodeList.length > 0) {
                    strs.push(`<span class="number strong">${nodeList.length}</span>${I18n.t('个节点.result')}`);
                }
                if (dynamicGroupList.length > 0) {
                    strs.push(`<span class="number strong">${dynamicGroupList.length}</span>${I18n.t('个分组.result')}`);
                }
                return strs.length > 0 ? strs.join('\n') : '--';
            },
            styles () {
                return {
                    width: this.width,
                };
            },
            isBlock () {
                return this.mode === 'block';
            },
        },
        watch: {
            value: {
                handler (value) {
                    this.localValue = Object.freeze(_.cloneDeep(value));
                },
                immediate: true,
            },
        },
        created () {
            this.originalValue = _.cloneDeep(this.value.hostNodeInfo);
        },
        methods: {
            /**
             * @desc 值验证
             */
            doValidator () {
                const checkValidator = (rule, value) => new Promise((resolve, reject) => {
                    if (rule.required && !value) {
                        reject(rule.message);
                    }
                    // 通过自定义方法来检测
                    if (rule.validator && (typeof rule.validator === 'function')) {
                        const result = rule.validator(value);
                        if (result.then) {
                            result.then((data) => {
                                if (data) {
                                    return resolve();
                                }
                                return reject(rule.message);
                            }).catch(() => {
                                reject(rule.message);
                            });
                        } else if (result) {
                            return resolve();
                        } else {
                            return reject(rule.message);
                        }
                    } else {
                        resolve();
                    }
                });
                
                const allPromise = this.rules.map(rule => checkValidator(rule, this.localValue));
                this.isValidatoring = true;
                return Promise.all(allPromise).finally(() => {
                    this.isValidatoring = false;
                });
            },
            /**
             * @desc 提交编辑
             */
            triggerChange () {
                this.doValidator()
                    .then(() => {
                        this.isEditing = false;
                        if (this.localValue === this.value) {
                            return;
                        }
                        this.isSubmiting = true;
                        this.remoteHander({
                            [this.field]: this.localValue,
                        }).then(() => {
                            this.$emit('on-change', {
                                [this.field]: this.localValue,
                            });
                            this.messageSuccess(I18n.t('编辑成功'));
                        })
                            .catch(() => {
                                this.localValue = this.value;
                            })
                            .finally(() => {
                                this.isSubmiting = false;
                            });
                    })
                    .catch((error) => {
                        this.error = error;
                    });
            },
            handleBlockShowEdit () {
                if (!this.isBlock) {
                    return;
                }
                this.handleShowEdit();
            },
            /**
             * @desc 显示input
             */
            handleShowEdit () {
                document.body.click();
                this.isShowChooseIp = true;
            },
            handleCloseIPSelector () {
                this.isShowChooseIp = false;
            },
            
            handleHostChange (hostNodeInfo) {
                this.localValue = Object.freeze({
                    ...this.localValue,
                    hostNodeInfo,
                });
                this.triggerChange();
            },
        },
    };
</script>
<style lang='postcss'>
    .jb-edit-host {
        &.block {
            position: relative;
            cursor: pointer;

            .render-value-box {
                padding-top: 5px;
                padding-left: 10px;
                margin-left: -10px;

                &:hover {
                    background: #f0f1f5;
                }
            }

            .edit-action-box {
                position: absolute;
                top: 0;
                right: 10px;
                width: 16px;
            }
        }

        .render-value-box {
            position: relative;
            display: flex;
            min-width: 36px;
            min-height: 28px;

            &:hover {
                .edit-action {
                    opacity: 100%;
                    transform: scale(1);
                }
            }
        }

        .value-text {
            line-height: 18px;
            white-space: pre;
        }

        .edit-action-box {
            display: flex;
            align-items: center;
            min-height: 1em;
            margin-right: auto;
            font-size: 16px;
            color: #979ba5;

            .edit-action {
                padding: 6px 0 6px 2px;
                cursor: pointer;
                opacity: 0%;
                transform: scale(0);
                transition: 0.15s;
                transform-origin: left center;

                &:hover {
                    color: #3a84ff;
                }
            }

            .edit-loading {
                position: absolute;
                top: 9px;
                margin-left: 2px;
                animation: rotate-loading 1s linear infinite;
            }
        }

        .edit-value-container {
            position: relative;
            width: 100%;
            font-size: 0;

            &.edit-error {
                .bk-form-input {
                    border-color: #ea3636 !important;
                }
            }

            .input-edit-info {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                z-index: 1;
                display: flex;
                align-items: center;
                padding: 0 10px;
                font-size: 16px;
                color: #ea3636;
            }
        }
    }
</style>
