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
        ref="editBox"
        class="edit-source-file"
        :class="{ 'path-error': !!error }"
        :data-error="error">
        <template v-if="mode === 'edit'">
            <div
                class="path-text"
                :style="styles"
                @click="handleShowEdit">
                <bk-popover placement="right">
                    <div
                        v-for="(item, index) in displayRows"
                        :key="index"
                        class="path-text-row">
                        {{ item }}
                    </div>
                    <div v-if="hasMore">
                        ...
                    </div>
                    <ul
                        slot="content"
                        class="source-file-tips-box">
                        <li
                            v-for="(item, index) in renderValue"
                            :key="index"
                            class="row">
                            <span class="dot" />
                            {{ item }}
                        </li>
                    </ul>
                </bk-popover>
            </div>
            <smart-input
                v-if="showEdit"
                ref="pathEdit"
                class="file-path-edit"
                :placeholder="$t('Enter 换行可输入多个路径')"
                :value="tempValue"
                @blur="handleEditSubmit"
                @input="handleEditChange" />
        </template>
        <template v-if="mode === 'input'">
            <smart-input
                ref="pathSubmit"
                class="file-path-create"
                :placeholder="$t('Enter 换行可输入多个路径')"
                @blur="handleCreteaBlur"
                @input="handleCreateSubmit" />
        </template>
        <div
            v-if="error"
            v-bk-tooltips="error"
            class="error-flag">
            <Icon type="info" />
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import {
        filePathRule,
    } from '@utils/validator';

    import SmartInput from '@components/smart-input';

    import I18n from '@/i18n';

    const formatValue = str => str.split('\n').reduce((result, item) => {
        const realValue = _.trim(item);
        if (realValue) {
            result.push(realValue);
        }
        return result;
    }, []);

    const DISPLAY_ROW_NUMS = 3;

    export default {
        name: '',

        components: {
            SmartInput,
        },
        props: {
            value: {
                type: Array,
                default: () => [],
            },
            mode: {
                type: String,
                default: 'edit',
            },
        },
        data () {
            return {
                showEdit: false,
                isError: false,
                renderValue: [],
                tempValue: '',
                error: '',
            };
        },
        computed: {
            displayRows () {
                return this.renderValue.slice(0, DISPLAY_ROW_NUMS);
            },
            hasMore () {
                return this.renderValue.length > DISPLAY_ROW_NUMS;
            },
            styles () {
                if (this.showEdit) {
                    return {
                        visibility: 'hidden',
                    };
                }
                return {
                    visibility: 'visible',
                };
            },
        },
        watch: {
            value: {
                handler  (newValue) {
                    this.renderValue = newValue;
                    this.tempValue = newValue.join('\n');
                },
                immediate: true,
            },
        },
        methods: {
            handleShowEdit () {
                this.showEdit = true;
                this.$nextTick(() => {
                    this.$refs.pathEdit.focus();
                });
            },
            handleEditChange (value) {
                this.error = '';
                if (!value) {
                    this.error = I18n.t('路径不能为空');
                }
                this.tempValue = value;
            },
            handleEditSubmit () {
                if (this.error) {
                    this.$refs.pathEdit.focused = true;
                    return;
                }
                
                const realValue = formatValue(this.tempValue);
                
                const hasError = !realValue.every(item => filePathRule.validator(item));
                if (hasError) {
                    this.error = I18n.t('路径格式不正确');
                    this.showEdit = true;
                    this.$refs.pathEdit.focused = true;
                    return;
                }
                this.showEdit = false;
                this.renderValue = realValue;
                this.$emit('on-change', realValue);
            },
            handleCreateSubmit (value) {
                this.tempValue = value;
                const realValue = formatValue(this.tempValue);
                const isError = !realValue.every(item => filePathRule.validator(item));
                if (isError) {
                    this.error = I18n.t('路径格式不正确');
                    return;
                }
                this.error = '';
                this.renderValue = realValue;
                this.$emit('on-change', realValue);
            },
            handleCreteaBlur () {
                if (this.error) {
                    this.$refs.pathSubmit.focused = true;
                }
            },
        },
    };
</script>
<style lang='postcss'>
    @import "@/css/mixins/scroll";

    .edit-source-file {
        position: relative;
        display: flex;
        width: 100%;
        margin-left: -10px;
        font-size: 12px;
        border-radius: 2px;

        &.path-error {
            .file-path-edit,
            .file-path-create {
                .job-smart-input-area {
                    border-color: #ea3636;
                }
            }
        }

        .path-text {
            width: 100%;
            padding: 6px 10px;
            cursor: pointer;
            border: 1px solid transparent !important;
            border-radius: 2px;

            &:hover {
                background: #f0f1f5;
            }

            .bk-tooltip,
            .bk-tooltip-ref {
                display: flex;
                flex-direction: column;
            }

            .path-text-row {
                width: 100%;
                height: 18px;
                max-width: 100%;
                overflow: hidden;
                line-height: 18px;
                text-overflow: ellipsis;
                word-break: keep-all;
                white-space: normal;
            }
        }

        .file-path-edit,
        .file-path-create {
            width: 100%;

            .job-smart-input-area {
                min-height: 30px;
                border-color: transparent;
            }
        }

        .file-path-edit {
            position: absolute;
            top: 0;
            left: 0;
        }

        .file-path-create {
            background: #f7f8fa;

            &:hover {
                background: #f0f1f5;
            }
        }

        .error-flag {
            position: absolute;
            top: 0;
            right: 0;
            z-index: 1999;
            display: flex;
            align-items: center;
            height: 30px;
            padding: 0 6px;
            font-size: 16px;
            color: #ea3636;
            cursor: pointer;
        }
    }

    .source-file-tips-box {
        max-width: 300px;
        max-height: 280px;
        min-width: 60px;
        overflow-y: auto;

        @mixin scroller;

        .row {
            word-break: break-all;
        }

        .dot {
            display: inline-block;
            width: 6px;
            height: 6px;
            background: currentcolor;
            border-radius: 50%;
        }
    }
</style>
