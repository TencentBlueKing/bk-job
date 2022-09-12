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
        class="jb-edit-textarea"
        :class="mode">
        <div
            v-if="!isEditing"
            class="render-value-box"
            @click.stop="handleBlockShowEdit">
            <div
                ref="valueTextBox"
                class="render-text-box"
                :style="boxStyles"
                @copy="handleCopy">
                <slot :value="newVal">
                    {{ renderText }}
                </slot>
                <span
                    v-if="isShowMore"
                    class="text-whole"
                    @click.stop="handleExpandAll">
                    <template v-if="isExpand">
                        <Icon
                            style="font-size: 12px;"
                            type="angle-double-up" />
                        <span>收起</span>
                    </template>
                    <template v-else>
                        <Icon
                            style="font-size: 12px;"
                            type="angle-double-down" />
                        <span>展开</span>
                    </template>
                </span>
            </div>
            <div
                v-if="!readonly"
                class="edit-action-box">
                <Icon
                    v-if="!isBlock && !isSubmiting"
                    class="edit-action"
                    type="edit-2"
                    @click.self.stop="handleShowInput" />
                <Icon
                    v-if="isSubmiting"
                    class="edit-loading"
                    type="loading-circle" />
            </div>
        </div>
        <div
            v-else
            @click.stop="">
            <jb-textarea
                ref="input"
                v-model="newVal"
                class="edit-value-box"
                :rows="rows"
                v-bind="$attrs"
                @blur="handleInputBlur" />
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import JbTextarea from '@components/jb-textarea';

    import I18n from '@/i18n';

    export default {
        name: 'JbEditTextarea',
        components: {
            JbTextarea,
        },
        props: {
            /**
             * @value block 块级交互
             * @value ‘’ 默认鼠标点击编辑按钮
             */
            mode: {
                type: String,
                default: '',
            },
            field: {
                type: String,
                required: true,
            },
            value: {
                type: String,
                default: '',
            },
            // 忽略换行强制文本在一行显示
            singleRowRender: {
                type: Boolean,
                default: false,
            },
            rows: {
                type: Number,
                default: 3,
            },
            remoteHander: {
                type: Function,
                default: () => Promise.resolve(),
            },
            rules: {
                type: Array,
                default: () => [],
            },
            readonly: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                isEditing: false,
                isSubmiting: false,
                isExpand: false,
                newVal: this.value,
                renderLength: 0,
            };
        },
        computed: {
            renderText () {
                if (this.newVal.length === this.renderLength) {
                    return this.newVal;
                }
                if (this.isExpand) {
                    return this.newVal;
                }
                if (this.renderLength < 1) {
                    if (!this.newVal) {
                        return '--';
                    }
                    return this.newVal;
                }
                return `${this.newVal.slice(0, this.renderLength)}...`;
            },
            isShowMore () {
                return this.newVal.length > this.renderLength && this.renderLength > 0;
            },
            boxStyles () {
                const styles = {
                    'max-height': this.isExpand ? 'unset' : '78px',
                };
                if (this.singleRowRender) {
                    styles.height = '26px';
                    styles['max-width'] = '100%';
                    styles['text-overflow'] = 'ellipsis';
                    styles['white-space'] = 'nowrap';
                }
                return styles;
            },
            isBlock () {
                return this.mode === 'block';
            },
        },
        watch: {
            value: {
                handler (newVal) {
                    this.newVal = newVal;
                    if (this.newVal) {
                        this.calcEllTextLength();
                    }
                },
                immediate: true,
            },
        },
        mounted () {
            document.body.addEventListener('click', this.handleHideInput);
            this.$once('hook:beforeDestroy', () => {
                document.body.removeEventListener('click', this.handleHideInput);
            });
        },
        methods: {
            /**
             * @desc 计算默认展示的文本宽度
             *
             * settimeout 保证计算过程在组件渲染之后
             */
            calcEllTextLength () {
                if (this.$slots.default) {
                    return;
                }
                if (this.isExpand) {
                    return;
                }
                setTimeout(() => {
                    const valLength = this.newVal.length;
                    const $el = document.createElement('div');
                    $el.style.opacity = 0;
                    $el.style.zIndex = '-1';
                    if (this.singleRowRender) {
                        $el.style.wordBreak = 'keep-all';
                        $el.style.whiteSpace = 'pre';
                    }
                    
                    this.$refs.valueTextBox.appendChild($el);

                    const lineHeight = 24;
                    const maxLine = 3;
                    const maxHeight = lineHeight * maxLine;
                    let realHeight = 0;
                    let realLength = 1;
                
                    const calcLength = () => {
                        const text = this.newVal.slice(0, realLength);
                        $el.innerText = `${text} 展开展开`;
                        Promise.resolve()
                            .then(() => {
                                realHeight = $el.getBoundingClientRect().height;
                                if (realHeight <= maxHeight && realLength < valLength) {
                                    realLength += 2;
                                    calcLength();
                                }
                            });
                    };
                    calcLength();
                    setTimeout(() => {
                        this.renderLength = 0;
                        if (realHeight > lineHeight) {
                            this.renderLength = realLength >= valLength ? valLength : realLength - 4;
                        }
                        this.$refs.valueTextBox.removeChild($el);
                    });
                });
            },
            /**
             * @desc 切换编辑状态
             */
            handleBlockShowEdit () {
                if (!this.isBlock) {
                    return;
                }
                this.handleShowInput();
            },
            /**
             * @desc 开始编辑
             *
             * 阻止事件的冒泡
             * 手动触发body的 click 事件，
             */
            handleShowInput () {
                document.body.click();
                this.isEditing = true;
                this.$nextTick(() => {
                    this.$refs.input.focus();
                });
            },
            /**
             * @desc 输入框失去焦点
             *
             * 值没有改变不需要提交
             */
            handleInputBlur () {
                this.isEditing = false;
                if (this.newVal === this.value) {
                    return;
                }
                this.isSubmiting = true;
                this.remoteHander({
                    [this.field]: this.newVal,
                }).then(() => {
                    this.$emit('on-change', {
                        [this.field]: this.newVal,
                    });
                    this.calcEllTextLength();
                    this.messageSuccess(I18n.t('编辑成功'));
                })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 退出编辑状态
             */
            handleHideInput (event) {
                if (event.path && event.path.length > 0) {
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < event.path.length; i++) {
                        const target = event.path[i];
                        if (target.className === 'jb-edit-textarea') {
                            return;
                        }
                    }
                }
                this.isEditing = false;
            },
            /**
             * @desc 复制内容
             * @param { Object } event 复制事件
             *
             * 删除内容的开头和结尾的换行符
             */
            handleCopy (event) {
                const clipboardData = event.clipboardData || window.clipboardData;
                if (!clipboardData) {
                    return;
                }
                let text = window.getSelection().toString();
                if (text && text.indexOf(this.renderText) > -1) {
                    text = this.value;
                }
                if (text) {
                    event.preventDefault();
                    clipboardData.setData('text/plain', _.trim(text, '\n'));
                }
            },
            /**
             * @desc 查看态的文本展开收起
             */
            handleExpandAll () {
                this.isExpand = !this.isExpand;
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .jb-edit-textarea {
        position: relative;

        &.block {
            position: relative;
            cursor: pointer;

            .render-value-box,
            .edit-value-box {
                margin-left: -10px;
            }

            .render-value-box {
                padding-left: 10px;

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
            align-items: center;
            min-width: 30px;
            min-height: 30px;
            padding: 3px 0;

            &:hover {
                .edit-action {
                    opacity: 100%;
                    transform: scale(1);
                }
            }

            .text-whole {
                color: #3a84ff;
                white-space: nowrap;
                cursor: pointer;
                user-select: none;
            }
        }

        .render-text-box {
            position: relative;
            overflow: hidden;
            line-height: 24px;
            white-space: pre-wrap;
        }

        .edit-action-box {
            display: flex;
            align-items: center;
            align-self: flex-start;
            height: 26px;
            margin-right: auto;
            font-size: 16px;

            .edit-action {
                padding: 4px 15px 4px 2px;
                color: #979ba5;
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
                top: 8px;
                margin-left: 2px;
                animation: rotate-loading 1s linear infinite;
            }
        }

        .edit-value-box {
            width: 100%;
        }
    }
</style>
