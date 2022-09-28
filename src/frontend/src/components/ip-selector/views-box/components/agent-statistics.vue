<template>
    <div class="ip-selector-agent-statistics">
        <template v-if="loading">
            <div class="bk-ip-selector-rotate-loading">
                <ip-selector-icon
                    style="width: 1em; height: 1em; vertical-align: middle; fill: currentcolor;"
                    svg
                    type="loading" />
            </div>
        </template>
        <template v-else>
            <template v-if="data">
                <div
                    class="statistics-item statistics-item-btn"
                    @click="handleClick('')">
                    共<span class="bk-ip-selector-number">{{ data.total_count }}</span>台主机
                </div>
                <div
                    class="statistics-item">
                    正常<span class="bk-ip-selector-number-success">{{ data.alive_count }}</span>台
                </div>
                <div
                    class="statistics-item">
                    异常<span class="bk-ip-selector-number-error">{{ data.not_alive_count }}</span>台
                </div>
            </template>
            <span v-else>--</span>
        </template>
    </div>
</template>
<script setup>
    import IpSelectorIcon from '../../common/ip-selector-icon';

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
            width: 130px;
            height: 24px;
            border-radius: 2px;
            align-items: center;

            & ~ & {
                margin-left: 2px;
            }
        }

        .statistics-item-btn {
            &:hover {
                background: #eaebf0;
            }
        }

        .bk-ip-selector-number,
        .bk-ip-selector-number-success,
        .bk-ip-selector-number-error {
            padding: 0 4px;
        }
    }
</style>
