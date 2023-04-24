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

  import AppManageService from '@service/app-manage';

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
        let allIpList = [];
        let allNodeList = [];
        let allDynamicGroupList = [];

        this.hostVariableList.forEach((hostVariable) => {
          if (!hostVariable.defaultTargetValue.isEmpty) {
            const {
              ipList,
              topoNodeList,
              dynamicGroupList,
            } = hostVariable.defaultTargetValue.hostNodeInfo;
            allIpList.push(...ipList);
            allNodeList.push(...topoNodeList.map(({ type, id }) => ({ type, id })));
            allDynamicGroupList.push(...dynamicGroupList);
          }
        });

        allIpList = _.uniq(allIpList.map(host => `${host.cloudAreaInfo.id}:${host.ip}`));
        allNodeList = _.uniqBy(allNodeList, ({ type, id }) => `#${type}#${id}`);
        allDynamicGroupList = _.uniq(allDynamicGroupList.map(({ id }) => id));

        const requestQueue = [];
        if (allIpList.length > 0) {
          requestQueue.push(AppManageService.fetchHostOfHost({
            ipList: allIpList,
          }));
        } else {
          requestQueue.push(Promise.resolve([]));
        }

        if (allNodeList.length > 0) {
          requestQueue.push(AppManageService.fetchNodeInfo(allNodeList));
        } else {
          requestQueue.push(Promise.resolve([]));
        }

        if (allDynamicGroupList.length > 0) {
          requestQueue.push(AppManageService.fetchHostOfDynamicGroup({
            id: allDynamicGroupList.join(','),
          }));
        } else {
          requestQueue.push(Promise.resolve([]));
        }

        Promise.all(requestQueue)
          .then(([ipListResult, nodeListResult, dynamicGroupListResult]) => {
            //
            this.isButtonDisable = allIpList.length <= ipListResult.length
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
