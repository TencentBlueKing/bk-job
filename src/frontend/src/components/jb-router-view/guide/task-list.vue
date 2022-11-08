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
    <div class="task-list-empty-page">
        <div class="page-header">
            <div class="header-wraper">
                <div class="page-title">
                    {{ $t('当前业务暂无作业，请先创建') }}
                </div>
                <div class="page-desc">
                    <p>{{ $t('「作业」是指由脚本执行、文件分发和人工确认组合成的一套操作流程，分为“模板”和“执行方案”；用户需要先创建 “作业模板”，再由该模板生成1个或多个不同步骤组合的 “执行方案”。') }}</p>
                    <p>{{ $t('使用“模板->执行方案”的模式来管理作业的好处，一是可以有效地减少重复的作业步骤，提高步骤复用率；二是可以将场景固化下来，提升操作场景的辨识度和可维护性。') }}</p>
                </div>
                <div class="page-action">
                    <bk-button
                        class="mr10"
                        theme="primary"
                        @click="handleGoTemplateCreate">
                        {{ $t('新建作业') }}
                    </bk-button>
                    <bk-button @click="handleGoImportTemplate">
                        {{ $t('导入作业.guide') }}
                    </bk-button>
                </div>
            </div>
        </div>
        <div class="page-wraper">
            <div class="page-container">
                <div class="feature-item">
                    <div class="feature-pic">
                        <img
                            src="/static/images/guide/task-template.svg"
                            style="width: 223px; margin: 28px 25px 0 22px;">
                    </div>
                    <div class="feature-box">
                        <div class="feature-title">
                            {{ $t('编排作业相关的执行步骤') }}
                        </div>
                        <div>
                            <span>{{ $t('点击') }}</span>
                            <router-link :to="{ name: 'templateCreate' }">
                                {{ $t('新建') }}
                            </router-link>
                            <span>{{ $t('将操作流程所需的步骤，以及一些需要在步骤间传递、或执行时可能需要根据不同需求传入不同值的参数抽离设置为“全局变量”, 编辑保存为一个全新的作业模板。') }}</span>
                        </div>
                    </div>
                </div>
                <div class="divide-line" />
                <div class="feature-item">
                    <div class="feature-pic">
                        <img
                            src="/static/images/guide/task-plan.svg"
                            style="width: 230px; margin: 20px 36px 0 7px;">
                    </div>
                    <div class="feature-box">
                        <div class="feature-title">
                            {{ $t('生成对应的执行方案') }}
                        </div>
                        <div>{{ $t('从已创建的作业模板中，按照操作场景的需要勾选所需的步骤或修改全局变量值，另存为对应的 “执行方案”。') }}</div>
                    </div>
                </div>
            </div>
            <div class="page-link">
                <span>{{ $t('了解更多关于作业模板和执行方案的功能细节，请点击查阅') }}</span>
                <a
                    :href="`${relatedSystemUrls.BK_DOC_CENTER_ROOT_URL}/markdown/作业平台/产品白皮书/Features/Jobs.md`"
                    target="_blank">
                    <span>{{ $t('产品文档') }}</span>
                    <Icon type="link" />
                </a>
            </div>
        </div>
    </div>
</template>
<script>
    import QueryGlobalSettingService from '@service/query-global-setting';
    
    export default {
        data () {
            return {
                relatedSystemUrls: {
                    BK_DOC_CENTER_ROOT_URL: '/',
                },
            };
        },
        created () {
            this.fetchRelatedSystemUrls();
        },
        methods: {
            fetchRelatedSystemUrls () {
                QueryGlobalSettingService.fetchRelatedSystemUrls()
                    .then((data) => {
                        this.relatedSystemUrls = Object.freeze(data);
                    });
            },
            handleGoTemplateCreate () {
                this.$router.push({
                    name: 'templateCreate',
                });
            },
            handleGoImportTemplate () {
                this.$router.push({
                    name: 'taskImport',
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .task-list-empty-page {
        .page-header {
            padding: 40px 0 30px;
            background: #f5f6fa;

            .header-wraper {
                width: 1175px;
                margin: 0 auto;
            }
        }

        .page-wraper {
            width: 1175px;
            margin: 0 auto;
        }

        .page-title {
            font-size: 20px;
            line-height: 26px;
            color: #313238;
        }

        .page-desc {
            margin-top: 12px;
            font-size: 13px;
            line-height: 24px;
            color: #63656e;
        }

        .page-container {
            display: flex;
            margin-top: 30px;
        }

        .feature-item {
            display: flex;
            flex: 1;
            justify-content: space-between;
            height: 236px;
            padding-right: 35px;
            font-size: 12px;
            line-height: 24px;
            color: #63656e;

            .feature-box {
                width: 275px;
            }

            .feature-title {
                margin-top: 42px;
                margin-bottom: 16px;
                font-size: 16px;
                line-height: 21px;
                color: #313238;
            }
        }

        .divide-line {
            width: 1px;
            height: 160px;
            margin-top: 42px;
            background: #dcdee5;
        }

        .page-action {
            margin-top: 30px;
        }

        .page-link {
            margin-top: 60px;
            font-size: 12px;
            line-height: 16px;
            color: #979ba5;
        }
    }
</style>
