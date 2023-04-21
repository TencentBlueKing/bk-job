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
            } = hostVariable.defaultTargetValue.hostNodeInfo;
            allHostList.push(...hostList.map(({ hostId }) => ({ hostId })));
            allNodeList.push(...nodeList.map(({ objectId, instanceId }) => ({ objectId, instanceId })));
            allDynamicGroupList.push(...dynamicGroupList.map(({ id }) => ({ id })));
          }
        });

        allHostList = _.uniqBy(allHostList, ({ hostId }) => ({ hostId }));
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
        this.$emits('change');
      },
    },
  };
</script>
