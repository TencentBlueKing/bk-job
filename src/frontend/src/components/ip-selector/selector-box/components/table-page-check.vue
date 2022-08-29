<template>
    <div
        class="ip-selector-table-pagination-check"
        :class="{ disabled }">
        <span
            v-if="value==='all'"
            class="all-checked"
            @click="handleAllCheckCancle" />
        <bk-checkbox
            v-else
            :checked="value === 'page'"
            @change="handlePageChange" />
        <i
            class="bk-ipselector-icon bk-ipselector-jiantou"
            style="font-size: 18px;" />
        <div class="pagination-check-menu">
            <div class="pop-menu">
                <div
                    class="item"
                    @click="handleChange('page')">
                    本页全选
                </div>
                <div
                    class="item"
                    @click="handleChange('all')">
                    跨页全选
                </div>
            </div>
        </div>
    </div>
</template>
<script setup>
    const props = defineProps({
        value: {
            type: String,
            default: '',
        },
        disabled: {
            type: Boolean,
            default: false,
        },
    });

    const emits = defineEmits(['change']);

    const triggerChange = (actionType) => {
        emits('change', actionType);
    };
    // 取消跨页全选
    const handleAllCheckCancle = () => {
        triggerChange('allCancle');
    };
    // 切换本页全选
    const handlePageChange = (value) => {
        if (!value) {
            triggerChange('pageCancle');
        } else {
            triggerChange('page');
        }
    };
    // 切换本页全选、跨页全选
    const handleChange = (value) => {
        if (value === props.value) {
            return;
        }
        if (props.value === 'all' && value === 'page') {
            return;
        }
        triggerChange(value);
    };
</script>
<style lang="postcss" scoped>
    .ip-selector-table-pagination-check {
        position: relative;
        z-index: 9999999;
        display: inline-flex;
        height: 40px;
        vertical-align: middle;
        align-items: center;

        &.disabled {
            color: #dcdee5;
            pointer-events: none;
            cursor: not-allowed;
        }

        &:hover {
            .pagination-check-menu {
                display: block;
            }
        }

        .all-checked {
            position: relative;
            display: inline-block;
            width: 16px;
            height: 16px;
            vertical-align: middle;
            background: #fff;
            border: 1px solid #3a84ff;
            border-radius: 2px;

            &::after {
                position: absolute;
                top: 1px;
                left: 4px;
                width: 4px;
                height: 8px;
                border: 2px solid #3a84ff;
                border-top: 0;
                border-left: 0;
                content: "";
                transform: rotate(45deg) scaleY(1);
                transform-origin: center;
            }
        }

        .bk-ipselector-open-line {
            margin-left: 5px;
        }

        .pagination-check-menu {
            position: absolute;
            top: 34px;
            left: 0;
            z-index: 10000;
            display: none;
            width: 80px;
            margin: 0 -14px;
            font-size: 12px;
            line-height: 32px;
            text-align: center;
            cursor: pointer;
            background: #fff;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            box-shadow: 0 2px 6px 0 rgb(0 0 0 / 10%);

            .item {
                padding: 0 14px;

                &:hover {
                    color: #3a84ff;
                    background: #f5f6fa;
                }
            }
        }
    }
</style>
