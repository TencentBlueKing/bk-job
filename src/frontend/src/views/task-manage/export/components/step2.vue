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
    <div class="task-export-step2-page">
        <div class="layout-wraper">
            <div
                v-bkloading="{ isLoading }"
                class="layout-left">
                <scroll-faker>
                    <div
                        v-for="item in jobList"
                        :key="item.id"
                        class="job-list">
                        <div
                            class="job-item"
                            :class="{ active: currentTemplateId === item.id }"
                            @click="handlePlanBasic(item.id)">
                            <div class="text">
                                [{{ item.id }}] {{ item.name }}
                            </div>
                        </div>
                    </div>
                </scroll-faker>
            </div>
            <div
                v-bkloading="{ isLoading: isPlanLoading }"
                class="layout-right">
                <scroll-faker>
                    <div
                        v-if="currentTemplateId"
                        class="content">
                        <p class="title">
                            {{ templateTitle }}
                        </p>
                        <div
                            v-if="renderPlanList.length > 0"
                            class="export-project">
                            <div class="project-select">
                                <p class="select-title">
                                    {{ $t('template.选择要导出的作业执行方案') }}
                                </p>
                                <div class="all-select-btn">
                                    <bk-button
                                        v-if="templateInfoMap[currentTemplateId].exportAll"
                                        text
                                        @click="handleCancelSelect">
                                        {{ $t('template.取消全选') }}
                                    </bk-button>
                                    <bk-button
                                        v-else
                                        text
                                        @click="handleAllSelect">
                                        {{ $t('template.全选') }}
                                    </bk-button>
                                </div>
                            </div>
                            <div class="project-list">
                                <div
                                    v-for="item in renderPlanList"
                                    :key="item.id"
                                    class="item"
                                    @click="handlePlanCheck(item.id)">
                                    <div
                                        class="top-middle"
                                        @click.stop="handleGoPlanDetail(item)">
                                        <span
                                            class="plan"
                                            :tippy-tips="$t('template.新窗口打开该执行方案')">
                                            {{ item.name }}
                                        </span>
                                    </div>
                                    <div
                                        class="plan-check"
                                        :class="{
                                            active: templateInfoMap[currentTemplateId].planSelectedMap[item.id],
                                        }" />
                                </div>
                            </div>
                        </div>
                        <empty
                            v-if="renderPlanList.length < 1 && !isPlanLoading"
                            style="margin-top: 100px;"
                            :title="$t('template.暂无执行方案')" />
                    </div>
                </scroll-faker>
            </div>
        </div>
        <div class="action-footer">
            <bk-button
                class="mr10"
                @click="handleCancel">
                {{ $t('template.取消') }}
            </bk-button>
            <bk-button
                class="mr10"
                @click="handleLast">
                {{ $t('template.上一步') }}
            </bk-button>
            <bk-button
                class="w120"
                theme="primary"
                @click="handleNext">
                {{ $t('template.下一步') }}
            </bk-button>
        </div>
    </div>
</template>
<script>
    import TaskManageService from '@service/task-manage';
    import TaskPlanService from '@service/task-plan';

    import { taskExport } from '@utils/cache-helper';

    import Empty from '@components/empty';

    export default {
        components: {
            Empty,
        },
        data () {
            return {
                isLoading: false,
                isPlanLoading: false,
                taskIds: taskExport.getItem('ids') || [],
                currentTemplateId: 0,
                jobList: [],
                planList: [],
                templateInfoMap: {},
            };
        },
        computed: {
            renderPlanList () {
                if (!this.currentTemplateId) {
                    return [];
                }
                return this.templateInfoMap[this.currentTemplateId].planList;
            },
            templateTitle () {
                let titleStr = '';
                this.jobList.forEach((item) => {
                    if (item.id === this.currentTemplateId) {
                        titleStr = `[${item.id}] ${item.name}`;
                    }
                });
                return titleStr;
            },
        },
        created () {
            this.fetchData();
        },
        methods: {
            /**
             * @desc 批量获取选中模板的基本数据
             */
            fetchData () {
                this.$request(TaskManageService.fetchBasic({
                    ids: this.taskIds.join(','),
                }), () => {
                    this.isLoading = true;
                }).then((data) => {
                    if (data.length < 1) {
                        return;
                    }
                    this.jobList = data;
                    this.templateInfoMap = Object.freeze(data.reduce((result, item) => {
                        result[item.id] = {
                            id: item.id,
                            planSelectedMap: {},
                            planList: [],
                            exportAll: 0,
                        };
                        return result;
                    }, {}));
                    this.currentTemplateId = data[0].id;
                    this.fetchPlanList();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 获取指定模板的执行方案数据
             */
            fetchPlanList () {
                this.isPlanLoading = true;
                TaskPlanService.fetchTaskPlan({
                    id: this.currentTemplateId,
                }).then((data) => {
                    const templateInfoMap = { ...this.templateInfoMap };
                    const currentTemplate = templateInfoMap[this.currentTemplateId];
                    currentTemplate.planList = Object.freeze(data);
                    currentTemplate.planSelectedMap = {};
                    data.forEach((plan) => {
                        currentTemplate.planSelectedMap[plan.id] = true;
                    });
                    this.templateInfoMap = Object.freeze(templateInfoMap);
                })
                    .finally(() => {
                        this.isPlanLoading = false;
                    });
            },
            /**
             * @desc 查看执行方案详情
             * @param { Object } planData
             */
            handleGoPlanDetail (planData) {
                const routerUrl = this.$router.resolve({
                    name: 'viewPlan',
                    params: {
                        templateId: planData.templateId,
                    },
                    query: {
                        from: 'taskExport',
                        viewTemplateId: planData.templateId,
                        viewPlanId: planData.id,
                    },
                });
                window.open(routerUrl.href, '_blank');
            },
            /**
             * @desc 获取模板的执行方案详情
             * @params { Number } 作业模板ID
             */
            handlePlanBasic (currentTemplateId) {
                this.currentTemplateId = currentTemplateId;
                // 已经请求过执行方案列表
                if (this.templateInfoMap[currentTemplateId].planList.length > 0) {
                    return;
                }
                this.fetchPlanList();
            },
            /**
             * @desc 选中执行方案
             * @param { Boolean } name
             */
            handlePlanCheck (planId) {
                const templateInfoMap = { ...this.templateInfoMap };
                const currentTemplate = templateInfoMap[this.currentTemplateId];
                if (currentTemplate.planSelectedMap[planId]) {
                    delete currentTemplate.planSelectedMap[planId];
                    currentTemplate.exportAll = 0;
                } else {
                    currentTemplate.planSelectedMap[planId] = true;
                    currentTemplate.exportAll
                        = Number(Object.keys(currentTemplate.planSelectedMap).length === currentTemplate.planList.length);
                }
                this.templateInfoMap = Object.freeze(templateInfoMap);
            },
            /**
             * @desc 选中所有执行方案
             */
            handleAllSelect () {
                const templateInfoMap = { ...this.templateInfoMap };
                const currTemplate = templateInfoMap[this.currentTemplateId];
                currTemplate.planList.forEach((planItem) => {
                    currTemplate.planSelectedMap[planItem.id] = true;
                });
                currTemplate.exportAll = 1;
                this.templateInfoMap = Object.freeze(templateInfoMap);
            },
            /**
             * @desc 取消选中
             */
            handleCancelSelect () {
                const templateInfoMap = { ...this.templateInfoMap };
                const currTemplate = templateInfoMap[this.currentTemplateId];
                currTemplate.planSelectedMap = {};
                currTemplate.exportAll = 0;
                this.templateInfoMap = Object.freeze(templateInfoMap);
            },
            /**
             * @desc 下一步
             */
            handleNext () {
                const templateInfo = [];
                for (const templteId in this.templateInfoMap) {
                    const currentTemplate = this.templateInfoMap[templteId];
                    templateInfo.push({
                        exportAll: currentTemplate.exportAll,
                        id: currentTemplate.id,
                        planId: Object.keys(currentTemplate.planSelectedMap),
                    });
                }
                taskExport.setItem('templateInfo', templateInfo);
                this.$emit('on-change', 3);
            },
            /**
             * @desc 上一步
             */
            handleLast () {
                this.$emit('on-change', 1);
            },
            handleCancel () {
                this.$emit('on-cancle');
            },
        },
    };
</script>
<style lang="postcss">
    .task-export-step2-page {
        .layout-wraper {
            display: flex;
        }

        .layout-left,
        .layout-right {
            height: calc(100vh - 205px);
        }

        .layout-left {
            position: relative;
            overflow: hidden;
            background: #fafbfd;
            flex: 0 0 360px;

            &::after {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                width: 1px;
                background: #dcdee5;
                content: "";
            }

            .job-list {
                .job-item {
                    height: 40px;
                    padding-left: 24px;
                    font-size: 14px;
                    line-height: 40px;
                    color: #63656e;
                    cursor: pointer;
                    border-bottom: 1px solid #dcdee5;

                    .text {
                        max-width: calc(100% - 48px);
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }

                    &.active {
                        position: relative;
                        z-index: 1;
                        background: #fff;
                    }
                }
            }
        }

        .layout-right {
            padding-top: 40px;
            padding-left: 50px;
            flex: 1;

            .content {
                width: 680px;

                .title {
                    padding-bottom: 16px;
                    font-size: 18px;
                    line-height: 24px;
                    border-bottom: 1px solid #f0f1f5;
                }
            }
        }

        .export-project {
            margin-top: 20px;

            .project-select {
                display: flex;
                font-size: 14px;
                line-height: 19px;
                color: #313238;

                .all-select-btn {
                    margin-right: 20px;
                    margin-left: auto;
                }
            }

            .project-list {
                margin-top: 12px;
                border: 1px solid #dcdee5;
                border-bottom: 0;

                .item {
                    display: flex;
                    height: 40px;
                    padding-right: 25px;
                    padding-left: 16px;
                    cursor: pointer;
                    border-bottom: 1px solid #dcdee5;
                    align-items: center;

                    .plan {
                        color: #3a84ff;
                    }
                }
            }

            .plan-check {
                position: relative;
                width: 18px;
                height: 18px;
                margin-left: auto;
                border: 1px solid #dcdee5;
                border-radius: 50%;

                &.active {
                    background: #3a84ff;
                    border-color: #3a84ff;

                    &::after {
                        position: absolute;
                        top: 4px;
                        left: 3px;
                        width: 8px;
                        height: 4px;
                        border-bottom: 2px solid #fff;
                        border-left: 2px solid #fff;
                        content: "";
                        transform: rotate(-45deg) scaleY(1);
                        transform-origin: center;
                    }
                }
            }
        }
    }
</style>
