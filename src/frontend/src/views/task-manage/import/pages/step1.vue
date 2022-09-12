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
    <div class="task-import-step1">
        <div
            v-once
            class="flag">
            <img src="/static/images/notice.svg">
            <div class="title">
                {{ $t('template.用户须知.title') }}
            </div>
        </div>
        <div
            v-once
            class="tips">
            <p>{{ $t('template.1. 作业内文件分发步骤中带有“本地文件”的会一并导出，没有大小限制！但导入时会受到目标环境的文件上传大小限制，请知悉；') }}</p>
            <p>{{ $t('template.2. 作业内引用的脚本会保留其原始ID，但如果在导入时目标环境找不到对应ID的脚本，将自动转换为「手工录入」的方式导入；') }}</p>
            <p>{{ $t('template.3. 由于导出和导入环境的服务器 IP 地址、人工确认的用户名、通知渠道可能存在差异，请在后续导入后修改对应的信息，以免造成作业不可用的问题；') }}</p>
            <p>{{ $t('template.4. 为了保证步骤信息一致性，作业中使用的「执行账号」会以账号名称导出，并在导入时在目标环境中挑选其中一个相同名称的账号进行保存；如果没有相同的账号名存在，请记得导入后及时更改步骤信息，以免造成作业不可用的问题；') }}</p>
            <p>{{ $t('template.5. 如作业的脚本步骤中使用了「公共函数」，请确保导入的环境中也同样存在对应的函数，以免造成脚本执行逻辑错误；') }}</p>
            <p>{{ $t('template.6. 导出的作业模板会保存其原始ID，如需在导入的环境中完全保留作业ID，请在导入作业的流程中按指引进行设置；') }}</p>
            <p>{{ $t('template.7. 因蓝鲸权限中心的规则中对实例的唯一标识是ID，如在导入时选择保留作业ID，请根据需要选择是否回收原有的权限策略；') }}</p>
            <p>{{ $t('template.8. 在导入时选择保留作业原始ID的前提下，请确保导入的环境中不存在已配置了相同作业ID的定时任务，以免造成影响；') }}</p>
            <p>{{ $t('template.9. 若导入的作业模板或执行方案名称与目标环境的作业存在冲突的情况，会根据导入时设置的"重名后缀"自动在作业名称后面追加命名。') }}</p>
        </div>
        <action-bar>
            <bk-button
                class="mr10"
                @click="handleCancel">
                {{ $t('template.取消') }}
            </bk-button>
            <bk-button
                class="w120"
                theme="primary"
                @click="handleNext">
                {{ $t('template.我知道了') }}
            </bk-button>
        </action-bar>
    </div>
</template>
<script>
    import ActionBar from '../components/action-bar';

    export default {
        name: '',
        components: {
            ActionBar,
        },
        methods: {
            handleCancel () {
                this.$emit('on-cancle');
            },
            handleNext () {
                this.$emit('on-change', 2);
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import "@/css/mixins/media";

    .task-import-step1 {
        display: flex;
        flex-direction: column;
        align-items: center;

        .flag {
            display: flex;
            flex-direction: column;
            padding-top: 70px;
        }

        .title {
            padding-top: 10px;
            font-size: 24px;
            color: #63656e;
            text-align: center;
        }

        .tips {
            width: 700px;
            max-height: calc(100vh - 422px);
            padding: 12px 16px;
            margin-top: 30px;
            overflow-y: scroll;
            font-size: 12px;
            line-height: 20px;
            color: #979ba5;
            background: #fafbfd;
            border: 1px solid #dcdee5;
            border-radius: 2px;

            &::-webkit-scrollbar {
                width: 13px;
            }

            &::-webkit-scrollbar-thumb {
                background-color: #dcdee5;
            }
        }

        @media (--small-viewports) {
            .flag {
                padding-top: 50px;

                img {
                    width: 120px;
                }
            }

            .title {
                font-size: 20px;
            }

            .tips {
                max-height: calc(100vh - 370px);
            }
        }
    }
</style>
