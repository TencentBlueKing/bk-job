<template>
    <div class="ip-selector-agent-status">
        <span v-html="statusIcon" />
        {{ statusText }}
    </div>
</template>
<script setup>
    import { computed } from 'vue';
    const props = defineProps({
        data: {
            type: Number,
        },
    });

    const statusIcon = computed(() => {
        if (props.data === 1) {
            return '<svg style="width: 1em; height: 1em; margin-right: 5px; vertical-align: middle;"><use xlink:href="#bk-ipselector-normal" /></svg>';
        } else if (props.data === 0) {
            return '<svg style="width: 1em; height: 1em; margin-right: 5px; vertical-align: middle;"><use xlink:href="#bk-ipselector-abnormal" /></svg>';
        }
        return '<svg style="width: 1em; height: 1em; margin-right: 5px; vertical-align: middle;"><use xlink:href="#bk-ipselector-unknown" /></svg>';
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
