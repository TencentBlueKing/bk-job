<template>
  <div
    v-if="executeObject"
    class="render-execution-info-title"
    :class="taskExecuteDetail.result">
    <div v-if="executeObject.container">
      {{ executeObject.container.name }}
    </div>
    <div
      v-if="executeObject.host"
      class="ip-box"
      :class="{
        'is-mult': executeObject.host.ip && executeObject.host.ipv6
      }">
      <div v-if="executeObject.host.ip">
        {{ executeObject.host.ip }}
      </div>
      <div v-if="executeObject.host.ipv6">
        {{ executeObject.host.ipv6 }}
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
  import { computed } from 'vue';

  const props = defineProps({
    taskExecuteDetail: {
      type: Object,
    },
  });

  const executeObject = computed(() => (props.taskExecuteDetail ? props.taskExecuteDetail.executeObject : undefined));
</script>
<style lang="postcss">
  .render-execution-info-title {
    display: flex;
    flex-direction: column;
    justify-content: center;
    width: 325px;
    padding-left: 20px;
    line-height: 14px;
    cursor: default;


    &.success,
    &.fail,
    &.running,
    &.waiting {
      &::before {
        position: absolute;
        top: 50%;
        left: 0;
        width: 3px;
        height: 12px;
        background: #2dc89d;
        content: "";
        transform: translateY(-50%);
      }
    }

    &.fail {
      &::before {
        background: #ea3636;
      }
    }

    &.running {
      &::before {
        background: #699df4;
      }
    }

    &.waiting {
      &::before {
        background: #dcdee5;
      }
    }

    .ip-box {
      &.is-mult {
        font-size: 12px;
      }
    }
  }
</style>
