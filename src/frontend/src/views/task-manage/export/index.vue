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
        v-bkloading="{ isLoading }"
        class="export-job">
        <div class="steps">
            <bk-steps
                :cur-step.sync="curStep"
                :steps="steps" />
        </div>
        <div class="page-wraper">
            <scroll-faker>
                <component
                    :is="pageCom"
                    @on-cancle="handleCancel"
                    @on-change="handleStepChange" />
            </scroll-faker>
        </div>
    </div>
</template>
<script>
    import BackupService from '@service/backup';

    import { taskExport } from '@utils/cache-helper';

    import Step1 from './components/step1';
    import Step2 from './components/step2';
    import Step3 from './components/step3';
    import Step4 from './components/step4';

    import I18n from '@/i18n';
    
    export default {
        name: 'ExportJob',
        data () {
            const step = parseInt(this.$route.query.step, 10);
            return {
                isLoading: true,
                curStep: step || 1,
            };
        },
        computed: {
            pageCom () {
                if (this.isLoading) {
                    return 'div';
                }
                const comMap = {
                    1: Step1,
                    2: Step2,
                    3: Step3,
                    4: Step4,
                };
                return comMap[this.curStep];
            },
        },
        created () {
            this.steps = [
                { title: I18n.t('template.用户须知.header'), icon: 1 },
                { title: I18n.t('template.导出内容确认'), icon: 2 },
                { title: I18n.t('template.导出设置'), icon: 3 },
                { title: I18n.t('template.开始导出'), icon: 4 },
            ];
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                BackupService.fetchInfo()
                    .then((data) => {
                        const { exportJob } = data;
                        if (exportJob.length > 0) {
                            // 有后台导入任务进行中
                            // 展示任务的执行结果——最后一步
                            this.handleStepChange(4);
                            taskExport.clearItem();
                            taskExport.setItem('id', exportJob[0].id);
                        } else {
                            // 没有后台任务
                            const taskExportInfo = taskExport.getItem();
                            // 检测本地操作缓存
                            // 没有本地操作缓存——返回作业列表
                            if (!taskExportInfo.ids || taskExportInfo.ids.length < 1) {
                                this.messageError(I18n.t('template.你还未选择导出作业'), () => {
                                    this.$router.push({
                                        name: 'taskList',
                                    });
                                });
                                return;
                            }
                            // 有本地操作缓存——停留在当前有效操作步骤
                            if (!taskExportInfo.templateInfo && this.curStep > 2) {
                                this.handleStepChange(2);
                            }
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleStepChange (step) {
                this.curStep = step;
                const searchParams = new URLSearchParams(window.location.search);
                searchParams.set('step', step);
                window.history.replaceState({}, '', `?${searchParams.toString()}`);
            },
            handleCancel () {
                this.routerBack();
            },
            routerBack () {
                this.$router.push({
                    name: 'taskList',
                });
            },
        },
    };
</script>
<style lang="postcss">
    .export-job {
        background-color: #fff;

        .steps {
            display: flex;
            width: 100%;
            height: 50px;
            padding: 0 200px;
            border-bottom: 1px solid #ccc;
            align-items: center;
        }

        .page-wraper {
            height: calc(100vh - 205px);
        }

        .action-footer {
            position: fixed;
            right: 0;
            bottom: 0;
            left: 0;
            z-index: 9;
            display: flex;
            height: 52px;
            padding-right: 24px;
            background-color: #fff;
            border-top: 1px solid #ccc;
            align-items: center;
            justify-content: flex-end;
        }
    }
</style>
