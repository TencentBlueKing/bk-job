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
    class="cron-job"
    :class="[
      { 'is-error': isError },
      `error-${errorField}`,
      `select-${selectIndex}`,
    ]">
    <div class="time-describe">
      <span
        class="time-text minute"
        @click="handleTimeTextChange('minute')">{{ $t('cron.分') }}</span>
      <span
        class="time-text hour"
        @click="handleTimeTextChange('hour')">{{ $t('cron.时') }}</span>
      <span
        class="time-text dayOfMonth"
        @click="handleTimeTextChange('dayOfMonth')">{{ $t('cron.日') }}</span>
      <span
        class="time-text month"
        @click="handleTimeTextChange('month')">{{ $t('cron.月') }}</span>
      <span
        class="time-text dayOfWeek"
        @click="handleTimeTextChange('dayOfWeek')">{{ $t('cron.周') }}</span>
    </div>
    <div class="time-input">
      <input
        ref="input"
        class="input"
        type="text"
        :value="nativeValue"
        @blur="handleBlur"
        @input="handleInput"
        @keyup.left="handleSelectText"
        @keyup.right="handleSelectText"
        @mousedown="handleSelectText">
    </div>
    <div
      v-if="parseValue.length > 1"
      class="time-parse">
      <template v-if="parseValue[0]">
        <span class="month">{{ parseValue[0] }}</span>
      </template>
      <template v-if="parseValue[1]">
        <span class="dayOfMonth">{{ parseValue[1] }}</span>
        <span v-if="parseValue[2]">以及当月</span>
      </template>
      <template v-if="parseValue[2]">
        <span class="dayOfWeek">{{ parseValue[2] }}</span>
      </template>
      <template v-if="parseValue[3]">
        <span class="hour">{{ parseValue[3] }}</span>
      </template>
      <span class="minute">{{ parseValue[4] }}</span>
    </div>
    <div
      v-if="nextTime.length > 0"
      class="time-next"
      :class="{ active: isTimeMore }">
      <div class="label">
        {{ $t('cron.下次：') }}
      </div>
      <div class="value">
        <div
          v-for="(time, index) in nextTime"
          :key="`${time}_${index}`">
          {{ time }}
        </div>
      </div>
      <div
        class="arrow"
        @click="handleShowMore">
        <icon
          class="arrow-button"
          type="angle-double-down" />
      </div>
    </div>
  </div>
</template>
<script>
  import CronExpression from 'cron-parser-custom';
  import _ from 'lodash';

  import { prettyDateTimeFormat } from '@utils/assist';
  import Translate from '@utils/cron/translate';

  const labelIndexMap = {
    minute: 0,
    hour: 1,
    dayOfMonth: 2,
    month: 3,
    dayOfWeek: 4,
    0: 'minute',
    1: 'hour',
    2: 'dayOfMonth',
    3: 'month',
    4: 'dayOfWeek',
  };

  export default {
    name: '',
    props: {
      value: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        selectIndex: '',
        nativeValue: this.value,
        nextTime: [],
        parseValue: [],
        errorField: '',
        isError: false,
        isTimeMore: false,
      };
    },
    mounted() {
      if (!this.nativeValue) {
        return;
      }
      this.checkAndTranslate(this.nativeValue);
    },
    methods: {
      /**
       * @desc 检测crontab格式和翻译
       */
      checkAndTranslate(value) {
        const interval = CronExpression.parse(`0 ${value.trim()}`, {
          currentDate: new Date(),
        });

        let i = 5;
        this.nextTime = [];
        while (i > 0) {
          this.nextTime.push(prettyDateTimeFormat(interval.next().toString()));
          i -= 1;
        }

        this.errorField = '';
        this.isError = false;
        this.parseValue = Translate(value);
      },
      /**
       * @desc 选中crontab字段
       * @param {String} lable 选中的字段名
       */
      handleTimeTextChange(label) {
        if (!this.nativeValue) {
          return;
        }
        const timeItem = this.nativeValue.split(' ');
        const index = labelIndexMap[label];
        if (timeItem.length < index) {
          return;
        }
        const preStrLength = timeItem.slice(0, index).join('').length + index;
        const endPosition = preStrLength + timeItem[index].length;
        setTimeout(() => {
          this.selectIndex = label;
          this.$refs.input.focus();
          this.$refs.input.selectionStart = preStrLength;
          this.$refs.input.selectionEnd = endPosition;
        });
      },
      /**
       * @desc 输入框失去焦点
       */
      handleBlur() {
        this.selectIndex = '';
      },
      /**
       * @desc 选中输入框文本
       * @param {Object} event 文本选择事件
       */
      handleSelectText(event) {
        const $target = event.target;
        const value = _.trim($target.value);
        this.nativeValue = value;
        if (!value) return;
        setTimeout(() => {
          const cursorStart = $target.selectionStart;
          const cursorStr = value.slice(0, cursorStart);
          const checkBackspce = cursorStr.match(/ /g);
          if (checkBackspce) {
            this.selectIndex = labelIndexMap[checkBackspce.length];
          } else {
            this.selectIndex = labelIndexMap['0'];
          }
        });
      },
      /**
       * @desc 输入框输入
       * @param {Object} event 输入框input事件
       */
      handleInput: _.debounce(function (event) {
        const { value } = event.target;
        this.nativeValue = value;

        try {
          this.checkAndTranslate(value);
          this.$emit('change', value);
          this.$emit('input', value);
        } catch (error) {
          this.parseValue = [];
          this.nextTime = [];
          const all = [
            'minute',
            'hour',
            'dayOfMonth',
            'month',
            'dayOfWeek',
          ];
          if (all.includes(error.message)) {
            this.errorField = error.message;
          }
          this.isError = true;
          this.$emit('change', '');
          this.$emit('input', '');
        }
      }, 200),
      /**
       * @desc 展示下次执行时间列表
       */
      handleShowMore() {
        this.isTimeMore = !this.isTimeMore;
      },
    },
  };
</script>
<style lang='postcss'>
  .cron-job {
    &.is-error {
      .time-input {
        .input {
          border-color: #ff5656;
        }
      }
    }

    /* stylelint-disable selector-class-pattern */
    &.error-month .month,
    &.error-dayOfMonth .dayOfMonth,
    &.error-dayOfWeek .dayOfWeek,
    &.error-hour .hour,
    &.error-minute .minute {
      color: #ff5656 !important;
    }

    &.select-month .month,
    &.select-dayOfMonth .dayOfMonth,
    &.select-dayOfWeek .dayOfWeek,
    &.select-hour .hour,
    &.select-minute .minute {
      color: #3a84ff;
    }

    .time-describe {
      display: flex;
      justify-content: center;
    }

    .time-text {
      padding: 0 19px;
      font-size: 12px;
      line-height: 22px;
      color: #c4c6cc;
      cursor: pointer;
      transition: all 0.1s;

      &.active {
        color: #3a84ff;
      }

      &.field-error {
        color: #ff5656;
      }
    }

    .time-input {
      .input {
        width: 100%;
        height: 48px;
        padding: 0 30px;
        font-size: 24px;
        line-height: 48px;
        word-spacing: 30px;
        color: #63656e;
        text-align: center;
        border: 1px solid #3a84ff;
        border-radius: 2px;
        outline: none;

        &::selection {
          color: #3a84ff;
          background: transparent;
        }
      }
    }

    .time-parse {
      padding: 10px 0;
      margin-top: 8px;
      line-height: 18px;
      color: #63656e;
      text-align: center;
    }

    .time-next {
      display: flex;
      height: 18px;
      overflow: hidden;
      font-size: 12px;
      line-height: 18px;
      color: #979ba5;
      text-align: center;
      transition: height 0.2s linear;
      align-content: center;
      justify-content: center;

      &.active {
        height: 90px;

        .arrow {
          align-items: flex-end;

          .arrow-button {
            transform: rotateZ(-180deg);
          }
        }
      }

      .value {
        text-align: left;
      }

      .arrow {
        display: flex;
        padding-top: 2px;
        padding-bottom: 2px;
        padding-left: 2px;
        font-size: 12px;
        cursor: pointer;
      }
    }
  }
</style>
