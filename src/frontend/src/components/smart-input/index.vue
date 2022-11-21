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
    <div class="job-smart-input">
        <div
            ref="input"
            class="job-smart-input-area"
            contenteditable="true"
            spellcheck="false"
            :style="stylees"
            @blur="handleBlur"
            @focus="handleFocus"
            @input="handleInput"
            @paste="handlePaste" />
        <div
            v-if="showPlaceholder"
            class="job-smart-input-placeholder"
            @click="handleInputFocus">
            {{ placeholder }}
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            value: {
                type: String,
                default: '',
            },
            placeholder: String,
            resize: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                localValue: this.value,
                focused: false,
            };
        },
        computed: {
            stylees () {
                const styles = {};
                if (this.focused) {
                    styles['z-index'] = 1999;
                } else {
                    styles.height = '30px';
                    styles.overflow = 'hidden';
                    styles['white-space'] = 'pre';
                    styles['text-overflow'] = 'ellipsis';
                }
                return styles;
            },
            showPlaceholder () {
                if (this.focused) {
                    return false;
                }
                return !this.localValue;
            },
        },
        mounted () {
            this.init();
        },
        methods: {
            init () {
                this.$refs.input.innerText = this.value;
            },
            focus () {
                this.$refs.input.focus();
            },
            handleInputFocus () {
                this.focus();
                setTimeout(() => {
                    this.$refs.input.selectionStart = this.localValue.length;
                    this.$refs.input.selectionEnd = this.localValue.length;
                });
            },
            handleInput (event) {
                const value = event.target.outerText;
                this.localValue = value.trim();
                this.$emit('input', this.localValue);
                this.$emit('change', this.localValue);
            },
            handleFocus () {
                this.focused = true;
            },
            handleBlur () {
                this.focused = false;
                this.$refs.input.scrollTop = 0;
                this.$emit('blur', this.localValue);
            },
            handlePaste (e) {
                e.preventDefault();
                e.stopPropagation();
                let text = '';
                const event = (e.originalEvent || e);
                if (event.clipboardData && event.clipboardData.getData) {
                    text = event.clipboardData.getData('text/plain');
                } else if (window.clipboardData && window.clipboardData.getData) {
                    text = window.clipboardData.getData('Text');
                }
                if (document.queryCommandSupported('insertText')) {
                    document.execCommand('insertText', false, text);
                } else {
                    document.execCommand('paste', false, text);
                }
            },
        },
    };
</script>
<style lang='postcss'>
    @import "@/css/mixins/scroll";

    .job-smart-input {
        position: relative;
        height: 30px;
        padding: 0 10px;
        font-size: 12px;
        line-height: 18px;
        word-break: break-all;
        cursor: pointer;
        background: #fff;

        .job-smart-input-area {
            @mixin scroller;

            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            max-height: 300px;
            min-height: 32px;
            padding: 6px 10px;
            overflow-y: scroll;
            font-size: 12px;
            color: #63656e;
            background: inherit;
            border: 1px solid #c4c6cc;
            border-radius: 2px;
            outline: none;

            &:focus {
                background: #fff !important;
                border: 1px solid #3a84ff !important;
            }
        }

        .job-smart-input-placeholder {
            position: absolute;
            top: 1px;
            right: 1px;
            bottom: 1px;
            left: 1px;
            height: 30px;
            padding: 6px 10px;
            overflow: hidden;
            color: #c4c6cc;
            text-overflow: ellipsis;
            white-space: nowrap;
            background: transparent;
        }
    }
</style>
