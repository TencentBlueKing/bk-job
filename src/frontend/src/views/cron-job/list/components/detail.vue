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
  <detail-layout
    v-bkloading="{ isLoading }"
    class="detail-layout-wrapper"
    mode="see">
    <detail-item :label="$t('cron.任务名称：')">
      {{ data.name }}
    </detail-item>
    <detail-item :label="$t('cron.执行策略：')">
      {{ data.executeStrategyText }}
    </detail-item>
    <detail-item :label="$t('cron.执行时间：')">
      <span
        v-bk-tooltips.right="data.executeTimeTips"
        class="tips">
        {{ data.policeText }}
      </span>
    </detail-item>
    <detail-item
      v-if="data.endTime"
      :label="$t('cron.结束时间：')">
      {{ data.endTime }}
    </detail-item>
    <detail-item
      v-if="data.notifyOffset"
      :label="data.executeStrategy === 'once' ? $t('cron.执行前通知：') : $t('cron.结束前通知：')">
      {{ data.notifyOffset }}{{ $t('cron.分钟') }}
    </detail-item>
    <detail-item
      v-if="renderRoleList.length > 0 || data.notifyUser.userList.length > 0"
      :label="$t('cron.通知对象：')">
      <div class="approval-wraper">
        <div
          v-for="role in renderRoleList"
          :key="role"
          class="item">
          <icon
            class="approval-flag"
            type="user-group-gray" />
          {{ role }}
        </div>
        <div
          v-for="user in data.notifyUser.userList"
          :key="user"
          class="item">
          <icon
            class="approval-flag"
            type="user" />
          {{ user }}
        </div>
      </div>
    </detail-item>
    <detail-item
      v-if="renderChannel"
      :label="$t('cron.通知方式：')">
      {{ renderChannel }}
    </detail-item>
    <detail-item :label="$t('cron.执行方案：')">
      {{ data.taskPlanName }}
    </detail-item>
    <render-info-detail
      v-bkloading="{ isVarLoading }"
      left="20">
      <template v-if="!isVarLoading">
        <span
          v-if="currentPlanVariableList.length < 1"
          class="plan-variable-empty">
          {{ $t('cron.该执行方案无全局变量') }}
        </span>
        <global-variable-layout v-else>
          <global-variable
            v-for="variable in currentPlanVariableList"
            :key="variable.id + variable.name"
            :data="variable"
            :layout="variable.type === 3 ? 'vertical' : ''"
            :type="variable.type"
            value-width="100%" />
        </global-variable-layout>
      </template>
    </render-info-detail>
  </detail-layout>
</template>
<script>
  import NotifyService from '@service/notify';
  import QueryGlobalSettingService from '@service/query-global-setting';
  import TaskPlanService from '@service/task-plan';

  import DetailLayout from '@components/detail-layout';
  import DetailItem from '@components/detail-layout/item';
  import GlobalVariableLayout from '@components/global-variable/layout';
  import GlobalVariable from '@components/global-variable/view';

  import RenderInfoDetail from './render-info-detail';

  export default {
    name: 'TimeTaskDetail',
    components: {
      DetailLayout,
      DetailItem,
      RenderInfoDetail,
      GlobalVariableLayout,
      GlobalVariable,
    },
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isLoading: true,
        isVarLoading: false,
        renderRoleList: [],
        renderChannel: '',
        currentPlanVariableList: [],
      };
    },

    created() {
      Promise.all([
        this.fetchData(),
        this.fetchRoleList(),
        this.fetchAllChannel(),
      ]).finally(() => {
        this.isLoading = false;
      });
    },
    methods: {
      /**
       * @desc 获取定时人详情
       */
      fetchData() {
        TaskPlanService.fetchPlanDetailInfo({
          templateId: this.data.taskTemplateId,
          id: this.data.taskPlanId,
        }).then((planInfo) => {
          // 使用执行方案的变量
          // 如果定时任务任务中存有变量变量值——拷贝过来
          const currentPlanVariableList = planInfo.variableList;
          // 当前定时任务变量
          const cronJobVariableMap = this.data.variableValue.reduce((result, variableItem) => {
            result[variableItem.id] = variableItem;
            return result;
          }, {});
          // 拷贝定时任务中的变量值到执行方案的相同变量中
          // 定时任务中的变量是执行的赋值逻辑，分别使用的是value、server字段来存储
          // 执行方案中的变量表示的是默认值，分别使用defaultValue、defaultTargetValue字段来存储
          currentPlanVariableList.forEach((validVariableFromPlan) => {
            if (cronJobVariableMap[validVariableFromPlan.id]) {
              const { value, targetValue } = cronJobVariableMap[validVariableFromPlan.id];
              validVariableFromPlan.defaultValue = value;
              validVariableFromPlan.defaultTargetValue = targetValue;
            }
          });
          this.currentPlanVariableList = Object.freeze(currentPlanVariableList);
        });
      },
      /**
       * @desc 通知人列表
       */
      fetchRoleList() {
        NotifyService.fetchRoleList()
          .then((data) => {
            const roleMap = {};
            data.forEach((role) => {
              roleMap[role.code] = role.name;
            });
            this.renderRoleList = this.data.notifyUser.roleList.map(_ => roleMap[_]);
          });
      },
      /**
       * @desc 通知渠道
       */
      fetchAllChannel() {
        QueryGlobalSettingService.fetchActiveNotifyChannel()
          .then((data) => {
            const channelMap = {};
            data.forEach((channel) => {
              channelMap[channel.code] = channel.name;
            });
            this.renderChannel = this.data.notifyChannel.map(_ => channelMap[_]).join('，');
          });
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .detail-layout-wrapper {
    .approval-wraper {
      display: flex;
      align-items: center;
      height: 34px;

      .item {
        display: flex;
        height: 20px;
        padding: 0 6px;
        margin-right: 10px;
        font-size: 12px;
        color: #63656e;
        background: #f0f1f5;
        border-radius: 2px;
        align-items: center;
      }

      .approval-flag {
        margin-right: 4px;
      }
    }

    .detail-item {
      margin-bottom: 0;
    }

    .plan-variable-empty {
      color: #b2b5bd;
    }
  }
</style>
