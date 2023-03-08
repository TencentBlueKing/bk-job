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
  <div class="bussiness-app-empty-page">
    <div class="page-header">
      <div class="header-wraper">
        <div class="page-title">
          {{ $t('暂无业务权限，请先申请或创建') }}
        </div>
        <div class="page-desc">
          {{ $t('作业平台的核心功能包括面向服务器操作系统的命令执行、文件分发，以及将多个操作组合成作业流程，并支持设置定时执行。') }}
        </div>
        <div class="page-action">
          <bk-button
            class="mr10"
            theme="primary"
            @click="handleGoCreateApp">
            {{ $t('新建业务') }}
          </bk-button>
          <bk-button
            :loading="isApplyLoading"
            theme="primary"
            @click="handleGoApplyPermission">
            {{ $t('申请业务权限') }}
          </bk-button>
        </div>
      </div>
    </div>
    <div class="page-wraper">
      <div class="page-container">
        <div class="feature-item">
          <div class="feature-pic">
            <img
              src="/static/images/guide/permission-apply.svg"
              style="width: 220px; margin: 19px 26px 0 24px;">
          </div>
          <div class="feature-box">
            <div class="feature-title">
              {{ $t('申请已有业务权限 / 创建新的业务') }}
            </div>
            <div>
              {{ $t('不同团队在作业平台上的资源以“业务”分隔，而“业务”是统一由配置平台进行创建和管理的，你可以选择') }}
              <a @click="handleGoApplyPermission">
                {{ $t('申请已有业务的权限') }}
              </a>
              {{ $t('，亦或是') }}
              <a
                :href="`${relatedSystemUrls.BK_CMDB_ROOT_URL}/#/resource/business`"
                target="_blank">
                {{ $t('新建') }}
              </a>
              {{ $t('一个全新的业务。') }}
            </div>
          </div>
        </div>
        <div class="divide-line" />
        <div class="feature-item">
          <div class="feature-pic">
            <img
              src="/static/images/guide/permission-use.svg"
              style="width: 230px; margin: 22px 32px 0 7px;">
          </div>
          <div class="feature-box">
            <div class="feature-title">
              {{ $t('开始使用作业平台') }}
            </div>
            <div>
              <span>{{ $t('作业平台的目标服务器信息同样来自') }} </span>
              <a
                :href="relatedSystemUrls.BK_CMDB_ROOT_URL"
                target="_blank">
                {{ $t('配置平台') }}
              </a>
              <span>{{ $t('，你需将服务器信息预先录入；并且，服务器上需要') }}</span>
              <a
                :href="relatedSystemUrls.BK_NODEMAN_ROOT_URL"
                target="_blank">
                {{ $t('安装好蓝鲸 GSE Agent') }}
              </a>
              <span>{{ $t('才能够正常接收来自作业平台的操作指令。') }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="page-link">
        <div>
          <span>{{ $t('第一次使用作业平台？点击查阅') }}  </span>
          <!-- eslint-disable-next-line  max-len -->
          <a
            :href="`${relatedSystemUrls.BK_DOC_CENTER_ROOT_URL}/markdown/作业平台/产品白皮书/Quick-Starts/1.Create-system-account.md`"
            target="_blank">
            <span>{{ $t('快速入门技巧') }}</span>
            <icon type="link" />
          </a>
        </div>
        <div style="margin-top: 10px;">
          <span>{{ $t('了解更多关于作业平台产品的功能介绍，点击前往') }}</span>
          <a
            :href="`${relatedSystemUrls.BK_DOC_JOB_ROOT_URL}/markdown/作业平台/产品白皮书/Introduction/What-is-Job.md`"
            target="_blank">
            <span>{{ $t('产品文档') }}</span>
            <icon type="link" />
          </a>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
  import QueryGlobalSettingService from '@service/query-global-setting';

  import I18n from '@/i18n';

  export default {
    data() {
      return {
        isApplyLoading: false,
        relatedSystemUrls: {
          BK_CMDB_ROOT_URL: '',
          BK_DOC_JOB_ROOT_URL: '',
          BK_DOC_CENTER_ROOT_URL: '/',
        },
      };
    },
    created() {
      this.fetchRelatedSystemUrls();
      document.title = I18n.t('无业务权限');
    },
    methods: {
      fetchRelatedSystemUrls() {
        QueryGlobalSettingService.fetchRelatedSystemUrls()
          .then((data) => {
            this.relatedSystemUrls = Object.freeze(data);
          });
      },
      handleGoCreateApp() {
        if (!this.relatedSystemUrls.BK_CMDB_ROOT_URL) {
          alert(I18n.t('网络错误，请刷新页面重试'));
          return;
        }
        window.open(`${this.relatedSystemUrls.BK_CMDB_ROOT_URL}/#/resource/business`);
      },
      handleGoApplyPermission() {
        this.isApplyLoading = true;
        QueryGlobalSettingService.fetchApplyBusinessUrl({
          scopeType: window.PROJECT_CONFIG.SCOPE_TYPE,
          scopeId: window.PROJECT_CONFIG.SCOPE_ID,
          resourceId: window.PROJECT_CONFIG.SCOPE_ID,
        }).then((data) => {
          window.open(data);
        })
          .finally(() => {
            this.isApplyLoading = false;
          });
      },
    },
  };
</script>
<style lang='postcss'>
  .bussiness-app-empty-page {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    background: #fff;

    .page-header {
      padding: 40px 0 30px;
      background: #f5f6fa;

      .header-wraper {
        width: 1175px;
        margin: 0 auto;
      }
    }

    .page-wraper {
      width: 1175px;
      margin: 0 auto;
    }

    .page-title {
      font-size: 20px;
      line-height: 26px;
      color: #313238;
    }

    .page-desc {
      margin-top: 12px;
      font-size: 13px;
      line-height: 24px;
      color: #63656e;
    }

    .page-container {
      display: flex;
      margin-top: 20px;
    }

    .feature-item {
      display: flex;
      flex: 1;
      justify-content: space-between;
      height: 236px;
      padding-right: 35px;
      font-size: 12px;
      line-height: 24px;
      color: #63656e;

      .feature-box {
        width: 275px;
      }

      .feature-title {
        margin-top: 42px;
        margin-bottom: 16px;
        font-size: 16px;
        line-height: 21px;
        color: #313238;
      }
    }

    .divide-line {
      width: 1px;
      height: 160px;
      margin-top: 42px;
      background: #dcdee5;
    }

    .page-action {
      margin-top: 30px;
    }

    .page-link {
      margin-top: 60px;
      font-size: 12px;
      line-height: 16px;
      color: #979ba5;
    }
  }
</style>
