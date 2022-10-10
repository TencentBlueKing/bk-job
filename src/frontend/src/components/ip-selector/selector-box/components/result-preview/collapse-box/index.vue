<template>
    <div class="preview-result-collsapn">
        <div
            class="collspan-header"
            @click="handleToggle">
            <ip-selector-icon
                class="arrow-flag"
                :class="{ 'is-show': isShow }"
                type="jiantou" />
            <div class="header-title">
                <slot name="title" />
            </div>
            <div
                v-if="slots.action"
                class="collspan-action"
                @click.stop="">
                <slot name="action" />
            </div>
        </div>
        <div
            v-if="isShow"
            class="collspan-content">
            <slot />
        </div>
    </div>
</template>
<script setup>
    import {
        ref,
        useSlots,
    } from 'vue';

    import IpSelectorIcon from '../../../../common/ip-selector-icon';

    const slots = useSlots();

    const isShow = ref(true);

    const handleToggle = () => {
        isShow.value = !isShow.value;
    };
</script>
<style lang="postcss" scoped>
    .preview-result-collsapn {
        position: relative;
        user-select: none;

        .collspan-header {
            display: flex;
            height: 32px;
            margin: 0 24px;
            font-size: 12px;
            color: #63656e;
            align-items: center;
            border-radius: 2px;

            .arrow-flag {
                font-size: 18px;
                transform: rotateZ(-90deg);
                transition: all 0.15s;

                &.is-show {
                    transform: rotateZ(0);
                }
            }

            &:hover {
                background: #e1ecff;

                .collspan-action {
                    display: flex;
                }
            }
        }

        .collspan-action {
            display: none;
            padding: 0 5px;
            margin-left: auto;
            color: #3a84ff;

            &:hover {
                color: #1768ef;
            }

            & > * {
                padding: 0 5px;
                cursor: pointer;
            }
        }

        .header-title {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            cursor: pointer;
        }

        .collspan-content {
            padding: 0 24px;
        }
    }
</style>
