<template>
    <div
        class="more-btn"
        v-bk-tooltips="{
            allowHtml: true,
            width: 280,
            distance: 10,
            trigger: 'click',
            theme: 'light',
            content: `#stepExecuteDetailBatchPagination`,
            placement: 'bottom',
            boundary: 'window',
        }">
        <Icon type="more" />
        <div style="display: none;">
            <div
                id="stepExecuteDetailBatchPagination"
                style="padding: 17px 10px;">
                <div style="margin-bottom: 10px; font-size: 14px; line-height: 20px; color: #63656e;">跳转至</div>
                <div>
                    <bk-input
                        v-model="batchLocation"
                        type="number"
                        :min="1"
                        :max="rollingTaskNums"
                        placeholder="请输入批次"
                        @keyup="handleEnterSubmit">
                        <template slot="prepend">
                            <div class="group-text">第</div>
                        </template>
                        <template slot="append">
                            <div class="group-text">批</div>
                        </template>
                    </bk-input>
                </div>
                <div style="margin-top: 6px;font-size: 12px; line-height: 16px; color: #979ba5;">
                    共
                    <span
                        style="font-weight: bold; cursor: pointer;"
                        @click="handleGoLast">
                        {{ rollingTaskNums }}
                    </span>
                    批，已执行
                    <span
                        style="font-weight: bold; cursor: pointer;"
                        @click="handleGoRunning">
                        {{ stepData.runningBatchOrder }}
                    </span>
                    批
                </div>
                <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
                    <bk-button
                        theme="primary"
                        size="small"
                        style="margin-right: 8px;"
                        @click="handleGoBatch">
                        确定
                    </bk-button>
                    <bk-button
                        size="small"
                        @click="handleCancelGoBatch">
                        取消
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
