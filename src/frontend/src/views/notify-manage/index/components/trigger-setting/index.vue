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
  <jb-form class="notify-trigger-setting">
    <jb-form-item :label="$t('notify.操作类型')">
      <bk-checkbox-group
        class="input"
        :value="localValue.resourceTypeList"
        @change="handleResourceTypeChange">
        <bk-checkbox
          v-for="(item) in templateData.resourceTypeList"
          :key="item.code"
          :value="item.code">
          {{ item.name }}
        </bk-checkbox>
      </bk-checkbox-group>
    </jb-form-item>
    <jb-form-item :label="$t('notify.通知对象')">
      <jb-user-selector
        class="input"
        :filter-list="['JOB_EXTRA_OBSERVER']"
        :placeholder="$t('notify.请输入')"
        :role="localValue.roleList"
        :user="localValue.extraObserverList"
        @on-change="handleUserChange" />
    </jb-form-item>
    <jb-form-item :label="$t('notify.通知方式')">
      <table class="notify-way-table input">
        <thead>
          <th style="width: 95px;">
            {{ $t('notify.状态') }}
          </th>
          <th>{{ $t('notify.通知方式') }}</th>
        </thead>
        <tbody>
          <tr
            v-for="(executeStatus, index) in templateData.executeStatusList"
            :key="index">
            <td>{{ executeStatus.name }}</td>
            <td>
              <render-notify-way
                :channel-list="templateData.availableNotifyChannelList"
                :value="localValue.resourceStatusChannelMap[executeStatus.code] || []"
                @on-change="value => handleNotifyWayChange(executeStatus.code, value)" />
            </td>
          </tr>
        </tbody>
      </table>
    </jb-form-item>
  </jb-form>
</template>
<script>
  import _ from 'lodash';

  import JbUserSelector from '@components/jb-user-selector';

  import RenderNotifyWay from './components/render-notify-way';

  export default {
    components: {
      JbUserSelector,
      RenderNotifyWay,
    },
    props: {
      type: {
        type: String,
        require: true,
      },
      data: {
        type: Object,
        default: () => ({}),
      },
      templateData: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        localValue: _.cloneDeep(this.data),
      };
    },
    methods: {
      /**
       * @desc 外部调用——重置用户输入
       */
      reset() {
        this.localValue = _.cloneDeep(this.data);
      },
      /**
       * @desc 外部调用——获取用户输入
       * @returns {Object} 通知策略配置
       */
      getValue() {
        const {
          resourceTypeList,
          extraObserverList,
          roleList,
          resourceStatusChannelMap,
        } = this.localValue;
        return {
          triggerType: this.type,
          resourceTypeList,
          roleList,
          extraObserverList,
          resourceStatusChannelList: Object.keys(resourceStatusChannelMap).reduce((result, executeStatus) => {
            result.push({
              executeStatus,
              channelList: resourceStatusChannelMap[executeStatus],
            });
            return result;
          }, []),
        };
      },
      /**
       * @desc 设置操作类型
       * @param { Array } resourceTypeList
       */
      handleResourceTypeChange(resourceTypeList) {
        this.localValue.resourceTypeList = resourceTypeList;
        window.changeFlag = true;
      },
      /**
       * @desc 设置通知对象
       * @param { Array } usextraObserverLister 额外通知人
       * @param { Array } role 通知角色
       */
      handleUserChange(extraObserverList, roleList) {
        if (extraObserverList.length > 0) {
          roleList.push('JOB_EXTRA_OBSERVER');
        }
        this.localValue.extraObserverList = extraObserverList;
        this.localValue.roleList = roleList;
        window.changeFlag = true;
      },
      /**
       * @desc 设置通知方式
       * @param { String } executeStatus 执行状态
       * @param { Array } channelList 通知渠道
       */
      handleNotifyWayChange(executeStatus, channelList) {
        this.localValue.resourceStatusChannelMap[executeStatus] = channelList;
        window.changeFlag = true;
      },
    },
  };
</script>
<style lang="postcss">
  .notify-trigger-setting {
    .bk-form-checkbox ~ .bk-form-checkbox {
      margin-left: 25px;
    }

    .input {
      width: 716px;
    }

    .notify-way-table {
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
    }
  }
</style>
