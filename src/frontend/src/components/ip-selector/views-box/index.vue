<template>
    <div class="bk-ip-selector-views">
        <search-host
            v-if="isShowHostSearch"
            class="view-host-serach"
            :data="lastHostList"
            :search-key="searchKey"
            @change="handleChange" />
        <div
            v-show="!searchKey"
            class="views-container">
            <render-host
                v-if="lastHostList && lastHostList.length > 0"
                ref="hostRef"
                :data="lastHostList"
                @change="handleChange" />
            <render-node
                v-if="lastNodeList && lastNodeList.length > 0"
                ref="nodeRef"
                :data="lastNodeList"
                @change="handleChange" />
            <render-dynamic-group
                v-if="lastDynamicGroupList && lastDynamicGroupList.length > 0"
                ref="dynamicGroupRef"
                :data="lastDynamicGroupList"
                @change="handleChange" />
        </div>
    </div>
</template>
<script setup>
    import {
        computed,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import {
        formatInput,
        formatOutput,
     } from '../utils';

    import SearchHost from './components/search-host.vue';
    import RenderDynamicGroup from './dynamic-group.vue';
    import RenderHost from './host.vue';
    import RenderNode from './node.vue';

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

    const lastHostList = shallowRef([]);
    const lastNodeList = shallowRef([]);
    const lastDynamicGroupList = shallowRef([]);

    const hostRef = ref();
    const nodeRef = ref();
    const dynamicGroupRef = ref();

    const isShowHostSearch = computed(() => {
        if (!props.value.hostList || props.value.hostList.length < 1) {
            return false;
        }
        return Boolean(props.searchKey);
    });

    let isInnerChange = false;

    watch(() => props.value, () => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        
        const {
            host_list: hostList,
            node_list: nodeList,
            dynamic_group_list: dynamicGroupList,
        } = formatInput(props.value || {});

        lastHostList.value = hostList;
        lastNodeList.value = nodeList;
        lastDynamicGroupList.value = dynamicGroupList;
    }, {
        immediate: true,
    });

    const handleChange = (name, value) => {
        switch (name) {
            case 'hostList':
                lastHostList.value = value;
                break;
            case 'nodeList':
                lastNodeList.value = value;
                break;
            case 'dynamicGroupList':
                lastDynamicGroupList.value = value;
                break;
        }
        
        isInnerChange = true;
        emits('change', formatOutput({
            hostList: lastHostList.value,
            nodeList: lastNodeList.value,
            dynamicGroupList: lastDynamicGroupList.value,
        }));
    };

    defineExpose({
        getHostIpList () {
            if (!hostRef.value) {
                return [];
            }
            return hostRef.value.getHostIpList();
        },
        getNotAlivelHostIpList () {
            if (!hostRef.value) {
                return [];
            }
            return hostRef.value.getNotAlivelHostIpList();
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
