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
    <jb-form form-type="vertical">
        <jb-form-item
            :desc="nametips"
            :label="$t('template.变量类型')"
            required>
            <bk-radio-group v-model="globalType">
                <bk-radio-button
                    class="item"
                    :disabled="isTypeDisabled"
                    :name="$t('template.字符串')"
                    value="string">
                    {{ $t('template.字符串') }}
                </bk-radio-button>
                <bk-radio-button
                    class="item"
                    :disabled="isTypeDisabled"
                    value="namespace">
                    {{ $t('template.命名空间') }}
                </bk-radio-button>
                <bk-radio-button
                    class="item"
                    :disabled="isTypeDisabled"
                    value="host">
                    {{ $t('template.主机列表') }}
                </bk-radio-button>
                <bk-radio-button
                    class="item"
                    :disabled="isTypeDisabled"
                    value="password">
                    {{ $t('template.密文') }}
                </bk-radio-button>
                <bk-radio-button
                    class="item"
                    :disabled="isTypeDisabled"
                    value="array">
                    {{ $t('template.数组') }}
                </bk-radio-button>
            </bk-radio-group>
        </jb-form-item>
        <component
            :is="globalVarCom"
            ref="handler"
            :data="data"
            :variable="variable" />
    </jb-form>
</template>
<script>
    import VarArray from './array';
    import VarHost from './host';
    import VarNamespace from './namespace';
    import VarPassword from './password';
    import VarString from './string';

    import I18n from '@/i18n';

    export default {
        name: 'GlobalVar',
        components: {
            VarString,
            VarNamespace,
            VarHost,
            VarPassword,
            VarArray,
        },
        props: {
            variable: {
                type: Array,
                default () {
                    return [];
                },
            },
            data: {
                type: Object,
                default () {
                    return {};
                },
            },
        },
        data () {
            return {
                globalType: 'string',
            };
        },
        computed: {
            globalVarCom () {
                const globalVarMap = {
                    string: VarString,
                    namespace: VarNamespace,
                    host: VarHost,
                    password: VarPassword,
                    array: VarArray,
                };
                if (!Object.prototype.hasOwnProperty.call(globalVarMap, this.globalType)) {
                    return 'div';
                }
                return globalVarMap[this.globalType];
            },
            isTypeDisabled () {
                return !!Object.keys(this.data).length;
            },
        },
        watch: {
            data: {
                handler (value) {
                    if (Object.keys(value).length) {
                        this.globalType = value.typeDescription;
                    }
                },
                immediate: true,
            },
        },
        created () {
            this.nametips = {
                theme: 'dark',
                content: I18n.t('template.在步骤参数或脚本内使用 ${变量名} 即可获取到变量值'),
                width: 200,
            };
        },
        methods: {
            submit () {
                return this.$refs.handler.submit()
                    .then((data) => {
                        this.$emit('on-change', data);
                    });
            },
            reset () {
                this.globalType = 1;
                return this.$refs.handler.reset();
            },
        },
    };
</script>
<style lang="postcss">
    .variable-type-wraper {
        .item {
            .bk-radio-button-text {
                width: 108px;
            }
        }
    }
</style>
