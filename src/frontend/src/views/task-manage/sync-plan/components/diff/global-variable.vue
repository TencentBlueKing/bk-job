<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
    :id="`${type}_variable_${data.id}_${data.name}`"
    class="diff-global-variable"
    :class="classes">
    <div class="name">
      <div class="type-flag">
        <icon :type="data.icon" />
      </div>
      <span>{{ data.name }}</span>
    </div>
    <div class="info">
      <div :class="diffValue.type">
        <span class="label">{{ $t('template.变量类型：') }}</span>
        <span class="value">{{ data.typeText }}</span>
      </div>
      <div :class="diffValue.name">
        <span class="label">{{ $t('template.变量名称：') }}</span>
        <span class="value">{{ data.name }}</span>
      </div>
      <div :class="valueDiffClass">
        <span class="label">{{ $t('template.变量值：') }}</span>
        <span class="value">{{ valueText }}</span>
        <host-detail
          v-if="data.isHost"
          class="host-value-detail"
          :data="data.defaultTargetValue"
          :diff-enable="diffValue.defaultTargetValue === 'changed'"
          :name="data.name" />
        <bk-popover
          v-if="isFollowTemplate"
          class="follow-template-popover"
          placement="top"
          :tippy-options="{
            theme: 'light'
          }">
          <icon
            class="follow-template-flag"
            type="global-var-line" />
          <div slot="content">
            <div class="follow-template-tips">
              <div class="follow-template-title">
                {{ $t('template.跟随作业') }}
              </div>
              <div class="follow-template-desc">
                {{ $t('template.跟随作业描述') }}
              </div>
            </div>
          </div>
        </bk-popover>
      </div>
      <div :class="diffValue.description">
        <span class="label">{{ $t('template.变量描述：') }}</span>
        <span class="value">{{ data.description || '-' }}</span>
      </div>
      <div :class="diffValue.changeable">
        <span class="label">{{ $t('template.赋值可变：') }}</span>
        <span class="value">{{ data.changeableText }}</span>
      </div>
      <div :class="diffValue.required">
        <span class="label">{{ $t('template.执行时必填：') }}</span>
        <span class="value">{{ data.requiredText }}</span>
      </div>
    </div>
  </div>
</template>
<script>
  import HostDetail from './host-detail';

  export default {
    name: '',
    components: {
      HostDetail,
    },
    props: {
      data: {
        type: Object,
        required: true,
      },
      diff: {
        type: Object,
        default: () => ({}),
      },
      type: {
        type: String,
        default: '',
      },
      account: {
        type: Array,
        default: () => ([])
      }
    },
    computed: {
      classes() {
        const diffKey = `${this.data.realId}`;
        if (this.diff[diffKey]) {
          return this.diff[diffKey].type;
        }
        return '';
      },
      diffValue() {
        const diffKey = `${this.data.realId}`;
        if (this.diff[diffKey]) {
          return this.diff[diffKey].value || {};
        }
        return {};
      },
      accName() {
        return (val) => {
          const filters = this.account.filter((item) => item.id === Number(val))
          return filters?.[0]?.alias || val
        }
      },
      // 变量值跟随作业（仅执行方案侧有该属性）
      isFollowTemplate() {
        return this.data.followTemplate === 1 && this.type !== 'sync-after';
      },
      valueDiffClass() {
        if (this.data.isHost) {
          return this.diffValue.defaultTargetValue;
        }
        if (this.data.isPassword) {
          return this.diffValue.defaultValueHash;
        }
        return this.diffValue.defaultValue;
      },
      valueText() {
        return this.data.isAccount ? this.accName(this.data.valueText) : this.data.valueText;
      },
    },
  };
</script>
<style lang='postcss'>
  html[lang="en-US"] {
    .diff-global-variable {
      .info {
        .label {
          flex-basis: 115px;
        }
      }
    }
  }

  .diff-global-variable {
    color: #63656e;

    &.new {
      position: relative;

      .name {
        display: flex;
        align-items: center;

        &::after {
          width: 28px;
          height: 14px;
          margin-left: 10px;
          font-size: 12px;
          line-height: 14px;
          color: #fff;
          text-align: center;
          background: #ffa86e;
          content: "new";
        }
      }
    }

    &.delete {
      text-decoration: line-through;
    }

    .changed {
      .value {
        padding: 3px;
        background: #fddfcb;
      }
    }

    .name {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      font-size: 16px;
      font-weight: bold;
      line-height: 24px;
      color: #313238;

      .type-flag {
        display: flex;
        width: 24px;
        height: 24px;
        margin-right: 10px;
        font-size: 17px;
        color: #fff;
        background: #979ba5;
        border-radius: 2px;
        align-items: center;
        justify-content: center;
      }
    }

    .info {
      padding-left: 34px;
      font-size: 0;
      line-height: 32px;
      color: #b2b5bd;

      & > div {
        display: flex;
      }

      .label {
        font-size: 14px;
        flex: 0 0 90px;
      }

      .value {
        min-width: 20px;
        font-size: 14px;
        color: #63656e;
        word-break: break-all;
      }

      .host-value-detail {
        margin-left: 4px;
        font-size: 17px;
        color: #3a84ff;
        cursor: pointer;
      }

      .follow-template-popover {
        display: flex;
        margin-left: 4px;
        align-items: center;

        .follow-template-flag {
          font-size: 16px;
          color: #3a84ff;
          display: block;
        }
      }
    }
  }

  .follow-template-tips {
    .follow-template-title {
      font-size: 12px;
      color: #313238;
    }

    .follow-template-desc {
      margin-top: 4px;
      font-size: 12px;
      line-height: 18px;
      color: #63656e;
    }
  }
</style>
