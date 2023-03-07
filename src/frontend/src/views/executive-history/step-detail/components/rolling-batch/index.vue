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
    ref="box"
    class="batch-box">
    <div
      v-if="hasPagination"
      class="pre-btn"
      :class="{
        disabled: isPrePageBtnDisabled,
      }"
      @click="handlePreScroll">
      <icon type="arrow-full-down" />
    </div>
    <div
      class="batch-content"
      :style="contentStyles">
      <render-all
        :is-total-btn-fixed="isTotalBtnFixed"
        :select-batch="selectBatch"
        :step-data="data"
        @on-change="handleSelectAll" />
      <div
        ref="list"
        class="content-list">
        <div
          class="wrapper"
          :style="scrollStyles">
          <render-item
            v-for="batchItem in list"
            :key="batchItem.batch"
            :current-running-batch="currentRunningBatch"
            :data="batchItem"
            :select-batch="selectBatch"
            @on-change="handleSelectBatch" />
        </div>
      </div>
    </div>
    <template v-if="hasPagination">
      <div
        class="next-btn"
        :class="{
          disabled: isNextPageBtnDisabled,
        }"
        @click="handleNextBatch">
        <icon type="arrow-full-down" />
      </div>
      <render-more-btn
        :step-data="data"
        @on-change="handleLocalBatchChange" />
    </template>
    <div style="display: none;">
      <div
        id="rollingConfirmAction"
        ref="rollingConfirmAction"
        style="padding: 3px 5px; color: #63656e; user-select: none;">
        <span
          style="color: #3a84ff; cursor: pointer;"
          @click="handleConfirmExecute">
          {{ $t('history.确认继续执行') }}
        </span>
      </div>
    </div>
  </div>
</template>
<script>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import _ from 'lodash';

  import RenderAll from './render-all';
  import RenderItem from './render-item';
  import RenderMoreBtn from './render-more-btn';

  export default {
    name: '',
    components: {
      RenderAll,
      RenderItem,
      RenderMoreBtn,
    },
    props: {
      data: Object,
      value: [Number, String],
    },
    data() {
      return {
        list: [],
        selectBatch: '',
        isPrePageBtnDisabled: true,
        isNextPageBtnDisabled: false,
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
      currentRunningBatch() {
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
      isTotalBtnFixed() {
        return this.scrollPosition !== 0;
      },
      /**
       * @desc 批次列表可查看范围宽度
       * @returns { Object }
       */
      contentStyles() {
        return {
          width: this.contentWidth,
        };
      },
      /**
       * @desc 批次列表滚动
       * @returns { Object }
       */
      scrollStyles() {
        return {
          width: `${this.itemTotalWidth}px`,
          transform: `translate(${this.scrollPosition}px, 0)`,
          'will-change': 'transform',
        };
      },
    },
    watch: {
      data() {
        this.list = Object.freeze(this.data.rollingTasks);
        if (this.selectBatch !== this.data.runningBatchOrder) {
          // 当前执行中批次变化且用户没有进行主动选择批次，每次自动选中正在执行的批次
          if (this.isAutoSelectRunningBatch) {
            this.selectBatch = this.data.runningBatchOrder;
            this.triggerChange();
          }
        }
        this.showConfirmActionPanel();
      },
    },
    created() {
      this.list = Object.freeze(this.data.rollingTasks);
      // url 上面有 batch 参数默认选中 url 指定的批次
      const URLQueryBatch = parseInt(this.$route.query.batch, 10);
      this.selectBatch = URLQueryBatch > -1 ? URLQueryBatch : this.data.runningBatchOrder;
      this.isAutoSelectRunningBatch = !(URLQueryBatch > -1);
    },
    mounted() {
      this.initRender();

      setTimeout(() => {
        this.handleGoBatch(this.selectBatch);
      });
      const resizeHandler = _.throttle(() => {
        this.initRender();
      }, 20);
      window.addEventListener('resize', resizeHandler);
      this.$emit('hook:beforeDestroy', () => {
        window.removeEventListener('resize', resizeHandler);
      });
    },
    beforeDestroy() {
      if (this.popperInstance) {
        this.popperInstance.hide();
        this.popperInstance.destroy();
      }
    },
    methods: {
      /**
       * @desc 展示效果初始化
       */
      initRender() {
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
      showConfirmActionPanel() {
        setTimeout(() => {
          const $targetItemEl = this.$refs.box.querySelector('.batch-item.confirm');
          // 确认批次变更
          if (this.popperInstance
            && this.popperInstance.reference !== $targetItemEl) {
            this.popperInstance.hide();
            this.popperInstance.destroy();
            this.popperInstance = null;
          }
          // 没有待确认批次
          if (!$targetItemEl) {
            return;
          }
          const {
            left: parentLeft,
          } = this.$refs.list.getBoundingClientRect();
          const {
            left: confirmBtnLeft,
            width: confirmBtnWidth,
          } = $targetItemEl.getBoundingClientRect();
          // 切换批次后待确认批次不可见
          if (confirmBtnLeft + confirmBtnWidth / 2 < parentLeft) {
            this.popperInstance.hide();
            this.popperInstance.destroy();
            this.popperInstance = null;
            return;
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
              content: this.$refs.rollingConfirmAction,
              zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
            });
          }
          this.popperInstance.show();
        });
      },
      triggerChange() {
        this.$emit('change', this.selectBatch);
        this.$emit('input', this.selectBatch);
      },
      /**
       * @desc 人工确认继续滚动下一批
       *
       * 参考API文档继续滚动code为13
       */
      handleConfirmExecute() {
        this.$emit('on-confirm', 13);
      },
      /**
       * @desc 查看全部批次
       */
      handleSelectAll() {
        this.selectBatch = 0;
        this.isAutoSelectRunningBatch = false;
        this.triggerChange();
      },
      /**
       * @desc 选中批次
       * @param { Number } selectBatch
       * @param { Object } event
       *
       * 批次按钮显示不完整需要左移、右移显示完整
       */
      handleSelectBatch(selectBatch, event) {
        this.selectBatch = selectBatch;
        this.isAutoSelectRunningBatch = false;
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
      /**
       * @desc 跳转到指定批次，选中的批次居中显示
       * @param { Number } selectBatch
       */
      handleLocalBatchChange(selectBatch) {
        this.selectBatch = selectBatch;
        this.isAutoSelectRunningBatch = false;
        this.handleGoBatch(selectBatch);
        this.triggerChange();
      },
      /**
       * @desc 跳转到指定批次
       */
      handleGoBatch(selectBatch) {
        const $listEl = this.$refs.list;
        const {
          width: containerWidth,
          left: containerStart,
          right: containerEnd,
        } = $listEl.getBoundingClientRect();
        const $itemListEl = $listEl.querySelectorAll('.batch-item');

        const $locationBatchItemEl = $itemListEl[selectBatch - 1];
        const {
          width: locationItemWidth,
          left: locationItemStart,
          right: locationItemEnd,
        } = $locationBatchItemEl.getBoundingClientRect();

        const maxOffset = containerWidth - this.itemTotalWidth;

        // 将要定位的批次在可见范围之内，不进行滚动位移
        if (locationItemStart + 10 > containerStart && locationItemEnd + 10 < containerEnd) {
          return;
        } if (selectBatch > $itemListEl.length) {
          // 已经到最后一个了
          this.startIndex = $itemListEl.length - this.scrollNum;
          this.scrollPosition = maxOffset;
        } else {
          const locationItemLeftPosition = Array.from($itemListEl).reduce((result, $item, index) => {
            if (index < selectBatch) {
              return result + $item.getBoundingClientRect().width;
            }
            return result;
          }, 0);

          // 定位批次居中
          const achorPosition = containerWidth / 2 - locationItemWidth / 2;
          const indexOffset = Math.floor((containerWidth / 2) / locationItemWidth);

          this.scrollPosition = Math.max(
            Math.min(achorPosition - locationItemLeftPosition, 0),
            maxOffset,
          );
          this.startIndex = selectBatch - indexOffset;
        }
        this.isPrePageBtnDisabled = this.scrollPosition === 0;
        this.isNextPageBtnDisabled = this.scrollPosition === maxOffset;
      },
      /**
       * @desc 上一页
       */
      handlePreScroll() {
        setTimeout(() => {
          this.showConfirmActionPanel();
        }, 160);
        const startIndex = this.startIndex - this.scrollNum;
        if (startIndex <= 0) {
          this.startIndex = 0;
          this.scrollPosition = 0;
          this.isPrePageBtnDisabled = true;
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
        this.isPrePageBtnDisabled = false;
      },
      /**
       * @desc 下一页
       */
      handleNextBatch() {
        setTimeout(() => {
          this.showConfirmActionPanel();
        }, 160);
        const nextStartIndex = this.startIndex + this.scrollNum;

        const $listEl = this.$refs.list;
        const $itemListEl = $listEl.querySelectorAll('.batch-item');
        if (nextStartIndex + this.scrollNum >= $itemListEl.length - 1) {
          const maxRenderWidth = $listEl.getBoundingClientRect().width;
          this.startIndex = $itemListEl.length - this.scrollNum;
          this.scrollPosition = maxRenderWidth - this.itemTotalWidth;
          this.isNextPageBtnDisabled = true;
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
        this.isNextPageBtnDisabled = false;
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
      color: #979ba5;
      cursor: pointer;
      background: #e8e9f0;
      border-radius: 50%;
      justify-content: center;
      align-items: center;

      &:hover {
        background: #dcdee5;
      }

      &.disabled {
        color: #c4c6cc;
        cursor: not-allowed;
        background: #e8e9f0 !important;
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

        &.will {
          color: #b1b6c2;
        }

        .batch-item-status {
          position: absolute;
          right: 3px;
          display: flex;
          font-size: 13px;
          align-items: center;
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

        &:hover {
          background: #f0f1f5;
          border-radius: 12px;
        }

        &.fixed {
          &::after {
            opacity: 100%;
            transform: scaleX(1);
          }
        }

        &.disabled {
          cursor: not-allowed;
        }

        &.active {
          cursor: default;
          background: #fff;
          border-radius: 12px;
          box-shadow: 0 1px 2px 0 rgb(0 0 0 / 10%);
        }

        &::after {
          position: absolute;
          top: -2px;
          right: -8px;
          width: 6px;
          height: calc(100% + 4px);
          background: linear-gradient(270deg, rgb(0 0 0 / 0%), rgb(0 0 0 / 8%));
          border-left: 2px solid #e4e6ed;
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
