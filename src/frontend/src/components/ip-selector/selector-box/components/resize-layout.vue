<template>
    <div class="ip-selector-resize-layout">
        <div
            class="layout-left"
            :style="layoutLeftStyles">
            <slot />
        </div>
        <div
            class="layout-right"
            :style="layoutRightStyles">
            <slot name="right" />
        </div>
        <div
            v-if="flexDirection === 'left'"
            class="left-divid"
            :style="leftBtnStyles"
            @mousedown="handleMousedown">
            <div
                class="resize-btn-left"
                :class="{
                    'is-expanded': isLeftExpanded,
                }"
                @click="handleToggleLeftExpanded">
                <span>&lt;</span>
            </div>
        </div>
        <div
            v-if="flexDirection === 'right'"
            class="right-divid"
            :style="rightBtnStyles"
            @mousedown="handleMousedown">
            <div
                class="resize-btn-right"
                :class="{
                    'is-expanded': isRightExpanded,
                }"
                @click="handleToggleRightExpanded">
                <span>&gt;</span>
            </div>
        </div>
    </div>
</template>
<script setup>
    import {
        computed,
        ref,
    } from 'vue';
    
    const props = defineProps({
        flexDirection: {
            type: String,
            default: 'left',
        },
        defaultWidth: {
            type: Number,
        },
    });

    const isLeftExpanded = ref(true);
    const isRightExpanded = ref(true);

    const layoutLeftStyles = computed(() => ({
        width: props.flexDirection === 'left' ? `${props.defaultWidth}px` : '',
    }));
    const layoutRightStyles = computed(() => ({
        width: props.flexDirection === 'right' ? `${props.defaultWidth}px` : '',
    }));
    const leftBtnStyles = computed(() => ({
        left: `${props.defaultWidth - 1}px`,
    }));
    const rightBtnStyles = computed(() => ({
        right: `${props.defaultWidth - 1}px`,
    }));

    const handleToggleLeftExpanded = () => {
        isLeftExpanded.value = !isLeftExpanded.value;
    };

    const handleToggleRightExpanded = () => {
        isRightExpanded.value = !isRightExpanded.value;
    };

    const handleMousedown = () => {
        
    };
</script>
<style lang="postcss">
    .ip-selector-resize-layout {
        position: relative;
        display: flex;

        .layout-left,
        .layout-right {
            position: absolute;
            top: 0;
            bottom: 0;
        }

        .left-divid,
        .right-divid {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 1px;
            cursor: ew-resize;
            background: #dcdee5;
            transition: all 0.15s;

            &:hover {
                background: #3a84ff;
            }
        }

        .resize-btn-left,
        .resize-btn-right {
            position: absolute;
            top: 50%;
            display: flex;
            width: 16px;
            height: 64px;
            color: #fff;
            cursor: pointer;
            background: #dcdee5;
            transform: translateY(-50%);
            transition: all 0.15s;
            align-items: center;
            justify-content: center;
            user-select: none;

            &:hover {
                background: #3a84ff;
            }

            &.is-expanded {
                span {
                    transform: rotateZ(-180deg);
                }
            }

            span {
                transition: all 0.15s;
            }
        }

        .resize-btn-left {
            left: 0;
            border-radius: 0 4px 4px 0;
        }

        .resize-btn-right {
            right: 0;
            border-radius: 4px 0 0 4px;
        }
    }
</style>
