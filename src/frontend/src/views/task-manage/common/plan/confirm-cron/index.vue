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
  <div
    class="plan-manage-confirm-cron"
    :empty="isEmpty"
    :loading="isLoading">
    <div class="layout-left">
      <scroll-faker>
        <div
          v-for="cronJobItem in timeTaskList"
          :key="cronJobItem.id"
          class="cron-job-tab"
          :class="{ active: cronJobItem.id === currentTaskId }">
          <auth-component
            auth="cron/view"
            :permission="cronJobItem.canManage"
            :resource-id="cronJobItem.id">
            <div
              class="tab-container"
              @click="handleTabChange(cronJobItem.id)">
              <span class="job-name">{{ cronJobItem.name }}</span>
              <icon
                v-if="cronJobItem.hasConfirm"
                class="job-check"
                type="check" />
              <div @click.stop="">
                <bk-switcher
                  class="job-switch"
                  size="small"
                  theme="primary"
                  :value="cronJobItem.enable"
                  @change="value => handleEnableChange(cronJobItem.id, value)" />
              </div>
            </div>
            <div
              slot="forbid"
              class="tab-container">
              <span class="job-name">{{ cronJobItem.name }}</span>
              <icon
                v-if="cronJobItem.hasConfirm"
                class="job-check"
                type="check" />
              <div @click.stop="">
                <bk-switcher
                  class="job-switch"
                  disabled
                  size="small"
                  theme="primary"
                  :value="cronJobItem.enable"
                  @change="value => handleEnableChange(cronJobItem.id, value)" />
              </div>
            </div>
          </auth-component>
        </div>
      </scroll-faker>
    </div>
    <div class="layout-right">
      <scroll-faker>
        <cron-detail
          :key="detailInfo.id"
          :data="detailInfo"
          :variable-list="templateVariableList"
          @on-change="handleVariableChange"
          @on-update-confirm="handleUpdateConfirm" />
      </scroll-faker>
    </div>
  </div>
</template>
<script>
  import TimeTaskService from '@service/time-task';

  import ScrollFaker from '@components/scroll-faker';

  import CronDetail from './cron-detail';

  const generatorData = ({ id, name, enable }) => ({
    id,
    name,
    enable,
    hasConfirm: false,
    variableValue: [],
  });

  export default {
    name: '',
    components: {
      ScrollFaker,
      CronDetail,
    },
    props: {
      templateInfo: {
        type: Object,
        required: true,
      },
      planId: {
        type: Number,
        required: true,
      },
      cronJobList: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        isLoading: true,
        isEmpty: false,
        timeTaskList: [],
        currentTaskId: 0,
      };
    },
    computed: {
      detailInfo() {
        return this.timeTaskList.find(item => item.id === this.currentTaskId) || {};
      },
    },
    created() {
      this.templateId = this.templateInfo.id;
      this.templateVariableList = this.templateInfo.variables;
      this.fetchTimeTaskList();
    },
    methods: {
      fetchTimeTaskList() {
        if (this.cronJobList.length > 0) {
          this.timeTaskList = this.cronJobList;
          this.currentTaskId = this.cronJobList[0].id;
          return;
        }
        this.isLoading = true;
        TimeTaskService.fetchTaskOfPlan({
          id: this.planId,
        }).then((data) => {
          if (data.length < 1) {
            this.isEmpty = true;
            return;
          }
          this.timeTaskList = data.map(item => generatorData(item));
          if (data.length > 0) {
            this.currentTaskId = data[0].id;
          }
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      trigger() {
        const cronJob = this.timeTaskList.reduce((result, item) => {
          // 关闭的定时任务传全局变量
          result.push({
            ...item,
            variableValue: item.enable ? item.variableValue : [],
          });
          return result;
        }, []);
        this.$emit('on-change', cronJob);
      },
      handleTabChange(id) {
        this.currentTaskId = id;
      },
      handleEnableChange(id, enable) {
        const timeTask = this.timeTaskList.find(item => item.id === id);
        timeTask.enable = enable;
        timeTask.hasConfirm = false;
        this.trigger();
      },
      handleVariableChange(payload) {
        const timeTask = this.timeTaskList.find(item => item.id === this.currentTaskId);
        timeTask.variableValue = Object.freeze(payload);
        timeTask.hasConfirm = true;
        this.trigger();
      },
      handleUpdateConfirm(payload) {
        const timeTask = this.timeTaskList.find(item => item.id === this.currentTaskId);
        timeTask.hasConfirm = payload;
        this.trigger();
      },
    },
  };
</script>
<style lang='postcss'>
  .plan-manage-confirm-cron {
    display: flex;
    background: #fff;

    .layout-left {
      position: relative;
      z-index: 1000;
      flex: 0 0 320px;
      height: calc(100vh - 154px);
      background: #fafbfd;

      &::after {
        position: absolute;
        top: 0;
        right: 0;
        width: 1px;
        height: 100%;
        background: #ecedf3;
        content: "";
      }

      .cron-job-tab {
        position: relative;
        z-index: 1;
        font-size: 14px;
        color: #63656e;
        cursor: pointer;
        border-bottom: 1px solid #ecedf3;

        .tab-container {
          display: flex;
          height: 40px;
          padding: 0 13px 0 24px;
          align-items: center;
        }

        &.active {
          background: #fff;
        }

        .job-name {
          margin-right: auto;
        }

        .job-check {
          font-size: 24px;
          color: #2dcb56;
        }

        .job-switch {
          margin-left: 20px;
        }
      }
    }

    .layout-right {
      flex: 1;
      height: calc(100vh - 154px);
    }
  }
</style>
