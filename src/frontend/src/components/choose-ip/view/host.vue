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
  <jb-collapse-item name="host">
    <span class="panel-title">
      <span>
        <span>{{ $t('已选择.result') }}</span>
        <span class="strong number">{{ data.length }}</span>
        <span>{{ $t('台主机.result') }}</span>
      </span>
      <span v-if="statisticsData.fail">
        （<span class="error number">{{ statisticsData.fail }}</span>
        {{ $t('台Agent异常') }}）
      </span>
    </span>
    <action-extend
      copyable
      :invalid-list="invalidList"
      :list="list">
      <template v-if="editable">
        <div
          class="action-item"
          @click="handleRemoveAll">
          {{ $t('移除全部') }}
        </div>
        <div
          class="action-item"
          @click="handleRemoveFail">
          {{ $t('移除异常') }}
        </div>
      </template>
    </action-extend>
    <template #content>
      <div v-bkloading="{ isLoading }">
        <host-table
          v-if="!isRequestError"
          :append-nums="invalidList.length"
          :diff="diff"
          :editable="editable"
          :list="list"
          :max-height="410"
          @on-change="handleRemoveOne">
          <tbody
            v-if="invalidList.length > 0"
            slot="appendBefore"
            class="invalid-list">
            <tr
              v-for="(ipInfo) in invalidList"
              :key="ipInfo.hostId">
              <td class="table-cell">
                <span
                  v-bk-tooltips="$t('指主机已不属于该业务，或已不存在')"
                  class="invalid">
                  {{ $t('无效') }}
                </span>
                <span>{{ ipInfo.ip }}</span>
              </td>
              <td>--</td>
              <td>--</td>
              <td>--</td>
              <td>--</td>
              <td
                v-if="editable"
                class="action-column">
                <bk-button
                  text
                  @click="handleInvalidRemove(hostId)">
                  {{ $t('移除') }}
                </bk-button>
              </td>
            </tr>
          </tbody>
        </host-table>
        <bk-exception
          v-if="isRequestError"
          style="padding-bottom: 50px;"
          type="500">
          <div style="display: flex; font-size: 14px;">
            <span>数据拉取失败，请</span>
            <bk-button
              text
              @click="handleRefresh">
              重试
            </bk-button>
          </div>
        </bk-exception>
      </div>
    </template>
  </jb-collapse-item>
</template>
<script>
  import _ from 'lodash';

  import HostManageService from '@service/host-manage';

  import JbCollapseItem from '@components/jb-collapse-item';

  import ActionExtend from '../components/action-extend';
  import HostTable from '../components/host-table';
  import {
    sortHost,
    statisticsHost,
  } from '../components/utils';

  import I18n from '@/i18n';

  export default {
    name: 'ViewHost',
    components: {
      JbCollapseItem,
      ActionExtend,
      HostTable,
    },
    props: {
      data: {
        type: Array,
        required: true,
      },
      editable: {
        type: Boolean,
        default: false,
      },
      allPanel: {
        type: Boolean,
        default: false,
      },
      diff: {
        type: Object,
        default: () => ({}),
      },
    },
    data() {
      return {
        isLoading: false,
        isRequestError: false,
        list: [],
        invalidList: [], // 无效主机
      };
    },
    computed: {
      statisticsData() {
        return statisticsHost(this.list);
      },
    },
    watch: {
      data: {
        handler(data) {
          if (this.isInnerChange) {
            this.isInnerChange = false;
            return;
          }
          if (data.length < 1) {
            this.list = [];
            return;
          }
          this.fetchHostOfHost();
        },
        immediate: true,
      },
    },
    created() {
      this.isInnerChange = false;
    },
    methods: {
      /**
       * @desc 通过ip和云区域获取主机信息
       */
      fetchHostOfHost() {
        this.isLoading = true;
        const hostIdMap = this.data.reduce((result, ipInfo) => {
          result[ipInfo.hostId] = ipInfo;
          return result;
        }, {});

        HostManageService.fetchHostOfHost({
          hostIdList: this.data.map(({ hostId }) => hostId),
        })
          .then((data) => {
            const list = [];
            this.invalidList = [];
            // list 用于收集有效的主机
            data.forEach((currentHost) => {
              if (hostIdMap[currentHost.hostId]) {
                list.push(currentHost);
                delete hostIdMap[currentHost.hostId];
              }
            });
            this.list = Object.freeze(sortHost(list));
            // 剩余没被delete的主机是无效主机
            this.invalidList = Object.freeze(Object.values(hostIdMap));
            this.isRequestError = false;
          })
          .catch(() => {
            this.isRequestError = true;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 外部调用刷新主机状态
       */
      refresh: _.debounce(function () {
        this.fetchHostOfHost();
      }, 300),

      /**
       * @desc 外部调用获取所有主机
       */
      getAllHost() {
        return [
          ...this.list,
        ];
      },

      /**
       * @desc 外部调用移除所有无效的主机
       */
      removeAllInvalidHost() {
        this.invalidList = [];
        this.triggerChange();
      },
      /**
       * @desc 外部调用获取所有无效的主机
       */
      getAllInvalidHost() {
        return this.invalidList;
      },

      triggerChange() {
        this.isInnerChange = true;
        const result = this.invalidList.map(({ hostId }) => ({ hostId }));
        this.list.forEach((hostInfo) => {
          result.push({
            hostId: hostInfo.hostId,
          });
        });
        this.$emit('on-change', result);
      },
      /**
       * @desc 失败重试
       */
      handleRefresh() {
        this.fetchHostOfHost();
      },
      /**
       * @desc 移除无效主机
       * @prams { Number } hostId  移除主机
       */
      handleInvalidRemove(hostId) {
        this.invalidList = Object.freeze(this.invalidList.reduce((result, item) => {
          if (item !== hostId) {
            result.push(item);
          }
          return result;
        }, []));
        this.triggerChange();
      },
      /**
       * @desc 移除所有主机
       */
      handleRemoveAll() {
        if (this.list.length < 1
          && this.invalidList.length < 1) {
          this.messageSuccess(I18n.t('没有可移除主机'));
          return;
        }
        this.invalidList = [];
        this.list = [];
        this.messageSuccess(I18n.t('移除全部主机成功'));
        this.triggerChange();
      },
      /**
       * @desc 移除主机
       * @param { String} hostInfo
       */
      handleRemoveOne(hostInfo) {
        // 内部显示删除
        const list = this.list.reduce((result, currentHostInfo) => {
          if (currentHostInfo.hostId !== hostInfo.hostId) {
            result.push(currentHostInfo);
          }
          return result;
        }, []);
        this.list = Object.freeze(list);
        this.triggerChange();
      },
      /**
       * @desc 移除异常主机
       */
      handleRemoveFail() {
        const effectiveIp = [];
        const failIp = [];
        this.list.forEach((currentHostInfo) => {
          if (currentHostInfo.alive) {
            effectiveIp.push(currentHostInfo);
          } else {
            failIp.push(currentHostInfo);
          }
        });
        if (failIp.length < 1 && this.invalidList.length < 1) {
          this.messageSuccess(I18n.t('没有可移除主机'));
          return;
        }
        this.invalidList = [];
        this.list = Object.freeze(effectiveIp);
        this.messageSuccess(I18n.t('移除异常主机成功'));
        this.triggerChange();
      },
    },
  };
</script>
