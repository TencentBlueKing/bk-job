<template>
    <div class="preview-result-collsapn">
        <div
            class="collspan-header"
            @click="handleToggle">
            <i
                class="bk-ipselector-icon bk-ipselector-jiantou arrow-flag"
                :class="{ 'is-show': isShow }" />
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
            height: 34px;
            margin: 0 24px;
            font-size: 12px;
            color: #63656e;
            align-items: center;

            .arrow-flag {
                font-size: 18px;
                transform: rotateZ(-90deg);
                transition: all 0.15s;

                &.is-show {
                    transform: rotateZ(0);
                }
            }

            &:hover {
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
