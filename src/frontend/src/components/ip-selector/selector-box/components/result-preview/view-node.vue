<template>
    <CollapseBox v-if="data.length > 0">
        <template #title>
            <div>
                <span>【动态拓扑】- 共</span>
                <span class="number">{{ data.length }}</span>
                <span>个，</span>
                <span>新增 1  个</span>
            </div>
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
                {{ item.namePath }}
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
    import _ from 'lodash';
    import {
        ref,
        shallowRef,
        watch,
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

    const fetchData = () => {
        isLoading.value = true;
        AppManageService.fetchNodePath(props.data)
            .then((data) => {
                listData.value = data.reduce((result, item) => {
                    const namePath = item.map(({ instanceName }) => instanceName).join('/');
                    const tailNode = _.last(item);
                    result.push({
                        node: tailNode,
                        namePath,
                    });
                    return result;
                }, []);
            })
            .finally(() => {
                isLoading.value = false;
            });
    };

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            fetchData();
        } else {
            listData.value = [];
        }
    }, {
        immediate: true,
    });

    const handleRemove = (removeTarget) => {
        const result = props.data.reduce((result, item) => {
            if (removeTarget !== item) {
            result.push(item);
            }
            return result;
        }, []);

        emits('change', 'node', result);
    };

    const handlRemoveAll = () => {
        emits('change', 'node', []);
    };
</script>
