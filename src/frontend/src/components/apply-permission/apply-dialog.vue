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
  <jb-dialog
    v-model="isShowDialog"
    class="apply-permission-dialog"
    :esc-close="false"
    :mask-close="false"
    :width="768">
    <ask-permission
      v-if="isShowDialog"
      :loading="isLoading"
      :max-height="360"
      :permission-list="permissionList" />
    <template #footer>
      <template v-if="!isLoading">
        <bk-button
          v-if="isAppleFlag"
          class="mr10"
          theme="primary"
          @click="handleApply">
          {{ applyText }}
        </bk-button>
        <bk-button
          v-else
          class="mr10"
          theme="primary"
          @click="handleHasApplyed">
          {{ appliedText }}
        </bk-button>
      </template>
      <bk-button @click="handleCancle">
        {{ cancelText }}
      </bk-button>
    </template>
  </jb-dialog>
</template>
<script>
  import PermissionCheckService from '@service/permission-check';

  import AskPermission from './index';

  import I18n from '@/i18n';

  export default {
    components: {
      AskPermission,
    },
    data() {
      return {
        isLoading: false,
        isShowDialog: false,
        isAppleFlag: true,
        authParams: null,
        authResult: {},
      };
    },
    computed: {
      permissionList() {
        if (this.isLoading) {
          return [];
        }
        if (this.authResult.requiredPermissions) {
          return this.authResult.requiredPermissions;
        }
        return [];
      },
    },
    created() {
      this.applyText = I18n.t('去申请');
      this.appliedText = I18n.t('已申请');
      this.cancelText = I18n.t('取消');
    },
    methods: {
      /**
       * @desc 申请资源权限
       */
      fetchPermission() {
        this.isLoading = true;
        PermissionCheckService.fetchPermission({
          ...this.authParams,
          scopeType: this.authParams.scopeType || window.PROJECT_CONFIG.SCOPE_TYPE,
          scopeId: this.authParams.scopeId || window.PROJECT_CONFIG.SCOPE_ID,
          returnPermissionDetail: true,
        }).then((data) => {
          this.authResult = data;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 供外部调用，显示权限申请弹框
       */
      show() {
        this.isShowDialog = true;
        if (this.authParams && !this.authResult.requiredPermissions) {
          this.fetchPermission();
        }
      },
      /**
       * @desc 跳转权限中心
       */
      handleApply() {
        window.open(this.authResult.applyUrl, '_blank');
        this.isAppleFlag = false;
      },
      /**
       * @desc 权限已申请刷新页面
       */
      handleHasApplyed() {
        this.handleCancle();
        window.location.reload();
      },
      handleCancle() {
        this.isAppleFlag = true;
        this.isShowDialog = false;
        this.authResult = {};
      },
    },
  };
</script>
