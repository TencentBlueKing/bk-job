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
  <detail-layout
    v-bkloading="{ isLoading }"
    class="cron-tab-detail-box"
    mode="see">
    <detail-item :label="$t('cron.任务名称：')">
      {{ cronJobDetail.name }}
    </detail-item>
    <detail-item :label="$t('cron.执行策略：')">
      {{ cronJobDetail.executeStrategyText }}
    </detail-item>
    <detail-item :label="$t('cron.执行时间：')">
      <span
        v-bk-tooltips.right="cronJobDetail.executeTimeTips"
        class="tips">
        {{ cronJobDetail.policeText }}
      </span>
    </detail-item>
    <detail-item
      v-if="cronJobDetail.endTime"
      :label="$t('cron.结束时间：')">
      {{ cronJobDetail.endTime }}
    </detail-item>
    <detail-item
      v-if="cronJobDetail.notifyOffset"
      :label="cronJobDetail.executeStrategy === 'once' ? $t('cron.执行前通知：') : $t('cron.结束前通知：')">
      {{ cronJobDetail.notifyOffset }}{{ $t('cron.分钟') }}
    </detail-item>
    <detail-item
      v-if="cronJobDetail.notifyUser"
      :label="$t('cron.通知对象：')">
      <div class="approval-wraper">
        <bk-tag
          v-for="role in cronJobDetail.notifyUser.roleList"
          :key="role">
          <icon
            class="approval-flag"
            type="user-group-gray" />
          {{ roleNameMap[role] }}
        </bk-tag>
        <bk-tag
          v-for="user in cronJobDetail.notifyUser.userList"
          :key="user">
          <icon
            class="approval-flag"
            type="user" />
          {{ user }}
        </bk-tag>
      </div>
    </detail-item>
    <detail-item
      v-if="cronJobDetail.notifyChannel"
      :label="$t('cron.通知方式：')">
      <bk-tag
        v-for="item in data.notifyChannel"
        :key="item">
        {{ channelNameMap[item] }}
      </bk-tag>
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
    <detail-item :label="$t('cron.消息通知：')">
      {{ cronJobDetail.notifyType === 1 ? $t('cron.继承业务设置') : $t('cron.自定义') }}
    </detail-item>
    <render-info-detail
      v-if="cronJobDetail.notifyType === 2"
      left="20">
      <div>
        <detail-item :label="$t('cron.通知对象：')">
          <div class="approval-wraper">
            <template v-for="role in cronJobDetail.cronJobCustomNotifyVO.roleList">
              <bk-tag
                v-if="role !== 'JOB_EXTRA_OBSERVER'"
                :key="role">
                <icon
                  class="approval-flag"
                  type="user-group-gray" />
                {{ roleNameMap[role] }}
              </bk-tag>
            </template>
            <bk-tag
              v-for="user in cronJobDetail.cronJobCustomNotifyVO.extraObserverList"
              :key="user">
              <icon
                class="approval-flag"
                type="user" />
              {{ user }}
            </bk-tag>
          </div>
        </detail-item>
        <detail-item
          :label="$t('cron.通知方式：')"
          layout="vertical">
          <table class="notify-way-table">
            <thead>
              <th style="width: 95px;">
                {{ $t('cron.状态') }}
              </th>
              <th>{{ $t('cron.通知方式') }}</th>
            </thead>
            <tbody>
              <tr
                v-for="channelStatus in Object.keys(cronJobDetail.cronJobCustomNotifyVO.resourceStatusChannelMap)"
                :key="channelStatus">
                <td>{{ channelStatus === 'SUCCESS' ? $t('cron.执行成功') : $t('cron.执行失败') }}</td>
                <td>
                  <bk-checkbox
                    v-for="item in channelList"
                    :key="item.code"
                    :checked="cronJobDetail.cronJobCustomNotifyVO.resourceStatusChannelMap[channelStatus].includes(item.code)"
                    disabled>
                    {{ item.name }}
                  </bk-checkbox>
                </td>
              </tr>
            </tbody>
          </table>
        </detail-item>
      </div>
    </render-info-detail>
  </detail-layout>
</template>
<script>
  import CronJobService from '@service/cron-job';
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
        cronJobDetail: {},
        currentPlanVariableList: [],
        roleNameMap: {},
        channelNameMap: {},
        channelList: [],
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
        return Promise.all([
          CronJobService.getDetail({
            id: this.data.id,
          }),
          TaskPlanService.fetchPlanDetailInfo({
            templateId: this.data.taskTemplateId,
            id: this.data.taskPlanId,
          }),
        ]).then(([cronJobDetail, planDetail]) => {
          this.cronJobDetail = Object.freeze(cronJobDetail);

          // 使用执行方案的变量
          // 如果定时任务任务中存有变量变量值——拷贝过来
          const currentPlanVariableList = planDetail.variableList;
          // 当前定时任务变量
          const cronJobVariableMap = cronJobDetail.variableValue.reduce((result, variableItem) => {
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
        return NotifyService.fetchRoleList()
          .then((data) => {
            const roleMap = {};
            data.forEach((role) => {
              roleMap[role.code] = role.name;
            });
            this.roleNameMap = Object.freeze(roleMap);
          });
      },
      /**
       * @desc 通知渠道
       */
      fetchAllChannel() {
        return QueryGlobalSettingService.fetchActiveNotifyChannel()
          .then((data) => {
            const channelMap = {};
            data.forEach((channel) => {
              channelMap[channel.code] = channel.name;
            });
            this.channelNameMap = Object.freeze(channelMap);
            this.channelList = Object.freeze(data);
          });
      },
    },
  };
</script>
<style lang="postcss">
.cron-tab-detail-box {
  .bk-tag{
    margin: 0;

    & ~ .bk-tag{
      margin-left: 10px;
    }
  }

  .approval-wraper {
    display: flex;
    align-items: center;
    height: 34px;

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

  .notify-way-table {
    width: 100%;
    background: #fff;
    border: 1px solid #dcdee5;

    th,
    td {
      height: 42px;
      padding-left: 16px;
      font-size: 12px;
      text-align: left;
      border-left: 1px solid #dcdee5;
    }

    th {
      font-weight: normal;
      color: #313238;
      background: #fafbfd;
    }

    td {
      color: #63656e;
      border-top: 1px solid #dcdee5;
    }


    .bk-form-checkbox {
      & ~ .bk-form-checkbox {
        margin-left: 40px;
      }
    }
  }
}
</style>
