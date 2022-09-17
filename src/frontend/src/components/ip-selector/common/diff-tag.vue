<template>
    <span
        v-if="value"
        style="margin-left: 4px; font-size: 20px;">
        <ip-selector-icon
            style="width: 1em; height: 1em; margin-right: 5px; vertical-align: middle;"
            svg
            :type="iconType" />
    </span>
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
