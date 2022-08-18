<template>
    <div class="ip-selector-views">
        <search-host
            v-if="searchKey"
            :key="searchKey"
            :data="data.hostList" />
        <template v-else>
            <render-host :data="data.hostList || []" />
            <render-node :data="data.nodeList || []" />
            <render-group :data="data.groupList || []" />
        </template>
    </div>
</template>
<script setup>
    import RenderHost from './host.vue';
    import RenderNode from './node.vue';
    import RenderGroup from './group.vue';
    import SearchHost from './components/search-host.vue';

    defineProps({
        data: {
            type: Object,
            default: () => ({
                hostList: [],
                nodeList: [],
                groupList: [],
            }),
        },
        searchKey: {
            type: String,
        },
    });
</script>
<style lang="postcss">
    .ip-selector-views {
        display: block;
    }
</style>
