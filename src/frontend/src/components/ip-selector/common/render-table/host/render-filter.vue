<template>
    <div
        class="ip-selector-host-table-filter"
        :class="{
            active: value !== undefined,
        }">
        <ip-selector-icon type="select" />
        <div class="filter-menu">
            <div
                v-for="item in data"
                :key="item.value"
                class="item"
                :class="{
                    active: value === item.value,
                }"
                @click="handleClick(item)">
                {{ item.name }}
            </div>
        </div>
    </div>
</template>
<script setup>
    import IpSelectorIcon from '../../ip-selector-icon';

    defineProps({
        data: {
            type: Array,
            required: true,
        },
        value: {
            type: [Number, String],
        },
    });

    const emits = defineEmits(['change']);

    const handleClick = (data) => {
        emits('change', data.value);
    };
</script>
<style lang="postcss">
    .ip-selector-host-table-filter {
        position: relative;
        padding-left: 4px;
        font-size: 12px;
        color: #c4c6cc;

        &:hover {
            .filter-menu {
                display: block;
            }
        }

        &.active {
            color: #3a84ff;
        }

        .filter-menu {
            position: absolute;
            display: none;
            font-size: 12px;
            line-height: 32px;
            color: #63656e;
            background: #fff;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            transform: translateX(-50%);
            box-shadow: 0 2px 6px 0 rgb(0 0 0 / 10%);

            .item {
                padding: 0 12px;
                word-break: keep-all;
                white-space: nowrap;
                cursor: pointer;

                &:hover {
                    color: #3a84ff;
                    background: #f5f6fa;
                }

                &.active {
                    color: #3a84ff;
                    background-color: #f4f6fa;
                }
            }
        }
    }
</style>
