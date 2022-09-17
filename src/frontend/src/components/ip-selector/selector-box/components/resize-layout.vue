<template>
    <div
        ref="rootRef"
        class="ip-selector-resize-layout">
        <div
            ref="leftRef"
            class="layout-left"
            :style="layoutLeftStyles">
            <slot />
        </div>
        <div
            ref="rightRef"
            class="layout-right"
            :style="layoutRightStyles">
            <slot name="right" />
        </div>
        <div
            v-if="flexDirection === 'left'"
            class="left-divid"
            :style="leftBtnStyles"
            @mousedown="handleMousedown('left', $event)">
            <div
                class="resize-btn-left"
                :class="{
                    'is-expanded': isLeftExpanded,
                }"
                @click="handleToggleLeftExpanded">
                <span>
                    <ip-selector-icon type="close-line" />
                </span>
            </div>
            <ip-selector-icon
                v-if="isLeftExpanded"
                class="move-dot"
                type="more" />
        </div>
        <div
            v-if="flexDirection === 'right'"
            class="right-divid"
            :style="rightBtnStyles"
            @mousedown="handleMousedown('right', $event)">
            <div
                class="resize-btn-right"
                :class="{
                    'is-expanded': isRightExpanded,
                }"
                @click="handleToggleRightExpanded">
                <span>
                    <ip-selector-icon type="close-line" />
                </span>
            </div>
            <ip-selector-icon
                v-if="isRightExpanded"
                class="move-dot"
                type="more" />
        </div>
    </div>
</template>
<script setup>
    import _ from 'lodash';
    import {
        computed,
        onBeforeUnmount,
        onMounted,
        ref,
    } from 'vue';

    import IpSelectorIcon from '../../common/ip-selector-icon';
    
    const props = defineProps({
        flexDirection: {
            type: String,
            default: 'left',
        },
        defaultWidth: {
            type: Number,
        },
        maxWidth: {
            type: Number,
        },
        minWidth: {
            type: Number,
        },
    });

    const rootRef = ref();
    const leftRef = ref();
    const rightRef = ref();

    const boxWidth = ref();
    const moveOffset = ref(0);
    const startWidth = ref(props.defaultWidth);

    const getLastLeftWidth = () => {
        if (props.flexDirection !== 'left') {
            return '';
        }
        const newWidth = startWidth.value - moveOffset.value;
        return newWidth <= props.defaultWidth * 0.6 ? 0 : newWidth;
    };
    const getLastRightWidth = () => {
        if (props.flexDirection !== 'right') {
            return '';
        }
        const newWidth = startWidth.value + moveOffset.value;
        return newWidth <= props.defaultWidth * 0.6 ? 0 : newWidth;
    };
    const lastLeftWidth = ref('');
    const lastRightWidth = ref('');

    const isLeftExpanded = computed(() => Number(lastLeftWidth.value) > 0);
    const isRightExpanded = computed(() => Number(lastRightWidth.value) > 0);
    
    const layoutLeftStyles = computed(() => {
        const styles = {};
        if (props.flexDirection === 'left') {
            styles.flex = `0 0 ${lastLeftWidth.value}px`;
            styles.overflow = 'hidden';
            styles.width = `${lastLeftWidth.value}px`;
            if (lastLeftWidth.value === 0) {
                styles.height = '0px';
            }
        }
        
        return styles;
    });
    const layoutRightStyles = computed(() => {
        const styles = {};
        if (props.flexDirection === 'right') {
            styles.flex = `0 0 ${lastRightWidth.value}px`;
            styles.overflow = 'hidden';
            styles.width = `${lastRightWidth.value}px`;
            if (lastRightWidth.value === 0) {
                styles.height = '0px';
            }
        }
        
        return styles;
    });
    const leftBtnStyles = computed(() => ({
        left: `${lastLeftWidth.value - 3}px`,
    }));
    const rightBtnStyles = computed(() => ({
        right: `${lastRightWidth.value - 3}px`,
    }));

    let isResizeable = false;
    let startClientX = 0;
    const handleToggleLeftExpanded = () => {
        lastLeftWidth.value = lastLeftWidth.value < 1 ? props.defaultWidth : 0;
    };

    const handleToggleRightExpanded = () => {
        lastRightWidth.value = lastRightWidth.value < 1 ? props.defaultWidth : 0;
    };

    const handleMousedown = (direction, event) => {
        isResizeable = true;
        startClientX = event.clientX;
        
        boxWidth.value = rootRef.value.getBoundingClientRect().width;

        const moveEl = direction === 'right' ? rightRef.value : leftRef.value;
        startWidth.value = moveEl.getBoundingClientRect().width;
        document.body.style.userSelect = 'none';
    };

    const handleMousemove = _.throttle((event) => {
        if (!isResizeable) {
            return;
        }
        const { clientX } = event;
        moveOffset.value = startClientX - clientX;
        lastLeftWidth.value = getLastLeftWidth();
        lastRightWidth.value = getLastRightWidth();
    }, 30);
    
     const handleMouseup = () => {
        isResizeable = false;
        document.body.style.userSelect = '';
        moveOffset.value = 0;
    };

    onMounted(() => {
        boxWidth.value = rootRef.value.getBoundingClientRect().width;
        lastLeftWidth.value = getLastLeftWidth();
        lastRightWidth.value = getLastRightWidth();
        
        document.body.addEventListener('mousemove', handleMousemove);
        document.body.addEventListener('mouseup', handleMouseup);
        onBeforeUnmount(() => {
            document.body.removeEventListener('mousemove', handleMousemove);
            document.body.removeEventListener('mouseup', handleMouseup);
        });
    });
</script>
<style lang="postcss">
    .ip-selector-resize-layout {
        position: relative;
        display: flex;
        width: 100%;
        height: 100%;

        .layout-left,
        .layout-right {
            display: block;
            overflow: auto;
            flex: 1;
        }

        .left-divid,
        .right-divid {
            position: absolute;
            top: 0;
            bottom: 0;
            display: flex;
            align-items: center;
            width: 5px;
            cursor: ew-resize;

            &:hover {
                &::after {
                    background: #3a84ff;
                }
            }

            &::after {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 2px;
                width: 1px;
                background: #dcdee5;
                content: "";
                transition: all 0.15s;
            }

            .move-dot {
                position: absolute;
                font-size: 18px;
                color: #c4c6cc;
            }
        }

        .left-divid {
            .move-dot {
                left: -4px;
            }
        }

        .right-divid {
            .move-dot {
                right: -4px;
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

            span {
                transition: all 0.15s;
            }
        }

        .resize-btn-left {
            left: 2px;
            border-radius: 0 4px 4px 0;

            &.is-expanded {
                span {
                    transform: rotateZ(-180deg);
                }
            }
        }

        .resize-btn-right {
            right: 2px;
            border-radius: 4px 0 0 4px;

            &.is-expanded {
                span {
                    transform: rotateZ(0);
                }
            }

            span {
                transform: rotateZ(-180deg);
            }
        }
    }
</style>
