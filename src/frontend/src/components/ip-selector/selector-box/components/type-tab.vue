<template>
    <div class="ip-selector-type-tab">
        <div
            class="tab-item"
            :class="{
                active: value === 'staticTopo',
                disabled: Boolean(uniquedType) && uniquedType !== 'INSTANCE',
            }"
            @click="handleChange('staticTopo', Boolean(uniquedType) && uniquedType !== 'INSTANCE')">
            静态拓扑
        </div>
        <div
            class="tab-item"
            :class="{
                active: value === 'dynamicTopo',
                disabled: Boolean(uniquedType) && uniquedType !== 'TOPO',
            }"
            @click="handleChange('dynamicTopo', Boolean(uniquedType) && uniquedType !== 'TOPO')">
            动态拓扑
        </div>
        <div
            class="tab-item"
            :class="{
                active: value === 'group',
                disabled: Boolean(uniquedType) && uniquedType !== 'group',
            }"
            @click="handleChange('group', Boolean(uniquedType) && uniquedType !== 'group')">
            动态分组
        </div>
        <div
            class="tab-item"
            :class="{
                active: value === 'serviceTemplate',
                disabled: Boolean(uniquedType) && uniquedType !== 'SERVICE_TEMPLATE',
            }"
            @click="handleChange('serviceTemplate', Boolean(uniquedType) && uniquedType !== 'SERVICE_TEMPLATE')">
            服务模板
        </div>
        <div
            class="tab-item"
            :class="{
                active: value === 'setTemplate',
                disabled: Boolean(uniquedType) && uniquedType !== 'SET_TEMPLATE',
            }"
            @click="handleChange('setTemplate', Boolean(uniquedType) && uniquedType !== 'SET_TEMPLATE')">
            集群模板
        </div>
        <div
            class="tab-item"
            :class="{
                active: value === 'customInput',
                disabled: Boolean(uniquedType) && uniquedType !== 'INSTANCE',
            }"
            @click="handleChange('customInput', Boolean(uniquedType) && uniquedType !== 'INSTANCE')">
            自定义输入
        </div>
    </div>
</template>
<script setup>
  const props = defineProps({
    value: {
        type: String,
        required: true,
    },
    uniquedType: {
        type: String,
    },
  });

  const emits = defineEmits(['change', 'update:value']);

  const handleChange = (value, disabled) => {
    if (disabled) {
      return;
    }
    if (value === props.value) {
      return;
    }
    emits('change', value);
    emits('update:value', value);
  };
</script>
<style lang="postcss" scoped>
    .ip-selector-type-tab {
        display: flex;
        background: #fafbfd;
        border-bottom: 1px solid #dcdee5;
        user-select: none;

        .tab-item {
            flex: 1;
            height: 40px;
            line-height: 40px;
            color: #63656e;
            text-align: center;
            cursor: pointer;
            border-right: 1px solid #dcdee5;

            &.active {
                position: relative;
                color: #313238;
                cursor: default;
                background: #fff;

                &::after {
                    position: absolute;
                    bottom: -2px;
                    left: 0;
                    width: 100%;
                    height: 3px;
                    background: #fff;
                    content: "";
                }
            }

            &.disabled {
                color: #c4c6cc;
                cursor: not-allowed;
            }

            &:last-child {
                border-right: none;
            }
        }
    }
</style>
