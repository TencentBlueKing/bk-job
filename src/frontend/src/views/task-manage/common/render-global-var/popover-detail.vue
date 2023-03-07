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
    ref="detail"
    class="global-variable-popover-detail"
    :class="classes"
    :style="styles">
    <div class="wraper">
      <div class="header">
        <span>{{ data.name }}</span>
        <span
          v-if="editOfPlan && !selectValue.includes(data.name)"
          class="tag">
          {{ $t('template.未引用') }}
        </span>
      </div>
      <detail-layout>
        <detail-item :label="$t('template.变量类型：')">
          {{ data.typeText }}
        </detail-item>
        <detail-item :label="`${defaultField}：`">
          {{ data.valueText }}
        </detail-item>
        <detail-item :label="$t('template.变量描述：')">
          {{ data.description || '--' }}
        </detail-item>
        <detail-item :label="$t('template.执行时必填：')">
          {{ data.requiredText }}
        </detail-item>
        <detail-item :label="$t('template.赋值可变：')">
          {{ data.changeableText }}
        </detail-item>
      </detail-layout>
    </div>
  </div>
</template>
<script>
  import DetailLayout from '@components/detail-layout';
  import DetailItem from '@components/detail-layout/item';

  import I18n from '@/i18n';

  export default {
    name: 'GlobalVariablePopoverDetail',
    components: {
      DetailLayout,
      DetailItem,
    },
    props: {
      data: {
        type: Object,
        required: true,
      },
      selectValue: {
        type: Array,
        required: true,
      },
      editOfPlan: {
        type: Boolean,
        DEFAULT: false,
      },
      defaultField: {
        type: String,
        default: I18n.t('template.初始值'),
      },
    },
    data() {
      return {
        position: 'left',
        width: 300,
        top: 0,
        left: 0,
      };
    },
    computed: {
      classes() {
        return {
          [`arrow-position-${this.position}`]: true,
        };
      },
      styles() {
        return {
          position: 'absolute',
          top: `${this.top}px`,
          left: `${this.left}px`,
          'z-index': window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
          width: `${this.width}px`,
        };
      },
    },
    mounted() {
      this.init();
    },
    beforeDestroy() {
      try {
        if (this.$refs.detail && document.body.hasChildNodes(this.$refs.detail)) {
          document.body.removeChild(this.$refs.detail);
        }
      } catch (error) {
        console.log(error);
      }
    },
    methods: {
      init() {
        const windowWidth = window.innerWidth;
        const $target = document.querySelector(`#globalVariableWithName_${this.data.name}`);
        const { top, left } = $target.getBoundingClientRect();
        this.top = top + 50;
        this.left = left - 11;
        if (windowWidth <= left + this.width) {
          this.left = left - 150;
          this.position = 'right';
        }
        document.body.appendChild(this.$refs.detail);
      },
    },
  };
</script>
<style lang='postcss'>
  .global-variable-popover-detail {
    border: 1px solid #dcdee5;
    border-radius: 2px;
    box-shadow: 0 0 5px 0 rgb(0 0 0 / 9%);

    &.arrow-position-right {
      &::before {
        right: 128px;
        left: unset;
      }
    }

    &::before {
      position: absolute;
      top: -5px;
      left: 20px;
      width: 11px;
      height: 11px;
      background: #fff;
      border: 1px solid #dcdee5;
      content: "";
      transform: rotateZ(45deg);
      box-shadow: 0 0 5px 0 rgb(0 0 0 / 9%);
    }

    .wraper {
      position: relative;
      padding: 12px 13px;
      background: #fff;
      border-radius: 2px;

      .header {
        padding-bottom: 8px;
        margin-bottom: 8px;
        font-size: 14px;
        color: #313238;
        border-bottom: 1px solid #f0f1f5;

        .tag {
          display: inline-block;
          height: 18px;
          padding: 0 6px;
          font-size: 12px;
          line-height: 18px;
          color: #979ba5;
          background-color: #f0f1f5;
          border-radius: 2px;
        }
      }

      .detail-item {
        margin-bottom: 0;
        font-size: 12px;
        line-height: 20px;
      }

      .detail-label {
        color: #b2b5bd;
      }

      .detail-content {
        /* stylelint-disable value-no-vendor-prefix */
        display: -webkit-box;
        max-height: 60px;
        overflow: hidden;
        text-overflow: ellipsis;
        word-break: break-all;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
      }
    }
  }
</style>
