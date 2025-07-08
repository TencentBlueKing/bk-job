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
  <span v-bk-tooltips="tooltops">
    <bk-button
      v-if="hostVariableList.length > 0"
      :disabled="isButtonDisable"
      :loading="isLoading"
      @click="handleRemoveAllInvalidHost">
      {{ $t('template.移除无效主机') }}
    </bk-button>
  </span>
</template>
<script>
  import _ from 'lodash';

  import HostManageService from '@service/host-manage';

  import { messageSuccess } from '@/common/bkmagic';
  import I18n from '@/i18n';

  export default {
    name: '',
    props: {
      hostVariableList: {
        type: Array,
        require: true,
      },
    },
    data() {
      return {
        isLoading: true,
        isButtonDisable: true,
      };
    },
    watch: {
      hostVariableList: {
        handler() {
          this.fetchData();
        },
      },
    },
    created() {
      this.tooltops = {
        disabled: true,
        content: I18n.t('template.未发现无效主机'),
      };
    },
    methods: {
      fetchData() {
        if (!this.isLoading) {
          return;
        }
        let allHostList = [];
        let allNodeList = [];
        let allDynamicGroupList = [];

        this.hostVariableList.forEach((hostVariable) => {
          if (!hostVariable.defaultTargetValue.isEmpty) {
            const {
              hostList,
              nodeList,
              dynamicGroupList,
            } = hostVariable.defaultTargetValue.executeObjectsInfo;
            allHostList.push(...hostList.map(({ hostId }) => ({ hostId })));
            allNodeList.push(...nodeList.map(({ objectId, instanceId }) => ({ objectId, instanceId })));
            allDynamicGroupList.push(...dynamicGroupList.map(({ id }) => ({ id })));
          }
        });

        // perf: 主机数据量过大
        const hostIdMap = {};
        const allHostListMemo = [...allHostList];
        allHostList = [];
        allHostListMemo.forEach((hostData) => {
          if (hostIdMap[hostData.hostId]) {
            return;
          }
          allHostList.push(hostData);
        });

        allNodeList = _.uniqBy(allNodeList, ({ objectId, instanceId }) => `#${objectId}#${instanceId}`);
        allDynamicGroupList = _.uniqBy(allDynamicGroupList, ({ id }) => id);

        const requestQueue = [];
        if (allHostList.length > 0) {
          requestQueue.push(HostManageService.fetchHostInfoByHostId({
            hostList: allHostList,
          }));
        } else {
          requestQueue.push(Promise.resolve([]));
        }

        if (allNodeList.length > 0) {
          requestQueue.push(HostManageService.fetchNodePath({
            nodeList: allNodeList,
          }));
        } else {
          requestQueue.push(Promise.resolve([]));
        }

        if (allDynamicGroupList.length > 0) {
          requestQueue.push(HostManageService.fetchDynamicGroup({
            dynamicGroupList: allDynamicGroupList,
          }));
        } else {
          requestQueue.push(Promise.resolve([]));
        }

        Promise.all(requestQueue)
          .then(([hostListResult, nodeListResult, dynamicGroupListResult]) => {
            this.isButtonDisable = allHostList.length <= hostListResult.length
              && allNodeList.length <= nodeListResult.length
              && allDynamicGroupList.length <= dynamicGroupListResult.length;

            this.tooltops.disabled = !this.isButtonDisable;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleRemoveAllInvalidHost() {
        this.$emit('change');
        this.isButtonDisable = true;
        this.tooltops.disabled = !this.isButtonDisable;
        messageSuccess(I18n.t('template.操作成功'));
      },
    },
  };
</script>
