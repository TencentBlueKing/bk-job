<template>
  <component
    :is="renderCom"
    v-bind="props"
    v-on="listeners" />
</template>
<script setup>
  import {
    computed,
    useListeners,
  } from 'vue';

  import ContainerList from './components/container-list/index.vue';
  import HostList from './components/host-list/index.vue';

  const props = defineProps({
    name: {
      type: [
        String,
        Number,
      ],
      required: true,
    },
    executeObjectType: {
      type: Number,
    },
    data: {
      type: Array,
      default: () => [],
    },
    listLoading: {
      type: Boolean,
      default: false,
    },
    total: {
      type: Number,
      default: 0,
    },
    searchValue: {
      type: String,
    },
    getAllTaskList: {
      type: Function,
      required: true,
    },
  });

  const listeners = useListeners();

  const comMap = {
    1: HostList,
    2: ContainerList,
  };

  const renderCom = computed(() => comMap[props.executeObjectType] || HostList);
</script>

