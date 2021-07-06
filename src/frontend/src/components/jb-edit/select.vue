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
    <div class="jb-edit-select" :class="mode" :key="value">
        <template v-if="!isEditing">
            <div class="render-value-box" @click.stop="handleBlockShowEdit">
                <div class="value-text" v-bk-overflow-tips>
                    <slot v-bind:value="renderText">
                        <span>{{ renderText }}</span>
                    </slot>
                </div>
                <i class="bk-icon icon-angle-down value-arrow" />
                <div class="edit-action-box">
                    <Icon
                        v-if="!isBlock && !isSubmiting"
                        type="edit-2"
                        class="edit-action"
                        @click.self.stop="handleShowEdit" />
                    <Icon
                        v-if="isSubmiting"
                        type="loading-circle"
                        class="edit-loading" />
                </div>
            </div>
        </template>
        <template v-else>
            <div
                class="edit-value-box"
                :class="{ 'edit-error': !!error }"
                @click.stop="">
                <bk-select
                    ref="select"
                    :value="value"
                    @change="handleSelectChange"
                    @toggle="handleSelectToggle">
                    <bk-option
                        v-for="item in list"
                        :id="item.id"
                        :name="item.name"
                        :key="item.id" />
                </bk-select>
                <div v-if="error" class="input-edit-info" v-bk-tooltips.top="error">
                    <Icon type="info" />
                </div>
            </div>
        </template>
    </div>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';

    export default {
        name: 'JbEditSelect',
        props: {
            /**
             * @value block 块级交互
             * @value ‘’ 默认鼠标点击编辑按钮
             */
            mode: {
                type: String,
                default: '',
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
                type: [String, Number],
                default: '',
            },
            /**
             * @desc 下拉数据列表
             */
            list: {
                type: Array,
                require: true,
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
                error: '',
                isEditing: false,
                isSubmiting: false,
            };
        },
        computed: {
            /**
             * @desc 非编辑状态时
             * @returns { Boolean }
             */
            renderText () {
                const item = _.find(this.list, ({ id }) => this.localValue === id);
                if (!item) {
                    return '--';
                }
                return item.name;
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
            value (localValue) {
                this.localValue = localValue;
            },
        },
        mounted () {
            this.isValidatoring = false;
            document.body.addEventListener('click', this.handleHideEdit);
            this.$once('hook:beforeDestroy', () => {
                document.body.removeEventListener('click', this.handleHideEdit);
            });
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
                this.isEditing = true;
                this.$nextTick(() => {
                    this.$refs.select.$el.querySelector('.bk-select-name').click();
                });
            },
            /**
             * @desc 隐藏 input 框
             * @param {Object} event dom 事件
             */
            handleHideEdit (event) {
                if (this.isValidatoring || this.error) {
                    return;
                }
                if (event.path && event.path.length > 0) {
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < event.path.length; i++) {
                        const target = event.path[i];
                        if (target.className === 'jb-edit-select') {
                            return;
                        }
                    }
                }
                this.isEditing = false;
            },
            /**
             * @desc input 值更新
             * @param {String} value 最新输入值
             */
            handleSelectChange (value) {
                this.localValue = value;
                this.triggerChange();
            },
            /**
             * @desc 下拉面板收起，取消编辑状态
             */
            handleSelectToggle (toggle) {
                if (!toggle) {
                    this.isEditing = false;
                }
            },
            
        },
    };
</script>
<style lang='postcss'>
    .jb-edit-select {
        &.block {
            position: relative;
            cursor: pointer;

            .render-value-box {
                padding-left: 10px;
                margin-left: -10px;

                &:hover {
                    background: #f0f1f5;

                    .value-text {
                        max-width: calc(100% - 32px);
                    }
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
            height: 30px;
            min-width: 36px;
            min-height: 28px;

            .value-arrow {
                position: absolute;
                top: 2px;
                right: 2px;
                display: none;
                font-size: 22px;
                color: #979ba5;
            }

            &:hover {
                .edit-action {
                    opacity: 1;
                    transform: scale(1);
                }

                .value-arrow {
                    display: block;
                }
            }
        }

        .value-text {
            overflow: hidden;
            line-height: 30px;
            text-overflow: ellipsis;
            white-space: nowrap;
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
                opacity: 0;
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

        .edit-value-box {
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
