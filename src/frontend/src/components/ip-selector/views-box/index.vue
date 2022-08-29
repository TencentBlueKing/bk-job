<template>
    <div class="bk-ip-selector-views">
        <search-host
            v-if="searchKey"
            :key="searchKey"
            :data="value.hostList" />
        <div
            v-else
            class="views-container">
            <render-host
                v-if="value.hostList && value.hostList.length > 0"
                :data="value.hostList"
                @change="handleChange" />
            <render-node
                v-if="value.nodeList && value.nodeList.length > 0"
                :data="value.nodeList"
                @change="handleChange" />
            <render-dynamic-group
                v-if="value.dynamicGroupList && value.dynamicGroupList.length > 0"
                :data="value.dynamicGroupList"
                @change="handleChange" />
        </div>
    </div>
</template>
<script setup>
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

    const handleChange = (name, value) => {
        console.log('from views change = ', name, value);
        const result = {
            ...props.value,
            ...formatOutput({
                [name]: value,
            }),
        };

        emits('change', result);
    };
</script>
<style lang="postcss">
    .bk-ip-selector-views {
        .views-container {
            & > * {
                margin-top: 16px;
            }
        }
    }
</style>
