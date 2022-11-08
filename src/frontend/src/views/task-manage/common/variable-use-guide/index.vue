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
    <div class="variable-use-guide">
        <div class="header">
            <div>{{ $t('template.使用指引') }}</div>
            <div class="tab-container">
                <div
                    class="tab-item"
                    :class="{ active: tab === 'global' }"
                    @click="handleTabToggle('global')">
                    {{ $t('template.全局变量.tab') }}
                </div>
                <div
                    class="tab-item"
                    :class="{ active: tab === 'magic' }"
                    @click="handleTabToggle('magic')">
                    {{ $t('template.魔法变量') }}
                </div>
            </div>
        </div>
        <scroll-faker style="height: calc(100% - 88px);">
            <div class="content">
                <div
                    style="margin-top: -24px;"
                    v-html="contentHtml" />
            </div>
        </scroll-faker>
        <div
            class="close-btn"
            @click="handleClose">
            <Icon type="close" />
        </div>
    </div>
</template>
<script>
    import globalVariableEN from './global-variable.en.md';
    import globalVariable from './global-variable.md';
    import magicVariableEN from './magic-variable.en.md';
    import magicVariable from './magic-variable.md';

    import 'highlight.js/styles/googlecode.css';

    export default {
        name: '',
        data () {
            return {
                tab: 'global',
            };
        },
        computed: {
            contentHtml () {
                const contentMap = {
                    global: globalVariableEN,
                    magic: magicVariableEN,
                };
                if (this.$i18n.locale === 'zh-CN') {
                    Object.assign(contentMap, {
                        global: globalVariable,
                        magic: magicVariable,
                    });
                }
                return contentMap[this.tab];
            },
        },
        methods: {
            handleTabToggle (tab) {
                this.tab = tab;
            },
            handleClose () {
                this.$emit('on-close');
            },
        },
    };
</script>
<style lang="postcss">
    html[lang="en-US"] {
        .variable-use-guide {
            .tab-container {
                .tab-item {
                    width: 120px;
                }
            }
        }
    }

    .variable-use-guide {
        position: relative;
        height: 100%;

        .header {
            padding-top: 16px;
            padding-left: 20px;
            font-size: 16px;
            color: #313238;
            background: #f0f1f5;
            border-bottom: 1px solid #dcdee5;

            .tab-container {
                display: flex;
                margin-top: 15px;

                .tab-item {
                    width: 84px;
                    height: 35px;
                    margin-right: 8px;
                    margin-bottom: -1px;
                    font-size: 13px;
                    line-height: 35px;
                    color: #63656e;
                    text-align: center;
                    cursor: pointer;
                    background: #dcdee5;
                    border: 1px solid #dcdee5;
                    border-bottom: none;
                    border-top-right-radius: 4px;
                    border-top-left-radius: 4px;
                    transition: all 0.1s;

                    &.active {
                        color: #313238;
                        background: #fff;
                    }
                }
            }
        }

        .content {
            padding: 15px 20px;
            overflow: hidden;
            line-height: 18px;
            color: #63656e;

            h1 {
                margin-top: 24px;
                font-size: 13px;
                font-weight: bold;
            }

            p {
                margin-top: 6px;
            }

            code {
                padding: 0 4px;
                font-size: 12px;
                color: #ea3636;
                white-space: nowrap;
                background: #fffafa;
                border: 1px solid #ffecec;
                border-radius: 2px;
            }

            pre {
                code {
                    display: block;
                    width: 100%;
                    padding: 8px 12px;
                    margin-top: 10px;
                    overflow-x: auto;
                    line-height: 22px;
                    color: #6a9a7b;
                    text-align: left;
                    white-space: pre;
                    background: #f5f6fa;
                    border: none;
                }
            }

            ul {
                & > li {
                    position: relative;
                    padding-left: 11px;
                    color: #313238;

                    &::before {
                        position: absolute;
                        top: 6px;
                        left: 0;
                        width: 5px;
                        height: 5px;
                        margin-right: 6px;
                        vertical-align: middle;
                        background: #979ba5;
                        border-radius: 50%;
                        content: "";
                    }

                    &:nth-child(n+2) {
                        margin-top: 20px;
                    }
                }
            }

            ol {
                color: #63656e;
            }

            li {
                margin-top: 6px;
            }
        }

        .close-btn {
            position: absolute;
            top: 10px;
            right: 10px;
            width: 26px;
            height: 26px;
            font-size: 18px;
            line-height: 26px;
            color: #979ba5;
            text-align: center;
            cursor: pointer;
            border-radius: 50%;

            &:hover {
                background-color: #dcdee5;
            }
        }
    }
</style>
