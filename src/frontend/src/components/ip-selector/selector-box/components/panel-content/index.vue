<template>
    <div
        v-bkloading="{ isLoading: !Boolean(type) }"
        class="ip-selector-panel-content">
        <component
            :is="renderCom"
            v-bind="$attrs"
            v-on="listeners" />
    </div>
</template>
<script>
    export default {
        inheritAttrs: false,
    };
</script>
<script setup>
    import {
        computed,
        useListeners,
    } from 'vue';

    import RenderDynamicGroup from './dynamic-group.vue';
    import RenderDynamicTopo from './dynamic-topo/index.vue';
    import RenderManualInput from './manual-input.vue';
    import RenderServiceTemplate from './service-template.vue';
    import RenderSetTemplate from './set-template.vue';
    import RenderStaticTopo from './static-topo.vue';

    const props = defineProps({
        type: {
            type: String,
            required: true,
        },
    });

    const listeners = useListeners();

    const comMap = {
        staticTopo: RenderStaticTopo,
        dynamicTopo: RenderDynamicTopo,
        dynamicGroup: RenderDynamicGroup,
        serviceTemplate: RenderServiceTemplate,
        setTemplate: RenderSetTemplate,
        manualInput: RenderManualInput,
    };

    const renderCom = computed(() => comMap[props.type] || 'div');
</script>
<style lang="postcss">
    .ip-selector-panel-content {
        height: 100%;
        padding: 16px 24px 0 0;
    }
</style>
