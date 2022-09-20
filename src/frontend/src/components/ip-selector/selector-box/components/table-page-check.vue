<template>
    <div
        class="ip-selector-table-page-check"
        :class="{ disabled }">
        <span
            v-if="value==='all'"
            class="all-checked"
            @click="handleAllCheckCancle" />
        <bk-checkbox
            v-else
            :checked="value === 'page'"
            @change="handlePageChange" />
        <span
            ref="handlerRef"
            :disabled="disabled"
            style="font-size: 18px;">
            <ip-selector-icon type="jiantou" />
        </span>
        <div
            ref="popRef"
            class="pagination-check-menu">
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
    import tippy from 'tippy.js';
    import {
        onMounted,
        ref,
    } from 'vue';

    import IpSelectorIcon from '../../common/ip-selector-icon';

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

    const handlerRef = ref();
    const popRef = ref();

    let tippyInstance;

    const triggerChange = (actionType) => {
        if (props.disabled) {
            return;
        }
        emits('change', actionType);
        tippyInstance.hide();
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

    onMounted(() => {
        tippyInstance = tippy(handlerRef.value, {
            content: popRef.value,
            placement: 'bottom',
            appendTo: () => document.body,
            theme: 'light',
            maxWidth: 'none',
            trigger: 'click',
            interactive: true,
            arrow: false,
            offset: [0, 8],
            zIndex: 999999,
            hideOnClick: true,
        });
    });
</script>
<style lang="postcss" scoped>
    .ip-selector-table-page-check {
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

        .menu-flag {
            margin-left: 2px;
            transition: all 0.15s;
        }

        .bk-ipselector-open-line {
            margin-left: 5px;
        }
    }

    .pagination-check-menu {
        width: 80px;
        margin: -5px -9px;
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
</style>
