<template>
    <div class="ip-selector-views">
        <search-host
            v-if="searchKey"
            :key="searchKey"
            :data="data.host" />
        <template v-else>
            <render-host :data="data.host" />
            <render-node :data="data.node" />
            <render-group :data="data.group" />
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
                host: [],
                node: [],
                group: [],
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
