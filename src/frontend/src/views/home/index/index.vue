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
    <div class="page-home">
        <div class="layout-row">
            <div class="layout-left">
                <div class="layout-row content-top">
                    <layout-card class="user-card">
                        <user />
                    </layout-card>
                    <layout-card class="agent-card" :title="$t('home.Agent 状态分布')">
                        <agent />
                    </layout-card>
                </div>
                <div class="layout-row">
                    <layout-card :title="$t('home.我收藏的作业')" class="my-task">
                        <favor-task />
                    </layout-card>
                </div>
            </div>
            <div class="layout-right">
                <div class="layout-row content-top">
                    <layout-card class="work-statistics-card">
                        <work-statistics type="job-statistics" link="taskList">
                            <template #default="{ jobNum }">
                                <span>{{ jobNum }}</span>
                            </template>
                            <span slot="name">{{ $t('home.作业量') }}</span>
                        </work-statistics>
                    </layout-card>
                    <layout-card class="work-statistics-card">
                        <work-statistics type="script-statistics" link="scriptList">
                            <template #default="{ scriptNum }">
                                <span>{{ scriptNum }}</span>
                            </template>
                            <span slot="name">{{ $t('home.脚本量') }}</span>
                        </work-statistics>
                    </layout-card>
                </div>
                <div class="layout-row">
                    <layout-card :title="$t('home.最近执行记录')" class="record-card">
                        <history-record />
                    </layout-card>
                </div>
            </div>
        </div>
        <div class="page-footer">
            <div v-html="footerLink" />
            <div v-html="footerCopyRight" />
        </div>
    </div>
</template>
<script>
    import marked from 'marked';
    import xss from 'xss';
    import QueryGlobalSettingService from '@service/query-global-setting';
    import LayoutCard from './components/card';
    import User from './components/user';
    import Agent from './components/agent';
    import WorkStatistics from './components/work-statistics';
    import FavorTask from './components/favor-task';
    import HistoryRecord from './components/history-record';

    const xssHTML = (html) => {
        const attrs = ['class', 'title', 'target', 'style'];
        return xss(html, {
            onTagAttr: (tag, name, value, isWhiteAttr) => {
                if (attrs.includes(name)) {
                    return `${name}=${value}`;
                }
            },
        });
    };

    export default {
        name: '',
        components: {
            LayoutCard,
            User,
            Agent,
            WorkStatistics,
            FavorTask,
            HistoryRecord,
        },
        data () {
            return {
                footerLink: '',
                footerCopyRight: '',
            };
        },
        created () {
            this.fetchTitleAndFooter();
        },
        methods: {
            fetchTitleAndFooter () {
                const formatLink = link => link.replace(/(?=( href))/g, ' target="_blank"');
                QueryGlobalSettingService.fetchFooterConfig()
                    .then((data) => {
                        this.footerLink = xssHTML(formatLink(marked(`${data.footerLink}`)));
                        this.footerCopyRight = xssHTML(marked(data.footerCopyRight));
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .page-home {
        display: flex;
        flex-direction: column;

        .layout-left {
            display: flex;
            flex-direction: column;
            flex: 1;
            margin-right: 20px;
        }

        .layout-right {
            display: flex;
            width: 28.6168%;
            max-width: 480px;
            min-width: 360px;
            flex-direction: column;
            flex: 1;
        }

        .layout-row {
            display: flex;

            &.content-top {
                height: 196px;
                margin-bottom: 20px;
            }
        }

        .user-card {
            flex: 1 0 480px;
            min-width: 480px;
            margin-right: 20px;
        }

        .agent-card {
            flex: 1 1 378px;
        }

        .work-statistics-card {
            width: calc(50% - 10px);
            margin-right: 20px;

            &:hover {
                box-shadow: 0 2px 6px 0 rgb(0 0 0 / 10%);

                .hexagon {
                    animation: hexagon-scale 0.3s ease-in forwards;
                }
            }

            &:last-child {
                margin-right: 0;
            }
        }

        .record-card {
            flex: 1;
        }

        .my-task {
            flex: 1;
            width: 878px;
        }

        .page-footer {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 52px;
            margin-top: 20px;
            font-size: 12px;
            color: #63656e;
            border-top: 1px solid #dcdee5;
        }
    }
</style>
