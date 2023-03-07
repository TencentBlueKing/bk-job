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
  <div>
    <jb-form-item>
      <bk-checkbox v-model="executeBeforeNotify">
        {{ $t('cron.执行前通知') }}
      </bk-checkbox>
    </jb-form-item>
    <render-info-detail
      v-if="executeBeforeNotify"
      class="notify-wraper"
      left="55">
      <execute-notify
        v-bind="$attrs"
        :form-data="formData"
        mode="execute-beofre"
        :notify-offset-label="$t('cron.执行前')"
        v-on="$listeners" />
    </render-info-detail>
  </div>
</template>
<script>
  import RenderInfoDetail from '../../render-info-detail';
  import ExecuteNotify from '../execute-notify';

  export default {
    name: '',
    components: {
      RenderInfoDetail,
      ExecuteNotify,
    },
    props: {
      formData: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        executeBeforeNotify: false,
      };
    },
    watch: {
      formData: {
        handler(formData) {
          if (this.formData.notifyOffset
            || this.formData.notifyChannel.length > 0
            || this.formData.notifyUser.roleList.length > 0
            || this.formData.notifyUser.userList.length > 0) {
            this.executeBeforeNotify = true;
          }
        },
        immediate: true,
      },
    },
  };
</script>
<style lang="postcss">
  .notify-wraper {
    margin-top: -20px !important;
    margin-bottom: 20px;
  }
</style>
