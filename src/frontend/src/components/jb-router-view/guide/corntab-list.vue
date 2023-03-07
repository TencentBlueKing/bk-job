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
  <div class="crontab-list-empty-page">
    <div class="page-header">
      <div class="header-wraper">
        <div class="page-title">
          {{ $t('当前业务暂无定时任务，请先创建') }}
        </div>
        <div class="page-desc">
          {{ $t('采用类Unix Crontab的配置表达式风格，降低使用门槛更快上手！提供了执行前/后通知提醒、执行记录等辅助功能，让用户可以放心地把定期执行的任务交给Job！') }}
        </div>
        <div class="page-action">
          <bk-button
            theme="primary"
            @click="handleCreateCron">
            {{ $t('新建定时任务') }}
          </bk-button>
        </div>
      </div>
    </div>
    <div class="page-wraper">
      <div class="page-container">
        <div class="feature-item">
          <div class="feature-pic">
            <img
              src="/static/images/guide/cron-plan.svg"
              style="width: 220px; margin: 24px 25px 0;">
          </div>
          <div class="feature-box">
            <div class="feature-title">
              {{ $t('选择需要定期执行的作业') }}
            </div>
            <div>{{ $t('定时任务关联的是“作业执行方案”，且支持在配置时按需传入定时场景的全局变量值；如果关联的作业执行方案有更新，也会在相关的操作流程中给予提醒和确认，确保不会受到影响。') }} </div>
          </div>
        </div>
        <div class="divide-line" />
        <div class="feature-item">
          <div class="feature-pic">
            <img
              src="/static/images/guide/cron-clock.svg"
              style="width: 246px; margin: 21px 8px 0 16px;">
          </div>
          <div class="feature-box">
            <div class="feature-title">
              {{ $t('设置定时模式和提醒机制') }}
            </div>
            <div>{{ $t('支持“周期性”和“一次性”的定期执行模式， “周期性”采用类Linux Crontab表达式的配置方式，支持设置“结束时间”；“一次性”模式可以通过配置执行前的通知提醒，避免时间间隔周期长，计划有变动。') }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
  export default {
    methods: {
      handleCreateCron() {
        this.$router.push({
          name: 'cronList',
          query: {
            mode: 'create',
            stamp: Date.now(),
          },
        });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .crontab-list-empty-page {
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
      margin-top: 30px;
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
      margin-top: 40px;
      font-size: 12px;
      line-height: 16px;
      color: #979ba5;
    }
  }
</style>
