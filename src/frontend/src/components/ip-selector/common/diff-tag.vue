<template>
    <ip-selector-icon
        v-if="value"
        style=" width: 1em; height: 1em; margin-right: 5px; margin-left: 4px; font-size: 20px; vertical-align: middle;"
        svg
        :type="iconType" />
</template>
<script setup>
    import { computed } from 'vue';

    import IpSelectorIcon from './ip-selector-icon';

    const props = defineProps({
        value: {
            type: String,
        },
    });

    const iconMap = {
        new: 'xinzeng',
        remove: 'shanchu',
        invalid: 'shixiao',
        repeat: 'miaobianqianhuangse',
    };
    const iconType = computed(() => iconMap[props.value]);
</script>
