<template>
    <div class="ip-selector-agent-statistics">
        <template v-if="loading">
            <div class="bk-ip-selector-rotate-loading">
                <svg style="width: 1em; height: 1em; vertical-align: middle; fill: currentcolor;">
                    <use xlink:href="#bk-ipselector-loading" />
                </svg>
            </div>
        </template>
        <template v-else>
            <template v-if="data">
                <div
                    class="statistics-item"
                    @click="handleClick('')">
                    共<span class="bk-ip-selector-number">{{ data.total_count }}</span>台主机
                </div>
                <div
                    v-if="data.alive_count"
                    class="statistics-item"
                    @click="handleClick('normal')">
                    正常：<span class="bk-ip-selector-number-success">{{ data.alive_count }}</span>台
                </div>
                <div
                    v-if="data.not_alive_count"
                    class="statistics-item"
                    @click="handleClick('abnormal')">
                    异常：<span class="bk-ip-selector-number-error">{{ data.not_alive_count }}</span>台
                </div>
            </template>
            <span v-else>--</span>
        </template>
    </div>
</template>
<script setup>
    defineProps({
        data: {
            type: Object,
        },
        loading: {
            type: Boolean,
            default: true,
        },
    });

    const emits = defineEmits(['select']);

    const handleClick = (agentStatus) => {
        emits('select', agentStatus);
    };

</script>
<style lang="postcss">
    .ip-selector-agent-statistics {
        display: flex;

        .statistics-item {
            display: flex;
            height: 24px;
            padding: 0 4px;
            align-items: center;
            border-radius: 2px;

            &:hover {
                background: #eaebf0;
            }

            & ~ & {
                margin-left: 20px;
            }
        }
    }
</style>
