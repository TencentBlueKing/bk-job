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
  <div class="push-speed-limit">
    <jb-form-item :label="label">
      <div class="speed-limit-wraper">
        <div class="speed-limit-content form-item-content">
          <bk-checkbox
            :value="enabled"
            @change="handleEnableChange">
            {{ $t('启用限速') }}
          </bk-checkbox>
          <bk-input
            v-show="enabled"
            class="speed-limit-input"
            :min="0"
            type="number"
            :value="formData[field]"
            @change="handleChange">
            <template slot="append">
              <div class="group-text">
                MB/s
              </div>
            </template>
          </bk-input>
        </div>
        <icon
          v-show="enabled"
          v-bk-tooltips="speedLimitTipsConfig"
          class="tips-flag"
          type="info" />
      </div>
    </jb-form-item>
    <div
      id="targetPathTips"
      class="speed-limit-tips">
      <div class="row">
        {{ $t('请根据机器的网卡情况酌情配置速率，以免影响其他服务的正常使用；') }}
      </div>
      <div class="row">
        {{ $t('未开启时，将按 Agent 默认配置规则限速 （Agent会根据机器资源使用情况，有自身保护机制）') }}
      </div>
    </div>
  </div>
</template>
<script>
  import I18n from '@/i18n';

  export default {
    name: '',
    props: {
      field: {
        type: String,
        required: true,
      },
      label: {
        type: String,
        default: I18n.t('限速'),
      },
      formData: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        enabled: false,
      };
    },
    watch: {
      formData: {
        handler(formData) {
          if (formData[this.field] > 0) {
            this.enabled = true;
          }
        },
        immediate: true,
      },
    },
    created() {
      this.speedLimitTipsConfig = {
        allowHTML: true,
        width: '325px',
        theme: 'light',
        trigger: 'mouseenter',
        content: '#targetPathTips',
        placement: 'right-start',
      };
    },
    methods: {
      handleEnableChange(enabled) {
        this.enabled = enabled;
        this.$emit('on-change', this.field, enabled ? 10 : 0);
      },
      handleChange(value) {
        this.$emit('on-change', this.field, value);
      },
    },
  };
</script>
<style lang='postcss'>
  .push-speed-limit {
    .bk-label {
      white-space: normal;
    }

    .speed-limit-wraper {
      display: flex;
      align-items: center;
      justify-content: flex-start;
    }

    .speed-limit-content {
      display: flex;
      align-items: center;
      height: 32px;
    }

    .speed-limit-input {
      width: calc(100% - 100px);
      margin-left: auto;
    }

    .tips-flag {
      margin-left: 8px;
      font-size: 14px;
      line-height: 32px;
      color: #c4c6cc;
      cursor: pointer;
    }
  }

  .speed-limit-tips {
    font-size: 12px;
    line-height: 16px;
    color: #63656e;

    .row {
      position: relative;
      padding-left: 12px;

      &::before {
        position: absolute;
        top: 6px;
        left: 0;
        width: 4px;
        height: 4px;
        background: currentcolor;
        border-radius: 50%;
        content: "";
      }
    }
  }
</style>
