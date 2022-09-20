<template>
    <div class="result-preview-item">
        <div class="item-text">
            <slot />
        </div>
        <div>
            <slot name="append" />
        </div>
        <div class="item-action">
            <div
                v-if="content"
                v-bk-tooltips="'复制'"
                class="item-btn"
                @click="handleCopy">
                <ip-selector-icon type="copy" />
            </div>
            <div
                v-if="removable"
                v-bk-tooltips="'删除'"
                class="item-btn"
                @click="handleRemove">
                <ip-selector-icon type="close-line-2" />
            </div>
        </div>
    </div>
</template>
<script setup>
    import IpSelectorIcon from '../../../../common/ip-selector-icon';
    import { execCopy } from '../../../../utils';

    const props = defineProps({
        content: {
            type: String,
        },
        removable: {
            type: Boolean,
            default: true,
        },
    });
    const emits = defineEmits(['remove']);

    // 复制 content
    const handleCopy = () => {
        execCopy(props.content);
    };

    // 删除
    const handleRemove = () => {
        emits('remove');
    };
</script>
<style lang="postcss" scoped>
    .result-preview-item {
        display: flex;
        height: 32px;
        padding-left: 12px;
        overflow: hidden;
        font-size: 12px;
        color: #63656e;
        background: #fff;
        border-radius: 2px;
        opacity: 100%;
        transition: all 0.15s;
        align-items: center;

        &:nth-child(n+2) {
            margin-top: 2px;
        }

        &:hover {
            background: #e1ecff;

            .item-action {
                display: flex;
            }
        }

        .item-text {
            margin-right: 12px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            direction: rtl;
            flex: 0 1 auto;
            align-items: center;
        }

        .item-action {
            display: none;
            padding: 0 5px;
            margin-left: auto;

            .item-btn {
                display: flex;
                height: 100%;
                padding: 0 5px;
                margin-left: auto;
                overflow: hidden;
                color: #3a84ff;
                cursor: pointer;
                align-items: center;
                justify-content: center;

                &:hover {
                    color: #1768ef;
                }
            }
        }
    }
</style>
