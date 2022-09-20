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
        ref="container"
        v-bkloading="{ isLoading }"
        class="favor-task-box">
        <template v-if="!isLoading">
            <bk-table
                v-if="favorList.length > 0"
                :data="favorList"
                :height="height">
                <bk-table-column
                    :label="$t('home.作业模板名称')"
                    prop="name"
                    show-overflow-tooltip>
                    <template slot-scope="{ row }">
                        <auth-router-link
                            auth="job_template/view"
                            class="task-name-text"
                            :permission="row.canView"
                            :resource-id="row.id"
                            :to="{
                                name: 'templateDetail',
                                params: {
                                    id: row.id,
                                },
                            }">
                            {{ row.name }}
                        </auth-router-link>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('home.状态')"
                    prop="status"
                    width="100">
                    {{ $t('home.已上线') }}
                </bk-table-column>
                <bk-table-column
                    :label="$t('home.更新人')"
                    prop="lastModifyUser"
                    width="180" />
                <bk-table-column
                    :label="$t('home.更新时间')"
                    prop="lastModifyTime"
                    width="180" />
                <bk-table-column
                    class-name="task-action"
                    :label="$t('home.操作')"
                    width="200">
                    <template slot-scope="{ row }">
                        <auth-router-link
                            auth="job_template/view"
                            :permission="row.canView"
                            :resource-id="row.id"
                            :to="{
                                name: 'viewPlan',
                                params: {
                                    templateId: row.id,
                                },
                            }">
                            {{ $t('home.执行方案') }}
                        </auth-router-link>
                        <auth-router-link
                            auth="job_template/debug"
                            :permission="row.canDebug"
                            :resource-id="row.id"
                            :to="{
                                name: 'debugPlan',
                                params: {
                                    id: row.id,
                                },
                            }">
                            {{ $t('home.调试') }}
                        </auth-router-link>
                        <auth-router-link
                            auth="job_template/edit"
                            :permission="row.canEdit"
                            :resource-id="row.id"
                            :to="{
                                name: 'templateEdit',
                                params: {
                                    id: row.id,
                                },
                            }">
                            {{ $t('home.编辑') }}
                        </auth-router-link>
                    </template>
                </bk-table-column>
            </bk-table>
            <div
                v-else
                class="list-empty">
                <img
                    class="empty-flag"
                    src="/static/images/favor-task-empty.png">
                <div style="margin-top: 12px; font-size: 14px; color: #63656e;">
                    {{ $t('home.暂无收藏的作业') }}
                </div>
                <div style="margin-top: 10px; font-size: 12px; color: #979ba5;">
                    {{ $t('home.将鼠标悬浮到作业模板行，点击收藏图标') }}
                </div>
            </div>
        </template>
    </div>
</template>
<script>
    import HomeService from '@service/home';

    export default {
        data () {
            return {
                isLoading: true,
                height: undefined,
                favorList: [],
            };
        },
        created () {
            this.fetchMyFavorList();
        },
        mounted () {
            this.init();
            window.addEventListener('resize', this.init);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.init);
            });
        },
        methods: {
            fetchMyFavorList () {
                HomeService.fetchMyFavorList()
                    .then((data) => {
                        this.favorList = Object.freeze(data);
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            init () {
                const { height } = this.$refs.container.getBoundingClientRect();
                this.height = height;
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .favor-task-box {
        height: calc(100vh - 484px);

        .task-action {
            .cell {
                & > * {
                    margin-right: 10px;
                }
            }
        }

        .list-empty {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 100%;
            margin: 0 auto;
            text-align: center;

            .empty-flag {
                width: auto;
                height: 50%;
                max-height: 240px;
                margin-top: -40px;
            }
        }
    }
</style>
