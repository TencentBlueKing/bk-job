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
  <div class="apply-permission-page">
    <div class="page-main">
      <ask-permission :permission-list="authResult.requiredPermissions" />
      <div class="footer">
        <bk-button
          v-if="isAppleFlag"
          class="mr10"
          theme="primary"
          @click="handleApply">
          {{ applyText }}
        </bk-button>
        <bk-button
          v-else
          theme="primary"
          @click="handleReload">
          {{ appliedText }}
        </bk-button>
      </div>
    </div>
  </div>
</template>
<script>
  import I18n from '@/i18n';

  import AskPermission from './index';

  export default {
    components: {
      AskPermission,
    },
    props: {
      authResult: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isAppleFlag: true,
      };
    },
    created() {
      this.applyText = I18n.t('去申请');
      this.appliedText = I18n.t('已申请');
    },
    methods: {
      /**
       * @desc 跳转权限中心
       */
      handleApply() {
        window.open(this.authResult.applyUrl, '_blank');
        this.isAppleFlag = false;
      },
      /**
       * @desc 已申请刷新页面
       */
      handleReload() {
        location.reload();
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .apply-permission-page {
    .page-main {
      width: 768px;
      padding: 24px;
      margin: 60px auto;
      background-color: #fff;
      border-radius: 2px;
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 5%);
    }

    .footer {
      margin: 24px auto 6px;
      text-align: center;
    }
  }
</style>
