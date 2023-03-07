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
  <div class="sync-plan-side-anchor">
    <scroll-faker ref="scroll">
      <div
        ref="anchor"
        class="anchor-wraper">
        <div class="item-title">
          {{ $t('template.全局变量.label') }}
        </div>
        <div
          v-for="item in variable"
          :key="`variable_${item.id}_${item.name}`"
          class="item"
          :class="{
            active: active === `variable_${item.id}_${item.name}`,
          }"
          :data-anchor="`variable_${item.id}_${item.name}`"
          @click="handleChoose(`variable_${item.id}_${item.name}`)"
          @mouseenter="handleShowTips(item.name, `variable_${item.id}_${item.name}`)"
          @mouseleave="handleShowTips('', '')">
          <div class="anchor-text">
            <span>{{ item.name }}</span>
          </div>
        </div>
        <div class="item-title">
          {{ $t('template.作业步骤.label') }}
        </div>
        <div
          v-for="item in step"
          :key="`step_${item.id}`"
          class="item"
          :class="{
            active: active === `step_${item.id}`,
          }"
          :data-anchor="`step_${item.id}`"
          @click="handleChoose(`step_${item.id}`)"
          @mouseenter="handleShowTips(item.name, `step_${item.id}`)"
          @mouseleave="handleShowTips('', '')">
          <div class="anchor-text">
            <span>{{ item.name }}</span>
          </div>
        </div>
      </div>
      <tips
        v-if="tips.name"
        :key="tips.name"
        :data="tips" />
    </scroll-faker>
  </div>
</template>
<script>
  import _ from 'lodash';

  import { scrollTopSmooth } from '@utils/assist';

  import Tips from './tips';

  export default {
    name: '',
    components: {
      Tips,
    },
    props: {
      variable: {
        type: Array,
        required: true,
      },
      step: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        active: '',
        tips: {
          name: '',
          target: '',
        },
      };
    },
    created() {
      if (this.variable.length > 0) {
        const [{ id, name }] = this.variable;
        this.active = `variable_${id}_${name}`;
      } else {
        this.active = `${this.step[0].id}_${this.step[0].name}`;
      }
      this.scrollNum = 0;
      this.offsetTopMap = {};
    },
    mounted() {
      const $scrollContent = document.querySelector('#asynContent').querySelector('.scroll-faker-content');
      $scrollContent.addEventListener('scroll', this.setActive);
      this.$once('hook:beforeDestroy', () => {
        $scrollContent.removeEventListener('scroll', this.setActive);
      });
      this.init();
    },
    methods: {
      init() {
        this.$refs.anchor.querySelectorAll('.item').forEach((item) => {
          const anchorTarget = `${item.getAttribute('data-anchor')}`;
          this.offsetTopMap[anchorTarget] = document.querySelector(`#sync-after_${anchorTarget}`).offsetTop;
        });
      },
      setActive: _.throttle(function (event) {
        this.scrollNum = 0;
        const $target = event.target;
        const { scrollTop } = $target;
        for (const key in this.offsetTopMap) {
          if (this.offsetTopMap[key] < scrollTop + 60) {
            this.active = key;
            this.scrollNum += 1;
          } else {
            break;
          }
        }
        this.$nextTick(() => {
          this.$refs.scroll.scrollTo(26 * this.scrollNum);
        });
      }, 50),
      handleShowTips(name, target) {
        this.tips = {
          name,
          target,
        };
      },
      handleChoose(active) {
        const target = document.querySelector(`#sync-after_${active}`);
        const scrollTop = target.offsetTop - 24;
        this.active = active;
        scrollTopSmooth(document.querySelector('#asynContent').querySelector('.scroll-faker-content'), scrollTop);
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .sync-plan-side-anchor {
    flex: 0 0 200px;
    height: calc(100vh - 160px);
    padding-right: 24px;
    padding-bottom: 60px;
    font-size: 12px;
    line-height: 26px;
    color: #63656e;
    background: #fff;
    border-top: 40px solid rgb(0 0 0 / 60%);
    user-select: none;

    .anchor-wraper {
      min-height: calc(100% - 20px);
      margin-left: 10px;
      border-left: 2px solid #f0f1f5;
    }

    .item-title {
      position: relative;
      padding-left: 13px;
      margin-top: 20px;
      color: #b2b5bd;

      &::before {
        position: absolute;
        top: 50%;
        left: -5px;
        width: 8px;
        height: 8px;
        background: #fff;
        border: 2px solid #c4c6cc;
        border-radius: 50%;
        content: "";
        transform: translateY(-50%);
        box-sizing: border-box;
      }
    }

    .item {
      position: relative;
      display: flex;
      height: 26px;
      padding-left: 26px;
      cursor: pointer;
      transition: all 0.3s linear;

      .anchor-text {
        max-width: 170px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: pre;
      }

      &:hover,
      &.active {
        color: #3a84ff;

        &::before {
          position: absolute;
          top: 0;
          left: -2px;
          width: 2px;
          height: 100%;
          background: #3a84ff;
          content: "";
        }
      }

      &:hover {
        span {
          transform: scale(1.1);
        }
      }

      &.active {
        span {
          transform: scale(1);
        }
      }

      span {
        transform-origin: left center;
        transition: transform 0.2s;
      }
    }
  }
</style>
