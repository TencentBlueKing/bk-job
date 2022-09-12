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
        ref="download"
        class="step-execute-log-export"
        :tippy-tips="isFile ? $t('history.分发文件步骤不支持日志导出') : ''"
        @click="handleShow">
        <div>{{ $t('history.导出日志') }}</div>
        <div
            v-if="isShowThumProcess"
            class="thum-precess-bar"
            :class="boxClass">
            <div
                class="thum-precess"
                :style="{ width: `${process * 100}%` }" />
        </div>
        <div style="display: none;">
            <div
                ref="progress"
                class="step-execute-log-package"
                :class="boxClass">
                <div class="package-baseinfo">
                    <div class="package-size">
                        {{ $t('history.文件大小') }}: {{ fileSize | formFileSize }}
                    </div>
                    <div class="package-result">
                        <span v-if="packageStatus === 1">{{ $t('history.打包中') }}…</span>
                        <span v-if="packageStatus === 2">{{ $t('history.打包中') }}…</span>
                        <span v-if="packageStatus === 3">
                            <span>{{ $t('history.打包失败，') }}</span>
                            <a @click="handleGetLogFilePackageResult">{{ $t('history.重试') }}</a>
                        </span>
                        <span v-if="packageStatus === 4">{{ $t('history.准备就绪') }}</span>
                    </div>
                </div>
                <bk-progress
                    :key="resetKey"
                    class="package-process"
                    :percent="process"
                    :show-text="false"
                    size="small"
                    theme="primary" />
                <div
                    v-if="packageStatus === 1"
                    class="package-result-tips">
                    {{ $t('history.温馨提示：打包耗时会受到总的日志内容大小影响，请耐心等待') }}
                </div>
                <div
                    v-if="packageStatus === 2"
                    class="package-result-tips">
                    {{ $t('history.温馨提示：文件打包中请勿关闭浏览器，以免导致任务中断') }}
                </div>
                <div
                    v-if="packageStatus === 3"
                    class="package-result-tips">
                    {{ $t('history.日志文件打包超时，可能因为日志量过大，请选择单台日志下载') }}
                </div>
                <div
                    v-if="packageStatus === 4"
                    class="package-result-tips">
                    <!-- eslint-disable-next-line max-len -->
                    <span>{{ $t('history.日志压缩包已准备就绪，') }}{{ $t('history.是否') }}<a @click="handleDownload">{{ $t('history.直接下载') }}</a></span>
                    <bk-button
                        style="margin-top: -5px; margin-left: auto;"
                        theme="primary"
                        @click="handleGetLogFilePackageResult">
                        {{ $t('history.重新打包') }}
                    </bk-button>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import TaskExecuteService from '@service/task-execute';

    import {
        bytePretty,
    } from '@utils/assist';

    export default {
        name: '',
        filters: {
            /**
             * @desc 格式化文件大小显示
             */
            formFileSize (value) {
                if (Number(value) < 1) {
                    return '--';
                }
                return bytePretty(value);
            },
        },
        props: {
            isFile: Boolean,
            stepInstanceId: {
                type: Number,
                required: true,
            },
        },
        data () {
            return {
                // 弹层显示状态标记
                isPopoverShowFlag: false,
                // 是否已经下载日志
                isLogDownloaded: false,
                fileSize: 0,
                // 日志打包状态
                packageStatus: 1,
                // 打包进度，本地模拟
                process: 0,
            };
        },
        computed: {
            boxClass () {
                const classMap = {
                    1: 'normal',
                    2: 'packageing',
                    3: 'failed',
                    4: 'success',
                };
                return classMap[this.packageStatus];
            },
            /**
             * @desc 日志文件打包完成但没有下载，并且弹层是关闭状态显示进度条缩略图
             */
            isShowThumProcess () {
                return !this.isPopoverShowFlag && !this.isLogDownloaded && this.isPackageing;
            },
        },
        created () {
            this.isPackageing = false;
            // api 标记重新打包
            this.repackage = false;
            // 用于重置 bk-progress
            this.resetKey = 1;
        },
        mounted () {
            this.initPopover();
            this.taskQueue = [];
        },
        beforeDestroy () {
            this.stopQueueRun();
        },
        methods: {
            /**
             * @desc 获取日志打包结果
             *
             * 打包过程没结束一直轮询状态
             */
            fetchLogFilePackageResult () {
                TaskExecuteService.fetchLogFilePackageResult({
                    stepInstanceId: this.stepInstanceId,
                    repackage: this.repackage,
                }).then((data) => {
                    this.fileSize = data.fileSize || 0;
                    this.packageStatus = data.status;
                    if (this.packageStatus === 1 || this.packageStatus === 2) {
                        this.pushQueue(this.fetchLogFilePackageResult);
                    } else {
                        this.stopQueueRun();
                        // 打包成功完成任务进度条
                        // 自动下载日志
                        if (this.packageStatus === 4) {
                            this.endProgress();
                            this.handleDownload();
                        }
                    }
                })
                    .catch(() => {
                        // 接口报错终止任务轮询
                        this.stopQueueRun();
                    })
                    .finally(() => {
                        this.repackage = false;
                    });
            },
            /**
             * @desc 弹层初始化
             */
            initPopover () {
                this.popperInstance = this.$bkPopover(this.$refs.download, {
                    arrow: true,
                    placement: 'bottom',
                    interactive: true,
                    trigger: 'click',
                    theme: 'light',
                    animation: 'slide-toggle',
                    distance: 30,
                    content: this.$refs.progress,
                    zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                    onShow: () => {
                        this.isPopoverShowFlag = true;
                    },
                    onHidden: () => {
                        this.isPopoverShowFlag = false;
                    },
                });
            },
            /**
             * @desc 推入轮询任务队列
             */
            pushQueue (task) {
                if (!this.taskQueue) {
                    this.taskQueue = [];
                }
                this.taskQueue.push(task);
            },
            /**
             * @desc 开始轮询
             */
            startQueueRun () {
                const nextTask = this.taskQueue.shift();
                if (nextTask) {
                    nextTask();
                }
                this.timer = setTimeout(() => {
                    this.startQueueRun();
                }, 1000);
            },
            /**
             * @desc 终止轮询
             */
            stopQueueRun () {
                this.stopQueueRunning = true;
                clearTimeout(this.timer);
            },
            /**
             * @desc 开始模拟任务进度
             */
            startProgress () {
                if (this.process > 0.85) {
                    return;
                }
                this.process += 0.05;
                setTimeout(() => {
                    this.startProgress();
                }, 30);
            },
            /**
             * @desc 任务进度完成
             */
            endProgress () {
                this.process = 1;
            },
            /**
             * @desc 显示弹层
             *
             * 如果没有开始打包过程则开始日志文件的打包
             */
            handleShow () {
                if (!this.isPackageing) {
                    this.isPackageing = true;
                    this.handleGetLogFilePackageResult();
                }
            },
            /**
             * @desc 重新打包日志文件
             *
             * 重置打包状态
             */
            handleGetLogFilePackageResult () {
                this.fileSize = 0;
                this.packageStatus = 1;
                this.process = 0;
                this.resetKey += 1;
                this.isLogDownloaded = false;
                this.startQueueRun();
                this.startProgress();
                setTimeout(() => {
                    this.repackage = true;
                    this.fetchLogFilePackageResult();
                }, 300);
            },
            /**
             * @desc 下载日志打包文件
             */
            handleDownload () {
                this.isLogDownloaded = true;
                TaskExecuteService.fetchStepExecutionLogFile({
                    id: this.stepInstanceId,
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .step-execute-log-export {
        position: relative;
        height: 32px;
        padding: 0 15px;
        margin-left: 10px;
        font-size: 14px;
        line-height: 32px;
        color: #63656e;
        cursor: pointer;
        background: #fafbfd;
        border: 1px solid #c4c6cc;
        border-radius: 2px;
        user-select: none;
        flex: 0 0 auto;

        .thum-precess-bar {
            position: absolute;
            right: 0;
            bottom: -1px;
            left: 0;
            height: 2px;
            background: #dcdee5;
            border-radius: 1px;

            &.normal,
            &.packageing {
                .thum-precess {
                    background: #3a84ff;
                }
            }

            &.failed {
                .thum-precess {
                    background: #ea3636;
                }
            }

            &.success {
                .thum-precess {
                    background: #2dcb56;
                }
            }

            .thum-precess {
                width: 50%;
                height: 100%;
                background: #3a84ff;
                border-radius: 1px;
            }
        }
    }

    .step-execute-log-package {
        position: relative;
        width: 390px;
        padding: 14px 17px;
        font-size: 12px;
        line-height: 16px;

        &.failed {
            .package-result,
            .package-result-tips {
                color: #ea3636;
            }

            .bk-progress .progress-inner {
                background: #ea3636;
            }
        }

        &.success {
            .package-result {
                color: #63656e;
            }

            .bk-progress .progress-inner {
                background: #2dcb56;
            }
        }

        .package-baseinfo {
            display: flex;

            .package-size {
                margin-right: auto;
            }

            .package-result {
                color: #979ba5;
            }
        }

        .package-process {
            margin-top: 10px;
        }

        .package-result-tips {
            display: flex;
            align-items: center;
            margin-top: 13px;
        }
    }
</style>
