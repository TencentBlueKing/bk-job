<template>
    <div class="ip-selector-agent-status">
        <ip-selector-icon
            style="margin-right: 4px;"
            svg
            :type="statusIconType" />
        {{ statusText }}
    </div>
</template>
<script setup>
    import { computed } from 'vue';

    import IpSelectorIcon from './ip-selector-icon';
    const props = defineProps({
        data: {
            type: Number,
        },
    });

    const statusIconType = computed(() => {
        if (props.data === 1) {
            return 'normal';
        } else if (props.data === 0) {
            return 'abnormal';
        }
        return 'unknown';
    });

    const statusText = computed(() => {
        if (props.data === 1) {
            return '正常';
        } else if (props.data === 0) {
            return '异常';
        }
        return '未知';
    });
</script>
<style lang="postcss">
    .ip-selector-agent-status {
        display: flex;
        align-items: center;
    }
</style>
