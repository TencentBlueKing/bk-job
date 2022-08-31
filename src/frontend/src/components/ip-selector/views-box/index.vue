<template>
    <div class="bk-ip-selector-views">
        <search-host
            v-if="isShowHostSearch"
            :search-key="searchKey"
            :data="value.hostList"
            class="view-host-serach"
            @change="handleChange" />
        <div
            v-show="!searchKey"
            class="views-container">
            <render-host
                v-if="value.hostList && value.hostList.length > 0"
                ref="hostRef"
                :data="value.hostList"
                @change="handleChange" />
            <render-node
                v-if="value.nodeList && value.nodeList.length > 0"
                ref="nodeRef"
                :data="value.nodeList"
                @change="handleChange" />
            <render-dynamic-group
                v-if="value.dynamicGroupList && value.dynamicGroupList.length > 0"
                ref="dynamicGroupRef"
                :data="value.dynamicGroupList"
                @change="handleChange" />
        </div>
    </div>
</template>
<script setup>
    import {
        ref,
        computed,
    } from 'vue';
    import RenderHost from './host.vue';
    import RenderNode from './node.vue';
    import { formatOutput } from '../utils';
    import RenderDynamicGroup from './dynamic-group.vue';
    import SearchHost from './components/search-host.vue';

    const props = defineProps({
        value: {
            type: Object,
            default: () => ({
                hostList: [],
                nodeList: [],
                dynamicGroupList: [],
            }),
        },
        searchKey: {
            type: String,
        },
    });

    const emits = defineEmits(['change']);

    const hostRef = ref();
    const nodeRef = ref();
    const dynamicGroupRef = ref();

    const isShowHostSearch = computed(() => {
        if (!props.value.hostList || props.value.hostList.length < 1) {
            return false;
        }
        return Boolean(props.searchKey);
    });

    const handleChange = (name, value) => {
        const result = {
            ...props.value,
            ...formatOutput({
                [name]: value,
            }),
        };

        emits('change', result);
    };

    defineExpose({
        getHostIpList () {
            if (!hostRef.value) {
                return [];
            }
            return hostRef.value.getHostIpList();
        },
        getAbnormalHostIpList () {
            if (!hostRef.value) {
                return [];
            }
            return hostRef.value.getAbnormalHostIpList();
        },
        refresh () {
            hostRef.value && hostRef.value.refresh();
            nodeRef.value && nodeRef.value.refresh();
            dynamicGroupRef.value && dynamicGroupRef.value.refresh();
        },
    });
</script>
<style lang="postcss">
    .bk-ip-selector-views {
        .view-host-serach {
            margin-top: 16px;
        }

        .views-container {
            & > * {
                margin-top: 16px;
            }
        }
    }
</style>
