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
        v-bk-tooltips="{
            allowHtml: true,
            width: 280,
            distance: 10,
            trigger: 'click',
            theme: 'light',
            content: `#stepExecuteDetailBatchPagination`,
            placement: 'bottom',
            boundary: 'window',
        }"
        class="more-btn">
        <Icon type="more" />
        <div style="display: none;">
            <div
                id="stepExecuteDetailBatchPagination"
                style="padding: 17px 10px;">
                <div style="margin-bottom: 10px; font-size: 14px; line-height: 20px; color: #63656e;">
                    {{ $t('history.跳转至') }}
                </div>
                <div>
                    <bk-input
                        v-model="batchLocation"
                        :max="rollingTaskNums"
                        :min="1"
                        :placeholder="$t('history.请输入批次')"
                        type="number"
                        @keyup="handleEnterSubmit">
                        <template slot="prepend">
                            <div class="group-text">
                                {{ $t('history.第') }}
                            </div>
                        </template>
                        <template slot="append">
                            <div class="group-text">
                                {{ $t('history.批.input') }}
                            </div>
                        </template>
                    </bk-input>
                </div>
                <div style="margin-top: 6px;font-size: 12px; line-height: 16px; color: #979ba5;">
                    {{ $t('history.共') }}
                    <span
                        style="font-weight: bold; cursor: pointer;"
                        @click="handleGoLast">
                        {{ rollingTaskNums }}
                    </span>
                    {{ $t('history.批.total') }}，{{ $t('history.已执行') }}
                    <span
                        style="font-weight: bold; cursor: pointer;"
                        @click="handleGoRunning">
                        {{ stepData.runningBatchOrder }}
                    </span>
                    {{ $t('history.批.finished') }}
                </div>
                <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
                    <bk-button
                        size="small"
                        style="margin-right: 8px;"
                        theme="primary"
                        @click="handleGoBatch">
                        {{ $t('history.确定') }}
                    </bk-button>
                    <bk-button
                        size="small"
                        @click="handleCancelGoBatch">
                        {{ $t('history.取消') }}
                    </bk-button>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            stepData: Object,
        },
        data () {
            return {
                batchLocation: '',
            };
        },
        computed: {
            rollingTaskNums () {
                return this.stepData.rollingTasks ? this.stepData.rollingTasks.length : 0;
            },
        },
        methods: {
            triggerChange () {
                this.$emit('on-change', this.batchLocation);
            },
            /**
             * @desc 定位到指定批次
             */
            handleGoBatch () {
                const batchLocation = Math.min(Math.max(this.batchLocation, 1), this.rollingTaskNums);
                this.batchLocation = batchLocation;
                this.triggerChange();
            },
            handleGoLast () {
                this.batchLocation = this.rollingTaskNums;
                this.triggerChange();
            },
            handleGoRunning () {
                this.batchLocation = this.stepData.runningBatchOrder;
                this.triggerChange();
            },
            /**
             * @desc 跳转指定批次
             * @param { Number } value
             * @param { Object } event
             */
            handleEnterSubmit (value, event) {
                if (event.isComposing) {
                    // 跳过输入法复合事件
                    return;
                }
                if (event.keyCode === 13
                    || event.type === 'click') {
                    this.handleGoBatch();
                }
            },
            /**
             * @desc 关闭批次跳转弹框
             */
            handleCancelGoBatch () {
                document.body.click();
            },
        },
    };
</script>
