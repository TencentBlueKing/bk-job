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
    <div class="jb-edit-textarea">
        <div v-if="!isEditing" class="value-text-wraper">
            <div
                ref="textWraper"
                class="render-text-box"
                :style="boxStyles">
                <slot v-bind:value="newVal">{{ renderText }}</slot>
                <span v-if="renderLength > 0" class="text-whole" @click="handleExpandAll">
                    <template v-if="isExpand">
                        <Icon type="angle-double-up" style="font-size: 12px;" />
                        <span>收起</span>
                    </template>
                    <template v-else>
                        <Icon type="angle-double-down" style="font-size: 12px;" />
                        <span>展开</span>
                    </template>
                </span>
            </div>
            <div v-if="!readonly" class="edit-action-box">
                <Icon v-if="!isSubmiting" type="edit-2" class="edit-action" @click.self.stop="handleShowInput" />
                <Icon v-if="isSubmiting" type="loading-circle" class="edit-loading" />
            </div>
        </div>
        <div v-else @click.stop="">
            <jb-textarea
                v-model="newVal"
                class="edit-value-container"
                ref="input"
                :rows="rows"
                v-bind="$attrs"
                @blur="handleInputBlur" />
        </div>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import JbTextarea from '@components/jb-textarea';

    export default {
        name: 'JbEditTextarea',
        components: {
            JbTextarea,
        },
        props: {
            field: {
                type: String,
                required: true,
            },
            value: {
                type: String,
                default: '',
            },
            // true 时值的内容一行展示
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
             * @desc 计算默认展示的文本宽度
             *
             * settimeout 保证计算过程在组件渲染之后
             */
            calcEllTextLength () {
                if (this.isExpand) {
                    return;
                }
                setTimeout(() => {
                    const valLength = this.newVal.length;
                    const $el = document.createElement('div');
                    $el.style.position = 'absolute';
                    $el.style.top = 0;
                    $el.style.right = 0;
                    $el.style.width = '100%';
                    $el.style.opacity = 0;
                    $el.style.zIndex = '-1';
                    this.$refs.textWraper.appendChild($el);

                    const lineHeight = 26;
                    const maxLine = 3;
                    const maxHeight = lineHeight * maxLine;
                    let realHeight = 0;
                    let realLength = 1;
                
                    const calcLength = () => {
                        const text = this.newVal.slice(0, realLength);
                        $el.innerText = text;
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
                            this.renderLength = realLength - 11;
                        }
                        this.$refs.textWraper.removeChild($el);
                    });
                });
            },
            handleExpandAll () {
                this.isExpand = !this.isExpand;
            },
        },
    };
</script>
<style lang="postcss">
    @keyframes textarea-edit-loading {
        to {
            transform: rotateZ(360deg);
        }
    }
</style>
<style lang='postcss' scoped>
    .jb-edit-textarea {
        position: relative;

        .value-text-wraper {
            position: relative;
            display: flex;
            align-items: center;
            min-width: 30px;
            min-height: 30px;
            padding: 3px 0;

            &:hover {
                .edit-action {
                    opacity: 1;
                    transform: scale(1);
                }
            }

            .text-whole {
                color: #3a84ff;
                cursor: pointer;
            }
        }

        .render-text-box {
            position: relative;
            max-width: calc(100% - 25px);
            overflow: hidden;
            line-height: 26px;
            white-space: pre-wrap;
            flex: 0 0 auto;
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
                top: 8px;
                margin-left: 2px;
                animation: 'textarea-edit-loading' 1s linear infinite;
            }
        }

        .edit-value-container {
            width: 100%;
        }
    }
</style>
