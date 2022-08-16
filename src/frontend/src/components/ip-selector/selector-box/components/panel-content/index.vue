<template>
    <div class="ip-selector-panel-content">
        <component
            :is="renderCom"
            v-bind="$attrs"
            v-on="listeners" />
    </div>
</template>
<script setup>
    import {
        computed,
        useListeners,
    } from 'vue';
    import RenderCustomInput from './render-custom-input.vue';
    import RenderDynamicTopo from './dynamic-topo/index.vue';
    import RenderServiceTemplate from './render-service-template.vue';
    import RenderSetTemplate from './render-set-template.vue';
    import RenderStaticTopo from './render-static-topo.vue';
    import RenderDynamicGroup from './dynamic-group.vue';

    const props = defineProps({
        type: {
            type: String,
            required: true,
        },
    });

    const listeners = useListeners();

    const comMap = {
        dynamicTopo: RenderDynamicTopo,
        staticTopo: RenderStaticTopo,
        serviceTemplate: RenderServiceTemplate,
        setTemplate: RenderSetTemplate,
        customInput: RenderCustomInput,
        group: RenderDynamicGroup,
    };

    const renderCom = computed(() => comMap[props.type]);
</script>
<style lang="postcss">
    .ip-selector-panel-content {
        height: 527px;
        padding: 16px 24px 0 16px;
    }
</style>
