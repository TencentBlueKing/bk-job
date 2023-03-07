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
  <div class="render-notify-way-check">
    <bk-checkbox
      class="check-all"
      v-bind="allCheckStatus"
      @click.native="handleCheckToggle">
      {{ $t('notify.全选') }}
    </bk-checkbox>
    <bk-checkbox-group
      class="check-item"
      :value="localValue"
      @change="handleChange">
      <bk-checkbox
        v-for="(channelItem) in channelList"
        :key="channelItem.code"
        :value="channelItem.code">
        {{ channelItem.name }}
      </bk-checkbox>
    </bk-checkbox-group>
  </div>
</template>
<script>
  export default {
    name: '',
    props: {
      channelList: {
        type: Array,
        default: () => [],
      },
      value: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        localValue: [],
      };
    },
    computed: {
      /**
       * @desc 全选状态
       * @returns { Object }
       */
      allCheckStatus() {
        if (this.localValue.length < 1) {
          return {
            checked: false,
            indeterminate: false,
          };
        }
        const allChannelList = this.channelList.map(({ code }) => code);
        let checked = true;
        let indeterminate = false;
        allChannelList.forEach((item) => {
          if (!this.localValue.includes(item)) {
            checked = false;
          }
        });
        indeterminate = !checked;
        if (allChannelList.length < 1) {
          checked = false;
          indeterminate = false;
        }
        return {
          checked,
          indeterminate,
        };
      },
    },
    watch: {
      value: {
        handler(value) {
          this.localValue = value;
        },
        immediate: true,
      },
    },
    methods: {
      handleCheckToggle() {
        if (this.allCheckStatus.checked) {
          this.handleChange([]);
        } else {
          const allChannelCode = this.channelList.map(({ code }) => code);
          this.handleChange(allChannelCode);
        }
      },
      handleChange(value) {
        this.localValue = value;
        this.$emit('on-change', this.localValue);
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .render-notify-way-check {
    display: flex;
    align-items: center;

    .check-all {
      flex: 0 0 auto;
      margin-right: 25px;
    }

    .check-item {
      flex: 0 0 auto;
    }
  }
</style>
