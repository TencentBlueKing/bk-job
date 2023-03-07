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
  <div class="job-router-view">
    <page-guide v-show="isShowView" />
    <skeleton
      :type="skeletonType"
      :visiable="!isShowView" />
    <permission
      v-if="isNotPermission"
      :auth-result="authResult"
      :class="{ 'permission-pending': !isShowView }" />
    <router-view
      v-if="!isNotPermission"
      ref="routerView"
      :class="{ 'view-pending': !isShowView }" />
  </div>
</template>
<script>
  import EventBus from '@utils/event-bus';

  import PageGuide from './guide';
  import Permission from './permission';
  import Skeleton from './skeleton';

  export default {
    name: 'JbRouterView',
    components: {
      Skeleton,
      Permission,
      PageGuide,
    },
    data() {
      return {
        isShowSkeleton: false,
        isShowView: true,
        skeletonType: '',
        isNotPermission: false,
        authResult: {
          requiredPermissions: [],
        },
      };
    },
    watch: {
      $route: {
        handler() {
          const { meta = {} } = this.$route;
          if (Object.prototype.hasOwnProperty.call(meta, 'skeleton')) {
            this.isShowView = false;
            this.isShowSkeleton = true;
            this.skeletonType = meta.skeleton;
          }

          this.isNotPermission = false;
          this.skeletonTimer = '';
          setTimeout(() => {
            this.init();
          });
        },
        immediate: true,
      },
    },
    created() {
      this.pendingStartTime = Date.now();
      EventBus.$on('permission-page', this.permissionHold);
      this.$once('hook:beforeDestroy', () => {
        EventBus.$off('permission-page', this.permissionHold);
      });
    },
    methods: {
      init() {
        if (!this.$refs.routerView || this.$refs.routerView.isSkeletonLoading === undefined) {
          this.isShowView = true;
          this.isShowSkeleton = false;
          return;
        }

        const startPendingTime = Date.now();
        const unWatch = this.$watch(() => {
          if (!this.$refs.routerView) {
            return false;
          }
          return this.$refs.routerView.isSkeletonLoading;
        }, (isSkeletonLoading) => {
          if (!isSkeletonLoading) {
            const spaceTime = Date.now() - startPendingTime;
            const letterTime = 1000;
            this.skeletonTimer = setTimeout(() => {
              this.isShowView = true;
              unWatch();
            }, spaceTime > letterTime ? 0 : letterTime - spaceTime);
          }
        }, {
          immediate: true,
        });
        this.$once('hook:beforeDestroy', () => {
          unWatch();
          clearTimeout(this.skeletonTimer);
        });
      },
      permissionHold(authResult) {
        this.isNotPermission = true;
        this.authResult = authResult;
      },
    },
  };
</script>
<style lang='postcss'>
  .job-router-view {
    .view-pending {
      max-height: 100%;
      overflow: hidden;
      opacity: 0%;
      visibility: hidden;
    }

    .permission-pending {
      opacity: 0%;
    }
  }
</style>
