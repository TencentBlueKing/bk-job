<template>
    <div
        ref="box"
        class="batch-box">
        <div
            v-if="hasPagination"
            class="pre-btn"
            @click="handlePreScroll">
            <Icon type="arrow-full-down" />
        </div>
        <div
            class="batch-content"
            :style="contentStyles">
            <!-- <div
                ref="allBtn"
                class="all-btn"
                :class="{
                    active: isTotalBtnFixed || selectBatch === 0,
                }"
                key="all"
                @click="handleSelectAll">
                全部批次
            </div> -->
            <render-all
                :step-data="data"
                :is-total-btn-fixed="isTotalBtnFixed"
                :select-batch="selectBatch"
                @on-change="handleSelectAll" />
            <div
                ref="list"
                class="content-list">
                <div
                    class="wrapper"
                    :style="scrollStyles">
                    <render-item
                        v-for="batchItem in list"
                        :data="batchItem"
                        :select-batch="selectBatch"
                        :current-running-batch="currentRunningBatch"
                        :key="batchItem.batch"
                        @on-change="handleSelectBatch(batchItem.batch, $event)" />
                </div>
            </div>
        </div>
        <template v-if="hasPagination">
            <div
                class="next-btn"
                @click="handleNextBatch">
                <Icon type="arrow-full-down" />
            </div>
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
            </div>
        </template>
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
                    共 <span style="font-weight: bold;">500</span> 批，已执行 <span style="font-weight: bold;">234</span> 批
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
            <div
                id="rollingConfirmAction"
                style="padding: 3px 5px; color: #63656e; user-select: none;">
                <span
                    style="color: #3a84ff; cursor: pointer;"
                    @click="handleConfirmNextBatch">
                    确认继续执行
                </span>
                <span>下一批滚动机器</span>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import Tippy from 'bk-magic-vue/lib/utils/tippy';
    import RenderAll from './render-all';
    import RenderItem from './render-item';

    export default {
        name: '',
        components: {
            RenderAll,
            RenderItem,
        },
        props: {
            data: Object,
            value: [Number, String],
        },
        data () {
            return {
                list: [],
                selectBatch: '',
                batchLocation: '',
                contentWidth: '100%',
                startIndex: 0,
                scrollNum: 0,
                itemTotalWidth: 0,
                scrollPosition: 0,
                hasPagination: false,
            };
        },
        computed: {
            /**
             * @desc 当前正在执行的批次，全部执行完成返回 0
             * @returns { Number }
             */
            currentRunningBatch () {
                const running = _.find(this.list, ({ latestBatch }) => latestBatch);
                if (running) {
                    return running.batch;
                }
                return Infinity;
            },
            /**
             * @desc 全部批次是否固定
             * @returns { Boolean }
             */
            isTotalBtnFixed () {
                return this.scrollPosition !== 0;
            },
            /**
             * @desc 批次列表可查看范围宽度
             * @returns { Object }
             */
            contentStyles () {
                return {
                    width: this.contentWidth,
                };
            },
            /**
             * @desc 批次列表滚动
             * @returns { Object }
             */
            scrollStyles () {
                return {
                    width: `${this.itemTotalWidth}px`,
                    transform: `translate(${this.scrollPosition}px, 0)`,
                };
            },
        },
        watch: {
            data () {
                this.list = Object.freeze(this.data.rollingTasks);
                // 用户没有选择批次时根据执行状态自动选中最新的批次
                if (this.value === '') {
                    this.selectBatch = this.data.runningBatchOrder;
                }
            },
        },
        
        mounted () {
            this.init();
            const resizeHandler = _.throttle(() => {
                this.init();
            }, 20);
            window.addEventListener('resize', resizeHandler);
            this.$emit('hook:beforeDestroy', () => {
                window.removeEventListener('resize', resizeHandler);
            });
        },
        created () {
            this.list = Object.freeze(this.data.rollingTasks);
            this.selectBatch = this.data.runningBatchOrder;
        },
        beforeDestroy () {
            this.popperInstance && this.popperInstance.hide();
        },
        methods: {
            /**
             * @desc 展示效果初始化
             */
            init () {
                const $listEL = this.$refs.box;
                const $itemList = $listEL.querySelectorAll('.batch-item');
                const allBtnWidth = this.$refs.allBtn.getBoundingClientRect().width;
                const maxRenderWidth = $listEL.clientWidth - allBtnWidth;

                let itemTotalWidth = 0;
                let scrollNum = 0;
                let hasPagination = false;
                $itemList.forEach((item) => {
                    const {
                        width,
                    } = item.getBoundingClientRect();
                    itemTotalWidth += width;
                    if (itemTotalWidth <= maxRenderWidth) {
                        scrollNum += 1;
                        return;
                    }
                    hasPagination = true;
                });
                if (hasPagination) {
                    // 需要分页
                    // 同时显示上一页、下一页、更多分页按钮，单页可显示批次需减去相应的位置
                    this.scrollNum = scrollNum - 2;
                    this.hasPagination = true;
                }
                
                this.itemTotalWidth = itemTotalWidth;
                this.contentWidth = this.hasPagination ? '100%' : `${itemTotalWidth + allBtnWidth}px`;
                this.showConfirmActionPanel();
            },
            /**
             * @desc 批次需要人工确认，弹出操作框
             */
            showConfirmActionPanel () {
                const $targetItemEl = this.$refs.box.querySelector('.batch-item.confirm');
                if (!$targetItemEl) {
                    return;
                }
                if (this.popperInstance
                    && this.popperInstance.reference !== $targetItemEl) {
                    this.popperInstance.hide();
                }
                if (!this.popperInstance) {
                    this.popperInstance = Tippy($targetItemEl, {
                        arrow: true,
                        placement: 'bottom',
                        trigger: 'manual',
                        theme: 'light',
                        interactive: true,
                        hideOnClick: false,
                        animateFill: false,
                        animation: 'slide-toggle',
                        lazy: false,
                        ignoreAttributes: true,
                        boundary: 'window',
                        distance: 20,
                        content: this.$refs.box.querySelector('#rollingConfirmAction'),
                        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                    });
                    this.popperInstance.show();
                }
            },
            triggerChange () {
                this.$emit('change', this.selectBatch);
                this.$emit('input', this.selectBatch);
            },
            /**
             * @desc 查看全部批次
             */
            handleSelectAll () {
                this.selectBatch = 0;
                this.triggerChange();
            },
            /**
             * @desc 选中批次
             * @param { Number } selectBatch
             * @param { Object } event
             *
             * 批次按钮显示不完整需要左移、右移显示完整
             */
            handleSelectBatch (selectBatch, event) {
                if (
                    selectBatch === this.selectBatch
                    || selectBatch > this.currentRunningBatch) {
                    return;
                }
                this.selectBatch = selectBatch;
                this.triggerChange();
                if (!this.hasPagination) {
                    return;
                }
                // 处理大量批次的分页问题
                const { target } = event;
                const {
                    width: containerWidth,
                    left: containerStart,
                    right: containerEnd,
                } = this.$refs.list.getBoundingClientRect();
                const {
                    width: selectTargetWidth,
                    left: selectTargetStart,
                    right: selectTargetEnd,
                } = target.getBoundingClientRect();

                if (selectTargetEnd > containerEnd) {
                    // 下一批
                    const maxSrollOffset = containerWidth - this.itemTotalWidth;
                    this.scrollPosition = Math.max(maxSrollOffset, this.scrollPosition - selectTargetWidth);
                    this.startIndex += 1;
                } else if (selectTargetStart < containerStart) {
                    // 上一批
                    this.scrollPosition = Math.min(0, this.scrollPosition + selectTargetWidth);
                    this.startIndex -= 1;
                }
            },
            handleConfirmNextBatch () {
                
            },
            /**
             * @desc 上一页
             */
            handlePreScroll () {
                setTimeout(() => {
                    this.popperInstance.show();
                }, 200);
                const startIndex = this.startIndex - this.scrollNum;
                if (startIndex <= 0) {
                    this.startIndex = 0;
                    this.scrollPosition = 0;
                    return;
                }
                const $itemList = this.$refs.list.querySelectorAll('.batch-item');
                let scrollPosition = 0;
                $itemList.forEach(($item, index) => {
                    if (index > startIndex) {
                        return;
                    }
                    scrollPosition += $item.getBoundingClientRect().width;
                });
                this.scrollPosition = -scrollPosition;
                this.startIndex = startIndex;
            },
            /**
             * @desc 下一页
             */
            handleNextBatch () {
                setTimeout(() => {
                    this.popperInstance.show();
                }, 200);
                const nextStartIndex = this.startIndex + this.scrollNum;

                const $listEl = this.$refs.list;
                const $itemListEl = $listEl.querySelectorAll('.batch-item');
                if (nextStartIndex + this.scrollNum >= $itemListEl.length - 1) {
                    const maxRenderWidth = $listEl.getBoundingClientRect().width;
                    this.startIndex = $itemListEl.length - this.scrollNum;
                    this.scrollPosition = maxRenderWidth - this.itemTotalWidth;
                    return;
                }
                let { scrollPosition } = this;
                $itemListEl.forEach(($item, index) => {
                    if (index > this.startIndex && index < nextStartIndex) {
                        scrollPosition -= $item.getBoundingClientRect().width;
                    }
                });
                this.startIndex = nextStartIndex;
                this.scrollPosition = scrollPosition;
            },
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
             * @desc 定位到指定批次，居中显示
             */
            handleGoBatch () {
                const $listEl = this.$refs.list;
                const {
                    width: containerWidth,
                    left: containerStart,
                    right: containerEnd,
                } = $listEl.getBoundingClientRect();
                const $itemListEl = $listEl.querySelectorAll('.batch-item');
                const batchLocation = Math.min(Math.max(this.batchLocation, 1), $itemListEl.length);
                this.selectBatch = batchLocation;
                this.triggerChange();
                
                const $locationBatchItemEl = $itemListEl[batchLocation - 1];
                const {
                    width: locationItemWidth,
                    left: locationItemStart,
                    right: locationItemEnd,
                } = $locationBatchItemEl.getBoundingClientRect();

                // 将要定位的批次在可见范围之内，不进行滚动位移
                if (locationItemStart + 10 > containerStart && locationItemEnd + 10 < containerEnd) {
                    return;
                }
                
                if (batchLocation > $itemListEl.length) {
                    this.startIndex = $itemListEl.length - this.scrollNum;
                    this.scrollPosition = containerWidth - this.itemTotalWidth;
                    return;
                }

                const locationItemLeftPosition = Array.from($itemListEl).reduce((result, $item, index) => {
                    if (index < batchLocation) {
                        return result + $item.getBoundingClientRect().width;
                    }
                    return result;
                }, 0);

                // 定位批次居中
                const achorPosition = containerWidth / 2 - locationItemWidth / 2;
                const indexOffset = Math.floor((containerWidth / 2) / locationItemWidth);
                
                this.scrollPosition = Math.max(
                    Math.min(achorPosition - locationItemLeftPosition, 0),
                    containerWidth - this.itemTotalWidth,
                );
                this.startIndex = batchLocation - indexOffset;
            },
            handleCancelGoBatch () {
                document.body.click();
            },
        },
    };
</script>
<style lang="postcss" scoped>
    .batch-box {
        display: flex;
        padding: 20px 24px 12px;
        background: #f5f6fa;

        .pre-btn,
        .next-btn,
        .more-btn {
            display: flex;
            flex: 0 0 auto;
            width: 28px;
            height: 28px;
            color: #c4c6cc;
            cursor: pointer;
            background: #e8e9f0;
            border-radius: 50%;
            justify-content: center;
            align-items: center;

            &:hover {
                background: #f0f1f5;
            }
        }

        .pre-btn {
            margin-right: 8px;

            i {
                transform: rotateZ(90deg);
            }
        }

        .next-btn {
            margin-left: 8px;

            i {
                transform: rotateZ(-90deg);
            }
        }

        .more-btn {
            margin-left: 6px;
        }

        .batch-content {
            display: flex;
            padding: 2px;
            font-size: 12px;
            color: #63656e;
            background: #e4e6ed;
            border-radius: 18px;
            box-sizing: content-box;
            user-select: none;

            .content-list {
                position: relative;
                height: 24px;
                overflow: hidden;
                flex: 1;
            }

            .wrapper {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                display: flex;
                align-items: center;
                transition: all 0.15s;
            }

            .batch-item {
                position: relative;
                display: flex;
                height: 24px;
                padding: 0 16px;
                cursor: pointer;
                border-radius: 12px;
                transition: all 0.15s;
                flex: 1 0 auto;
                align-items: center;
                justify-content: center;

                &:hover,
                &.active {
                    &::after {
                        opacity: 0%;
                    }
                }

                &:hover {
                    background: #f0f1f5;
                }

                &.active {
                    cursor: default;
                    background: #fff;
                    border-radius: 12px;
                    box-shadow: 0 1px 2px 0 rgb(0 0 0 / 10%);
                }

                &.confirm {
                    &::after {
                        position: absolute;
                        right: 6px;
                        bottom: 0;
                        left: 15px;
                        border-bottom: 1px dashed #ff9c01;
                        content: "";
                    }

                    .batch-item-status {
                        color: #ff9c01;
                    }
                }

                &.disabled {
                    color: #b1b6c2;
                    cursor: not-allowed;

                    &:hover {
                        background: transparent;
                    }
                }

                .batch-item-status {
                    position: absolute;
                    right: 3px;
                    font-size: 13px;
                }
            }

            .all-btn {
                position: relative;
                z-index: 1;
                display: flex;
                height: 100%;
                padding: 0 18px;
                cursor: pointer;
                align-items: center;
                flex: 0 0 auto;
                border-radius: 0;

                &:hover,
                &.active {
                    &::after {
                        opacity: 100%;
                        transform: scaleX(1);
                    }
                }

                &.disabled {
                    cursor: not-allowed;
                }

                &::after {
                    position: absolute;
                    top: -2px;
                    right: -4px;
                    width: 6px;
                    height: calc(100% + 4px);
                    background: linear-gradient(270deg, rgb(0 0 0 / 0%), rgb(0 0 0 / 8%));
                    content: "";
                    opacity: 0%;
                    transform: scaleX(0);
                    transition: all 0.15s;
                    transform-origin: left center;
                }
            }
        }
    }
</style>
