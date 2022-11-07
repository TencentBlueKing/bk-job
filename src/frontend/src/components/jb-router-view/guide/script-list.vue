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
    <div class="script-list-empty-page">
        <div class="page-header">
            <div class="header-wraper">
                <div class="page-title">
                    {{ $t('当前业务暂无脚本，请先创建') }}
                </div>
                <div class="page-desc">
                    {{ $t('脚本模块提供标准规范的版本管理功能，帮助用户有效的管理脚本各版本的状态和被作业引用的情况，提高脚本使用的安全性和可复用率。') }}
                </div>
                <div class="page-action">
                    <bk-button
                        theme="primary"
                        @click="handleGoCreateScript">
                        {{ $t('新建脚本') }}
                    </bk-button>
                </div>
            </div>
        </div>
        <div class="page-wraper">
            <div class="page-container">
                <div class="feature-item">
                    <div class="feature-pic">
                        <img
                            src="/static/images/guide/script-new.svg"
                            style="width: 220px; margin: 39px 39px 0 31px;">
                    </div>
                    <div class="feature-box">
                        <div class="feature-title">
                            {{ $t('创建一个全新的脚本') }}
                        </div>
                        <div>{{ $t('我们鼓励大家将业务脚本升迁到作业平台，通过 “云化”管理模式有序地整理业务脚本、有效地维护脚本使用场景的调用关系，提高使用的安全性。') }}</div>
                    </div>
                </div>
                <div class="divide-line" />
                <div class="feature-item">
                    <div class="feature-pic">
                        <img
                            src="/static/images/guide/script-version.svg"
                            style="width: 230px; margin: 32px 27px 0 7px;">
                    </div>
                    <div class="feature-box">
                        <div class="feature-title">
                            {{ $t('管理脚本各个版本的内容和状态') }}
                        </div>
                        <div>{{ $t('版本管理功能提供除了基础的版本代码内容切换查看和对比以外，支持根据需求设置版本的状态，以及维护脚本和作业的引用和同步关系。') }}</div>
                    </div>
                </div>
            </div>
            <div class="page-link">
                <div>
                    <span>{{ $t('脚本的版本管理功能有哪些？点击') }} </span>
                    <a
                        :href="`${relatedSystemUrls.BK_DOC_CENTER_ROOT_URL}/markdown/作业平台/产品白皮书/Features/Scripts.md`"
                        target="_blank">
                        <span>{{ $t('脚本功能介绍') }}</span>
                        <Icon type="link" />
                    </a>
                    <span>{{ $t('了解更多细节') }}</span>
                </div>
                <div style="margin-top: 10px;">
                    <span>{{ $t('想要 “禁用” 脚本使其他调用方无法使用怎么做？') }}</span>
                    <a
                        :href="`${relatedSystemUrls.BK_DOC_CENTER_ROOT_URL}/markdown/作业平台/产品白皮书/Best-Practices/How-to-stop-the-spread-of-the-problem-script-immediately.md`"
                        target="_blank">
                        <span>{{ $t('立即查看') }}</span>
                        <Icon type="link" />
                    </a>
                </div>
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
            handleGoCreateScript () {
                this.$router.push({
                    name: 'createScript',
                });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .script-list-empty-page {
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
