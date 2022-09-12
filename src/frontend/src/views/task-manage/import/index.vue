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
        class="task-import-page">
        <div class="header">
            <bk-steps
                :cur-step.sync="currentStep"
                :steps="steps" />
        </div>
        <div class="content">
            <scroll-faker>
                <component
                    :is="pageCom"
                    @on-cancle="handleCancle"
                    @on-change="handleStepChange" />
            </scroll-faker>
        </div>
    </div>
</template>
<script>
    import BackupService from '@service/backup';

    import { taskImport } from '@utils/cache-helper';

    import Step1 from './pages/step1';
    import Step2 from './pages/step2';
    import Step3 from './pages/step3';
    import Step4 from './pages/step4';
    import Step5 from './pages/step5';

    import I18n from '@/i18n';

    export default {
        name: '',
        data () {
            const step = parseInt(this.$route.query.step, 10);
            return {
                isLoading: true,
                currentStep: step || 1,
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
                    5: Step5,
                };
                return comMap[this.currentStep];
            },
        },
        created () {
            this.steps = [
                { title: I18n.t('template.用户须知.header'), icon: 1 },
                { title: I18n.t('template.文件包上传'), icon: 2 },
                { title: I18n.t('template.导入内容确认'), icon: 3 },
                { title: I18n.t('template.导入设置'), icon: 4 },
                { title: I18n.t('template.开始导入'), icon: 5 },
            ];
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                BackupService.fetchInfo()
                    .then((data) => {
                        const { importJob } = data;
                        if (importJob.length > 0) {
                            // 有导入任务，直接显示第5步日志信息
                            this.handleStepChange(5);
                            taskImport.clearItem();
                            taskImport.setItem('id', importJob[0].id);
                        } else {
                            const taskImportInfo = taskImport.getItem();
                            // 没有作业导入信息，回到第一步开始
                            if (!taskImportInfo) {
                                this.handleStepChange(1);
                                return;
                            }
                            // 没有作业导入任务，回到第一步开始
                            if (!taskImportInfo.id) {
                                this.handleStepChange(1);
                                return;
                            }
                            // 没有作业选择信息，回到第3步
                            if (!taskImportInfo.templateInfo && this.currentStep > 3) {
                                this.handleStepChange(3);
                            }
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleStepChange (step) {
                this.currentStep = step;
                const searchParams = new URLSearchParams(window.location.search);
                searchParams.set('step', step);
                window.history.replaceState({}, '', `?${searchParams.toString()}`);
            },
            handleCancle () {
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
<style lang='postcss'>
    .task-import-page {
        background: #fff;

        .header {
            display: flex;
            align-items: center;
            height: 49px;
            padding: 0 57px;
            border: 1px solid #dcdee5;
        }

        .content {
            height: calc(100vh - 205px);
        }
    }
</style>
