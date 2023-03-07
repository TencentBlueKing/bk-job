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
  <div class="dashboard-page">
    <div class="operation-bar">
      <div class="app-select">
        <bk-button
          theme="primary"
          @click="handleScreenshot">
          {{ $t('dashboard.截图') }}
        </bk-button>
      </div>
      <div class="date-setting">
        <div class="date-info">
          <p>{{ $t('dashboard.数据初始时间') }}：{{ dateInfo.STATISTICS_DATA_START_DATE }}</p>
          <p>{{ $t('dashboard.最近更新时间') }}：{{ dateInfo.STATISTICS_DATA_UPDATE_TIME }}</p>
        </div>
        <bk-date-picker
          class="date-picker"
          :clearable="false"
          :placeholder="$t('dashboard.选择日期')"
          :value="date"
          @change="handleDateChange" />
      </div>
    </div>
    <scroll-faker
      v-if="!isLoading"
      ref="scroll"
      class="dashboard-container"
      style="height: calc(100vh - 161px);">
      <div
        ref="content"
        class="dashboard-wraper">
        <div class="section-block">
          <div class="section-title">
            {{ $t('dashboard.业务类') }}
          </div>
          <div class="section-content">
            <div class="content-left">
              <app-dashboard :date="date" />
            </div>
            <div class="content-right">
              <platform-dashboard :date="date" />
            </div>
          </div>
        </div>
        <lower-component>
          <div class="section-block">
            <div class="section-title">
              {{ $t('dashboard.资源类') }}
            </div>
            <div class="section-content">
              <div class="content-left">
                <template-dashboard :date="date" />
              </div>
              <div class="content-right">
                <script-dashboard :date="date" />
              </div>
            </div>
          </div>
        </lower-component>
        <lower-component>
          <div class="section-block">
            <div class="section-content">
              <div class="content-left">
                <crontab-dashboard :date="date" />
              </div>
              <div class="content-center">
                <tag-dashboard :date="date" />
              </div>
              <div class="content-right">
                <account-dashboard :date="date" />
              </div>
            </div>
          </div>
        </lower-component>
        <lower-component>
          <div class="section-block">
            <div class="section-title">
              {{ $t('dashboard.执行类') }}
            </div>
            <div class="section-content">
              <execute-dashboard :date="date" />
            </div>
          </div>
        </lower-component>
      </div>
    </scroll-faker>
  </div>
</template>
<script>
  import html2canvas from 'html2canvas';

  import StatisticsService from '@service/statistics';

  import { prettyDateFormat } from '@utils/assist';

  import AccountDashboard from './components/account';
  import AppDashboard from './components/app';
  import CrontabDashboard from './components/crontab';
  import ExecuteDashboard from './components/execute';
  import PlatformDashboard from './components/platform';
  import ScriptDashboard from './components/script';
  import TagDashboard from './components/tag';
  import TemplateDashboard from './components/template';

  export default {
    name: '',
    components: {
      AppDashboard,
      PlatformDashboard,
      TemplateDashboard,
      ScriptDashboard,
      CrontabDashboard,
      TagDashboard,
      AccountDashboard,
      ExecuteDashboard,
    },
    data() {
      return {
        isLoading: true,
        date: prettyDateFormat(Date.now()),
        dateInfo: {},
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.isLoading;
      },
    },
    created() {
      this.fetchDateInfo();
    },
    methods: {
      fetchDateInfo() {
        this.isLoading = true;
        StatisticsService.fetchDateInfo({}, {
          permission: 'page',
        }).then((data) => {
          this.dateInfo = data;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleScreenshot() {
        const {
          top,
          width,
          height,
        } = this.$refs.content.getBoundingClientRect();
        const {
          scrollTop,
        } = this.$refs.scroll.getScroll();
        const scrollY = scrollTop > 0 ? top : 0;
        html2canvas(this.$refs.content, {
          width,
          height,
          windowHeight: height,
          scrollY,
          backgroundColor: '#f5f6fa',
        }).then((canvas) => {
          const a = document.createElement('a');
          a.href = canvas.toDataURL('image/png');
          a.download = this.title;
          a.download = `${document.title}_${this.date}`;
          a.click();
        });
      },
      handleDateChange(date) {
        this.date = date;
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .dashboard-page {
    .operation-bar {
      position: relative;
      z-index: 1;
      display: flex;
      align-items: center;
      height: 56px;
      padding: 0 24px;
      background: #fff;
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 10%);

      .app-select {
        display: flex;
        align-items: center;

        .app-list {
          width: 360px;
          background: #f0f1f5;
          border: none;
        }

        .app-action-btn {
          display: flex;
          width: 30px;
          height: 30px;
          margin-left: 10px;
          font-size: 14px;
          color: #979ba5;
          cursor: pointer;
          background: #f0f1f5;
          border-radius: 2px;
          align-items: center;
          justify-content: center;
        }
      }

      .date-setting {
        display: flex;
        align-items: center;
        margin-left: auto;

        .date-info {
          font-size: 12px;
          color: #c4c6cc;
          transform-origin: right center;
          transform: scale(0.85);
        }

        .date-picker {
          width: 180px;
          margin-left: 16px;

          .bk-date-picker-rel .bk-date-picker-editor {
            background: #f0f1f5;
            border-color: transparent;
          }
        }
      }
    }

    .dashboard-container {
      .dashboard-wraper {
        padding: 20px 24px;
      }
    }

    .section-block {
      display: flex;
      flex-direction: column;

      &:nth-child(n + 2) {
        padding-top: 30px;
      }

      .section-title {
        margin-bottom: 16px;
        font-size: 16px;
        line-height: 24px;
        color: #313238;
      }

      .section-content {
        display: flex;

        .content-left,
        .content-center,
        .content-right {
          display: flex;
          background: #fff;
          box-shadow: 0 1px 2px 0 rgb(0 0 0 / 10%);

          & > * {
            flex: 1;
          }
        }

        .content-left {
          flex: 0 0 auto;
        }

        .content-center {
          flex: 1 0 auto;
          margin-left: 20px;

          & ~ .content-right {
            flex: 0;
          }
        }

        .content-right {
          flex: 1 1 auto;
          margin-left: 20px;
        }
      }
    }
  }
</style>
