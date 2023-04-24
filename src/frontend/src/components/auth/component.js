/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

import PermissionCheckService from '@service/permission-check';

import './style.css';
import {
  permissionDialog,
} from '@/common/bkmagic';

export default {
  props: {
    permission: {
      type: [Boolean, String],
      default: '',
    },
    auth: {
      type: String,
      required: true,
    },
    resourceId: {
      type: [
        Number, String,
      ],
    },
    scopeType: String,
    scopeId: String,
  },
  data() {
    return {
      hasPermission: false,
    };
  },
  computed: {
    showRaw() {
      if (this.permission) {
        return true;
      }
      if (this.hasPermission) {
        return true;
      }
      return false;
    },
  },
  watch: {
    resourceId(resourceId) {
      if (!resourceId) {
        return;
      }
      this.checkPermission();
    },
  },
  created() {
    this.checkPermission();
    this.authResult = {};
  },
  methods: {
    /**
         * @desc 主动鉴权，指定资源和资源权限
        */
    fetchPermission() {
      this.isLoading = true;
      PermissionCheckService.fetchPermission({
        operation: this.auth,
        resourceId: this.resourceId,
        // appId: window.PROJECT_CONFIG.APP_ID,
        scopeType: window.PROJECT_CONFIG.SCOPE_TYPE,
        scopeId: window.PROJECT_CONFIG.SCOPE_ID,
        returnPermissionDetail: true,
      })
        .then((data) => {
          this.hasPermission = data.pass;
          this.authResult = data;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
    /**
         * @desc 判断预鉴权逻辑
        */
    checkPermission() {
      if (this.permission === '' && this.auth) {
        this.fetchPermission();
      }
    },
    /**
         * @desc 无权限时弹框提示资源权限申请
        */
    handleCheckPermission(event) {
      event.stopPropagation();
      permissionDialog({
        operation: this.auth,
        resourceId: this.resourceId,
        scopeType: this.scopeType,
        scopeId: this.scopeId,
      }, this.authResult);
    },
  },

  render(h) {
    if (this.showRaw) {
      if (this.$slots.default) {
        return this.$slots.default[0];
      }
    }
    if (this.$slots.forbid) {
      return h('div', {
        class: {
          'component-permission-disabled': true,
        },
        on: {
          click: this.handleCheckPermission,
        },
        directives: [
          {
            name: 'cursor',
          },
        ],
      }, this.$slots.forbid);
    }
    return this._e(); // eslint-disable-line no-underscore-dangle
  },
};
