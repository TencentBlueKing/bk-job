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
        <div v-bkloading="{ isLoading }">
            <CallapseContentItem
                v-for="(item, index) in listData"
                :key="index"
                @remove="handleRemove(item)">
                {{ item.name }}
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
    import _ from 'lodash';
    import {
        ref,
        watch,
        shallowRef,
    } from 'vue';
    import AppManageService from '@service/app-manage';
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

    const isLoading = ref(false);
    const listData = shallowRef([]);

    // 根据 ID 获取组件详情
    const fetchData = _.throttle(() => {
        isLoading.value = true;
        AppManageService.fetchHostOfDynamicGroup({
            id: props.data.map(({ id }) => id).join(','),
        })
        .then((data) => {
            listData.value = data;
        })
        .finally(() => {
            isLoading.value = false;
        });
    }, 100);

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            fetchData();
        } else {
            listData.value = [];
        }
    }, {
        immediate: true,
    });

    // 移除单个分组
    const handleRemove = (removeTarget) => {
        const result = props.data.reduce((result, item) => {
            if (removeTarget !== item) {
                result.push(item);
            }
            return result;
        }, []);

        emits('change', 'group', result);
    };

    // 移除所有分组
    const handlRemoveAll = () => {
        emits('change', 'group', []);
    };
</script>
