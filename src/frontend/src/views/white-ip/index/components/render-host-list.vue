<template>
  <div>
    <div
      class="render-host-list"
      @click="handleShowIp">
      <span class="number strong">{{ data.length }}</span>
      台主机
    </div>
    <bk-dialog
      v-model="showHostList"
      :draggable="false"
      header-position="left"
      title="IP预览"
      :width="940">
      <ip-selector
        v-if="showHostList"
        readonly
        show-view
        v-bind="ipSelectorConfig"
        :value="{hostList: data}" />
      <template #footer>
        <bk-button @click="handleHideHostList">
          关闭
        </bk-button>
      </template>
    </bk-dialog>
  </div>
</template>
<script>
  import HostAllManageService from '@service/host-all-manage';

  export default {
    name: '',
    props: {
      data: {
        type: Array,
      },
    },
    data() {
      return {
        showHostList: false,
      };
    },
    created() {
      this.ipSelectorConfig = {
        service: {
          fetchTopologyHostCount: HostAllManageService.fetchTopologyWithCount,
          fetchTopologyHostsNodes: HostAllManageService.fetchTopologyHost,
          fetchTopologyHostIdsNodes: HostAllManageService.fetchTopogyHostIdList,
          fetchHostsDetails: HostAllManageService.fetchHostInfoByHostId,
          fetchHostCheck: HostAllManageService.fetchInputParseHostList,
        },
      };
    },
    methods: {
      handleShowIp() {
        this.showHostList = true;
      },
      handleHideHostList() {
        this.showHostList = false;
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .render-host-list {
    cursor: pointer;
  }
</style>
