<template>
    <CollapseBox v-if="data.length > 0">
        <template #title>
            <div>
                <span>【静态拓扑】- 共 8 台，新增 5 台，删除 1 台</span>
            </div>
        </template>
        <template #action>
            <CollapseExtendAction>
                <div @click="handleCopyIP">
                    复制 IP
                </div>
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
                {{ item.ip }}
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
    import {
        ref,
        watch,
        shallowRef,
    } from 'vue';
    import AppManageService from '@service/app-manage';
    import CallapseContentItem from './collapse-box/content-item.vue';
    import CollapseExtendAction from './collapse-box/extend-action.vue';
    import CollapseBox from './collapse-box/index.vue';
    import { execCopy } from '../../utils';

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
        AppManageService.fetchHostOfHost({
            hostIdList: props.data.map(({ hostId }) => hostId),
        })
        .then((data) => {
            listData.value = data;
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
    });
    // 移除单个IP
    const handleRemove = (removeTarget) => {
        const result = props.data.reduce((result, item) => {
            if (removeTarget !== item) {
                result.push(item);
            }
            return result;
        }, []);

        emits('change', 'host', result);
    };
    // 复制IP
    const handleCopyIP = () => {
        execCopy(props.data.map(({ ip }) => ip).join('\n'), `复制成功 ${props.data.length} 个 IP`);
    };
    // 移除所有IP
    const handlRemoveAll = () => {
        emits('change', 'host', []);
    };
</script>
