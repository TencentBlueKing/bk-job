<template>
  <ip-selector
    ref="ipSelector"
    :disable-host-method="disableHostCallback"
    v-bind="$attrs"
    v-on="$listeners" />
</template>
<script>
  export default {
    name: 'JbIpSelector',
    props: {
      disableHostMethod: {
        type: Function,
        default: () => false,
      },
    },
    methods: {
      disableHostCallback(hostData, hostList) {
        if (hostData.cloud_area.id === -1) {
          return this.$t('未分配管控区，请到节点管理重装 Agent');
        }

        return this.disableHostMethod(hostData, hostList);
      },
      getHostList() {
        return this.$refs.ipSelector.getHostList();
      },
      // 获取所有主机的 ipv4 列表
      getHostIpv4List() {
        return this.$refs.ipSelector.getHostIpv4List();
      },
      // 获取所有主机的 ipv6 列表
      getHostIpv6List() {
        return this.$refs.ipSelector.getHostIpv6List();
      },
      // 获取所有 agent 异常主机的 ipv4 列表
      getAbnormalHostIpv4List() {
        return this.$refs.ipSelector.getAbnormalHostIpv4List();
      },
      // 获取所有 agent 异常主机的 ipv6 列表
      getAbnormalHostIpv6List() {
        return this.$refs.ipSelector.getAbnormalHostIpv6List();
      },
      // 获取所有 ipv4 主机的列表
      getIpv4HostList() {
        return this.$refs.ipSelector.getIpv4HostList();
      },
      // 获取所有 ipv6 主机的列表
      getIpv6HostList() {
        return this.$refs.ipSelector.getIpv6HostList();
      },
      // 获取所有 agent 异常的 ipv4 主机列表
      getAbnormalIpv4HostList() {
        return this.$refs.ipSelector.getAbnormalIpv4HostList();
      },
      // 获取所有 agent 异常的  ipv6  主机的列表
      getAbnormalIpv6HostList() {
        return this.$refs.ipSelector.getAbnormalIpv6HostList();
      },
      resetValue() {
        this.$refs.ipSelector.resetValue();
      },
      refresh() {
        this.$refs.ipSelector.refresh();
      },
      collapseToggle(lastStatus) {
        this.$refs.ipSelector.collapseToggle(lastStatus);
      },
      removeInvalidData() {
        this.$refs.ipSelector.removeInvalidData();
      },
    },
  };
</script>

