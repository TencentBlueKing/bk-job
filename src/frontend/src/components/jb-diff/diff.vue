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
        class="bk-diff"
        :class="classes">
        <div
            ref="content"
            v-html="html" />
    </div>
</template>
<script>
    import { createPatch } from 'diff';
    import * as Diff2Html from 'diff2html';
    import hljs from 'highlight.js/lib/core';
    import bash from 'highlight.js/lib/languages/bash';
    import diff from 'highlight.js/lib/languages/diff';
    import go from 'highlight.js/lib/languages/go';
    import javascript from 'highlight.js/lib/languages/javascript';
    import json from 'highlight.js/lib/languages/json';
    import perl from 'highlight.js/lib/languages/perl';
    import powershell from 'highlight.js/lib/languages/powershell';
    import python from 'highlight.js/lib/languages/python';
    import sql from 'highlight.js/lib/languages/sql';
    import typescript from 'highlight.js/lib/languages/typescript';
    import xml from 'highlight.js/lib/languages/xml';
    import _ from 'lodash';

    import 'highlight.js/styles/googlecode.css';

    const languageList = [
        { lang: 'javascript', mod: javascript },
        { lang: 'json', mod: json },
        { lang: 'shell', mod: bash },
        { lang: 'bash', mod: bash },
        { lang: 'go', mod: go },
        { lang: 'xml', mod: xml },
        { lang: 'python', mod: python },
        { lang: 'typescript', mod: typescript },
        { lang: 'sql', mod: sql },
        { lang: 'perl', mod: perl },
        { lang: 'powershell', mod: powershell },
        { lang: 'diff', mod: diff },
    ];
    languageList.forEach((item) => {
        hljs.registerLanguage(item.lang, item.mod);
    });

    export default {
        name: 'Diff',
        props: {
            oldContent: {
                type: String,
                default: '',
            },
            newContent: {
                type: String,
                default: '',
            },
            context: {
                type: Number,
                default: Infinity,
            },
            format: {
                type: String,
                default: 'line-by-line',
            },
            theme: {
                type: String,
                default: 'light',
                validator: value => ['light', 'dark'].includes(value),
            },
            language: {
                type: String,
                required: true,
            },
        },
        data () {
            return {
                html: '',
            };
        },
        computed: {
            classes () {
                return [this.theme, `format-${this.format}`];
            },
        },
        created () {
            const unwatch = this.$watch(() => `（${this.language}）${this.oldContent + this.newContent}`, () => {
                const oldContent = `${this.oldContent}\n// --end--`;
                const newContent = `${this.newContent}\n// -- end--`;

                const dd = createPatch('', oldContent, newContent, '', '', {
                    context: this.context,
                });
                const html = Diff2Html.html(dd, {
                    drawFileList: false,
                    fileListToggle: false,
                    fileListStartVisible: false,
                    fileContentToggle: false,
                    matching: 'lines',
                    outputFormat: 'side-by-side',
                    synchronisedScroll: true,
                    highlight: true,
                    renderNothingWhenEmpty: false,
                });
                this.html = html;
                setTimeout(() => {
                    const blocks = this.$refs.content.querySelectorAll('span');
                    const language = _.find(languageList, ({ lang }) => lang === this.language.toLocaleLowerCase());
                    blocks.forEach((node) => {
                        const text = node.textContent;
                        const result = language
                            ? hljs.highlight(text, {
                                language: language.lang,
                                ignoreIllegals: false,
                            })
                            : hljs.highlightAuto(text);
                        node.innerHTML = result.value;
                    });
                });
            }, {
                immediate: true,
            });
            this.$once('hook:beforeDestroy', () => {
                unwatch();
            });
        },
    };
</script>
<style lang="postcss">
    .bk-diff {
        &.dark {
            .d2h-code-line-ctn {
                color: #979ba5;
            }

            .hljs-addition,
            .hljs-deletion {
                width: auto;
                background: none;
            }
        }
    }
</style>
