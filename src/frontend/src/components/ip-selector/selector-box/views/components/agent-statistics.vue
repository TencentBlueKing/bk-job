<template>
    <div class="ip-selector-agent-statistics">
        <div class="statistics-item">共<span class="bk-ip-selector-number">{{ data.totalNum }}</span>台主机</div>
        <div class="statistics-item">正常：<span class="bk-ip-selector-number-success">{{ data.normalNum }}</span>台</div>
        <div class="statistics-item">异常：<span class="bk-ip-selector-number-error">{{ data.abnormalNum }}</span>台</div>
    </div>
</template>
<script setup>
    defineProps({
        data: {
            type: Object,
            required: true,
        },
    });
</script>
<style lang="postcss">
    .ip-selector-agent-statistics {
        display: flex;

        .statistics-item {
            & ~ & {
                margin-left: 20px;
            }
        }
    }
</style>
