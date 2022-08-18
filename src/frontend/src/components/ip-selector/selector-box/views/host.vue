<template>
    <div>
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【静态拓扑】</span>
                <span>
                    - 共
                    <span class="bk-ip-selector-number">131231</span>
                    台，新增
                    <span class="bk-ip-selector-number-success">131231</span>
                    台，删除
                    <span class="bk-ip-selector-number-error">131231</span>
                    台
                </span>
            </template>
            <render-table
                :data="listData"
                :pagination="pagination" />
        </collapse-box>
    </div>
</template>
<script setup>
    import {
        ref,
        watch,
        shallowRef,
        reactive,
    } from 'vue';
    import AppManageService from '@service/app-manage';
    import CollapseBox from './components/collapse-box/index.vue';
    import RenderTable from '../components/render-table/host';

    const props = defineProps({
        data: {
            type: Array,
            default: () => [],
        },
    });

    const pagination = reactive({
        count: 0,
        current: 1,
        limit: 10,
    });

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
</script>
<style lang="postcss">
    .root {
        display: block;
    }
</style>
