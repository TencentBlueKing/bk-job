<template>
    <CollapseBox v-if="data.length > 0">
        <template #title>
            <!-- <div>已选<span class="number">{{ data.length }}</span>个节点</div> -->
            <div>【动态分组】- 共 2 个，新增 1  个</div>
        </template>
        <template #action>
            <CollapseExtendAction>
                <div @click="handlRemoveAll">
                    移除所有
                </div>
            </CollapseExtendAction>
        </template>
        <div>
            <CallapseContentItem
                v-for="(item, index) in data"
                :key="index"
                @remove="handleRemove(item)">
                {{ item.node_path }}
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
  import CallapseContentItem from './collapse-box/content-item.vue';
  import CollapseExtendAction from './collapse-box/extend-action.vue';
  import CollapseBox from './collapse-box/index.vue';

  const props = defineProps({
    data: {
        type: Array,
        required: true,
    },
  });
  const emits = defineEmits(['change']);

  const handleRemove = (removeTarget) => {
    const result = props.data.reduce((result, item) => {
      if (removeTarget !== item) {
        result.push(item);
      }
      return result;
    }, []);

    emits('change', 'group', result);
  };

  const handlRemoveAll = () => {
    emits('change', 'group', []);
  };
</script>
