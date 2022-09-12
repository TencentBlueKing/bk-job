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
    <jb-form-item
        :label="$t('传输模式')"
        required>
        <div class="form-item-content">
            <div class="file-step-transfer-mode-wraper">
                <bk-radio-group
                    class="radio-check"
                    :value="formData[field]"
                    @change="handleChange">
                    <bk-radio
                        v-bk-tooltips="constraintTips"
                        :value="2">
                        {{ $t('强制模式') }}
                    </bk-radio>
                    <bk-radio
                        v-bk-tooltips="strictTips"
                        :value="1">
                        {{ $t('严谨模式') }}
                    </bk-radio>
                </bk-radio-group>
            </div>
        </div>
    </jb-form-item>
</template>
<script>
    import I18n from '@/i18n';

    export default {
        name: '',
        props: {
            field: {
                type: String,
                required: true,
            },
            formData: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                radioValue: 2,
            };
        },
        computed: {
            // radio 选择框的值是-1表示是保险模式
            // 保险模式有两种类型可选
            showSelectCheck () {
                return this.radioValue === -1;
            },
        },
        watch: {
            formData: {
                // 值是3、4归类为保险模式
                handler  (formData) {
                    const transferMode = parseInt(formData[this.field], 10);
                    if ([
                        3,
                        4,
                    ].includes(transferMode)) {
                        this.radioValue = -1;
                    } else {
                        this.radioValue = transferMode;
                    }
                },
                immediate: true,
                deep: true,
            },
        },
        created () {
            this.strictTips = {
                content: I18n.t('严谨判断目标路径是否存在，若不存在将直接终止任务。'),
                width: 180,
            };
            this.constraintTips = {
                content: I18n.t('不论目标路径是否存在，都将强制按照用户填写的目标路径进行传输（路径不存在会自动创建）。'),
                width: 210,
            };
            this.safeTips = {
                content: I18n.t('避免因源或目标机器有同名文件时被覆盖，该模式下将按用户选择的规则在目标路径后面追加创建相应的文件夹。'),
                width: 210,
            };
        },
        methods: {
            handleChange (transferMode) {
                const realValue = transferMode === -1 ? 3 : transferMode;
                this.$emit('on-change', this.field, realValue);
            },
            handleSelectChange (transferMode) {
                this.$emit('on-change', this.field, transferMode);
            },
        },
    };
</script>
<style lang="postcss">
    .file-step-transfer-mode-wraper {
        display: flex;
        align-items: center;
        height: 32px;

        .radio-check {
            flex: 0 0 auto;
            width: auto;

            .bk-form-radio {
                &:nth-child(n+2) {
                    margin-left: 32px;
                }
            }

            .bk-radio-text {
                border-bottom: 1px dashed #c4c6cc;
            }
        }

        .select-check {
            flex: 1;
            min-width: 295px;
            margin-left: 12px;
        }
    }
</style>
