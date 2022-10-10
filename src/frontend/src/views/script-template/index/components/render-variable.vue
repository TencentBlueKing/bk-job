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
    <resizeable-box>
        <div
            v-bkloading="{ isLoading }"
            class="script-template-render-variable">
            <scroll-faker theme="dark">
                <div class="title">
                    {{ $t('scriptTemplate.变量列表') }}
                </div>
                <table class="script-variable-list">
                    <thead>
                        <tr>
                            <td>{{ $t('scriptTemplate.变量名称') }}</td>
                            <td>{{ $t('scriptTemplate.含义') }}</td>
                            <td>{{ $t('scriptTemplate.示例') }}</td>
                        </tr>
                    </thead>
                    <tbody
                        v-for="item in variableList"
                        :key="item.name">
                        <tr>
                            <td>
                                <div
                                    class="variable-name"
                                    @click="handleCopyScriptVariable(`{{${item.name}}}`)">
                                    {{ item.name }}
                                </div>
                            </td>
                            <td>{{ item.description }}</td>
                            <td>{{ item.demo }}</td>
                        </tr>
                    </tbody>
                </table>
            </scroll-faker>
        </div>
    </resizeable-box>
</template>
<script>
    import ScriptTemplateService from '@service/script-template';

    import { execCopy } from '@utils/assist';

    import ResizeableBox from './resizeable-box';

    import I18n from '@/i18n';

    export default {
        components: {
            ResizeableBox,
        },
        inheritAttrs: false,
        data () {
            return {
                isLoading: false,
                variableList: [],
            };
        },
        created () {
            this.fetchVariableList();
        },
        methods: {
            /**
             * @desc 获取变量列表
             */
            fetchVariableList () {
                this.isLoading = true;
                ScriptTemplateService.fetchVaribaleList()
                    .then((data) => {
                        this.variableList = Object.freeze(data);
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 复制变量（变量名被 {{}} 包裹）
             */
            handleCopyScriptVariable (variableName) {
                execCopy(variableName, `${I18n.t('scriptTemplate.复制成功')} ${variableName}`);
            },
        },
    };
</script>
<style lang='postcss'>
    .script-template-render-variable {
        height: 100%;
        padding: 16px 20px;
        white-space: nowrap;
        background: #292929;

        .script-variable-list {
            width: 100%;
            margin-top: 16px;
            font-size: 12px;

            thead {
                background: #3a3b3d;
            }

            td {
                height: 32px;
                padding: 0 16px;
            }

            tbody {
                td {
                    border-bottom: 1px solid #4a4a4a;
                }
            }

            .variable-name {
                height: 24px;
                padding-left: 10px;
                margin-left: -10px;
                line-height: 24px;
                cursor: pointer;

                &:hover {
                    background: #3b3c3d;
                }
            }
        }
    }
</style>
