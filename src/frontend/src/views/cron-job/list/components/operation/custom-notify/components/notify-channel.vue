<template>
  <div
    ref="rootRef"
    v-bkloading="{ isLoading: isChannelLoading }"
    class="custom-notify-channel-wraper">
    <bk-checkbox
      :checked="isChannelAll"
      :indeterminate="isChannelIndeterminate"
      @click.native="handleToggleAllChannel">
      {{ $t('cron.全部') }}
    </bk-checkbox>
    <bk-checkbox-group
      class="all-channel"
      :value="value"
      @change="handleNotifyChannelChange">
      <bk-checkbox
        v-for="channel in channleList"
        :key="channel.code"
        :value="channel.code">
        {{ channel.name }}
      </bk-checkbox>
    </bk-checkbox-group>
  </div>
</template>
<script setup lang="ts">
  import { computed, onMounted, ref, shallowRef } from 'vue';

  import QueryGlobalSettingService from '@service/query-global-setting';

  const props = defineProps({
    value: {
      type: Array,
      requird: true,
    },
  });

  const emits = defineEmits(['change']);

  const rootRef = ref();
  const channleList = shallowRef([]);
  const isChannelLoading = ref(false);

  const isChannelAll = computed(() => {
    if (channleList.value.length < 1) {
      return false;
    }
    return props.value.length === channleList.value.length;
  });

  const isChannelIndeterminate = computed(() => {
    if (props.value.length < 1) {
      return false;
    }
    return props.value.length !== channleList.value.length;
  });

  const fetchAllChannel = () => {
    isChannelLoading.value = true;
    QueryGlobalSettingService.fetchActiveNotifyChannel()
      .then((data) => {
        channleList.value = data;
      })
      .finally(() => {
        isChannelLoading.value = false;
      });
  };

  fetchAllChannel();

  const handleNotifyChannelChange = (value) => {
    emits('change', value);
  };

  const handleToggleAllChannel = () => {
    if (isChannelAll.value) {
      handleNotifyChannelChange([]);
    } else {
      handleNotifyChannelChange(channleList.value.map(_ => _.code));
    }
  };

  onMounted(() => {
    rootRef.value?.scrollIntoView();
  });
</script>
<style lang='postcss'>

  .custom-notify-channel-wraper {
    display: flex;
    align-items: flex-start;
    min-height: 32px;
    white-space: nowrap;

    .all-channel {
      display: flex;
      flex-wrap: wrap;
    }

    .bk-form-checkbox {
      display: flex;
      flex: 0 0 auto;
      margin-top: 6px;
      margin-right: 40px;
    }

    .bk-checkbox {
      background: #fff;
    }
  }
</style>

