<template>
    <div class="ip-selector-view-group">
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【动态分组】</span>
                <span>
                    - 共
                    <span class="bk-ip-selector-number">131231</span>
                    个，新增
                    <span class="bk-ip-selector-number-success">131231</span>
                    个，删除
                    <span class="bk-ip-selector-number-error">131231</span>
                    个
                </span>
            </template>
            <table>
                <tr
                    v-for="row in tableData"
                    :key="row.id">
                    <td>{{ row.name }}</td>
                </tr>
            </table>
        </collapse-box>
    </div>
</template>
<script setup>
    import CollapseBox from './components/collapse-box/index.vue';
    import _ from 'lodash';
    import {
        ref,
        watch,
        shallowRef,
    } from 'vue';
    import AppManageService from '@service/app-manage';

    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
    });
    defineEmits(['change']);

    const isLoading = ref(false);
    const tableData = shallowRef([]);

    // 根据 ID 获取组件详情
    const fetchData = _.throttle(() => {
        isLoading.value = true;
        AppManageService.fetchHostOfDynamicGroup({
            id: props.data.map(({ id }) => id).join(','),
        })
        .then((data) => {
            tableData.value = data;
        })
        .finally(() => {
            isLoading.value = false;
        });
    }, 100);

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            fetchData();
        } else {
            tableData.value = [];
        }
    }, {
        immediate: true,
    });
</script>
<style lang="postcss">
    @import "../styles/table-mixin.css";

    .ip-selector-view-group {
        @include table;
    }
</style>
