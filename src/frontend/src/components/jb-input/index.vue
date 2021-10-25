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
    <div class="jb-input" ref="jbInput">
        <bk-input
            ref="input"
            :value="localValue"
            @input="handleInput"
            @blur="handleBlur"
            @keyup="handleKeyup"
            v-bind="$attrs" />
        <span
            v-if="maxlength > 0"
            ref="number"
            class="jb-input-number">
            {{ inputLength }}/{{ maxlength }}
        </span>
    </div>
</template>
<script>
    export default {
        name: '',
        model: {
            prop: 'value',
            event: 'input',
        },
        props: {
            value: {
                type: [String, Number],
                default: '',
            },
            maxlength: {
                type: Number,
                default: 0,
            },
            enterTrigger: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                inputLength: 0,
                localValue: '',
            };
        },
        watch: {
            value: {
                handler (value) {
                    this.localValue = value;
                    this.inputLength = value.length;
                },
                immediate: true,
            },
            inputLength () {
                this.init();
            },
        },
        mounted () {
            this.timer = '';
            this.inputHander = this.$refs.jbInput.querySelector('.bk-form-input');
            this.init();
        },
        methods: {
            init () {
                if (!this.$refs.number || this.maxlength < 1) {
                    return;
                }
                this.$nextTick(() => {
                    const numberText = this.$refs.number.outerText;
                    this.inputHander.style.paddingRight = `${numberText.length - 1}em`;
                });
            },
            triggerChange (value) {
                this.$emit('input', value);
                this.$emit('change', value);
            },
            handleBlur () {
                setTimeout(() => {
                    this.$refs.input.setCurValue(this.localValue);
                });
            },
            handleKeyup (value, event) {
                if (event.isComposing || !this.enterTrigger) {
                    // 跳过输入法复合事件
                    return;
                }
                // 输入框的值被清空直接触发搜索
                // enter键开始搜索
                if ((value === '' && value !== this.value)
                    || event.keyCode === 13) {
                    setTimeout(() => {
                        this.triggerChange(this.inputHander.value);
                    });
                }
            },
            handleInput (str) {
                let value = str.trim();
                if (this.maxlength > 0 && value.length > this.maxlength) {
                    value = value.slice(0, this.maxlength);
                    this.$nextTick(() => {
                        this.inputHander.value = value;
                        this.$refs.input.setCurValue(value);
                    });
                }
                this.inputLength = value.length;
                this.localValue = value;
                if (!this.enterTrigger) {
                    this.triggerChange(value);
                }
            },
        },
    };
</script>
<style lang='postcss'>
    .jb-input {
        position: relative;

        .jb-input-number {
            position: absolute;
            top: 50%;
            right: 9px;
            font-size: 12px;
            color: #979ba5;
            transform: translateY(-50%);
        }
    }
</style>
