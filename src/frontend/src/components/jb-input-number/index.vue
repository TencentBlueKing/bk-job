<template>
    <div
        class="jb-input-number"
        :class="{
            'is-focused': isFocused
        }">
        <input
            class="input-inner"
            type="number"
            :value="lovalValue"
            @blur="handleBlur"
            @focus="handleFocus"
            @input="handleInput">
        <div class="number-action">
            <div
                class="action-btn"
                @click="handleAdd">
                <i class="bk-icon icon-angle-up" />
            </div>
            <div
                class="action-btn"
                @click="handleSub">
                <i class="bk-icon icon-angle-down" />
            </div>
        </div>
    </div>
</template>
<script setup>
    import {
        ref,
        watch,
    } from 'vue';

    const props = defineProps({
        value: {
            type: [Number, String],
        },
        min: {
            type: Number,
            default: Number.MIN_VALUE,
        },
        max: {
            type: Number,
            default: Number.MAX_VALUE,
        },
    });
    const emits = defineEmits([
        'update:modelValue',
        'update:value',
        'input',
        'change',
    ]);

    const isFocused = ref(false);
    const lovalValue = ref(props.value);

    watch(() => props.value, () => {
        lovalValue.value = props.value;
    }, {
        immediate: true,
    });

    const handleInput = (event) => {
        let lastValue = Number(event.target.value);
        if (lastValue < props.min) {
            lastValue = props.min;
        } else if (lastValue > props.max) {
            lastValue = props.max;
        }
        lovalValue.value = lastValue;
        emits('update:value', lovalValue.value);
        emits('input', lovalValue.value);
        emits('change', lovalValue.value);
    };
    const handleFocus = () => {
        isFocused.value = true;
    };
    const handleBlur = (event) => {
        isFocused.value = false;
        if (event.target.value !== `${lovalValue.value}`) {
            event.target.value = lovalValue.value;
        }
    };
    const handleAdd = () => {
        const lastValue = lovalValue.value + 1;
        lovalValue.value = Math.min(lastValue, props.max);
        emits('update:value', lovalValue.value);
        emits('input', lovalValue.value);
        emits('change', lovalValue.value);
    };
    const handleSub = () => {
        const lastValue = lovalValue.value - 1;
        lovalValue.value = Math.max(lastValue, props.min);
        emits('update:value', lovalValue.value);
        emits('input', lovalValue.value);
        emits('change', lovalValue.value);
    };
</script>
<style lang="postcss">
    .jb-input-number {
        position: relative;
        display: block;
        width: 100%;
        padding: 0 10px;
        font-size: 12px;
        line-height: normal;
        color: #63656e;
        text-align: left;
        background-color: #fff;
        border: 1px solid #c4c6cc;
        border-radius: 2px;
        outline: none;
        box-sizing: border-box;
        transition: border 0.2s linear;
        resize: none;

        &.is-focused {
            border-color: #3a84ff;
        }

        .input-inner {
            display: block;
            width: 100%;
            height: 30px;
            padding: 0;
            margin: 0;
            border: none;
            outline: none;
            appearance: textfield;

            &::-webkit-inner-spin-button,
            &::-webkit-outer-spin-button {
                margin: 0;
                appearance: none;
            }
        }

        .number-action {
            position: absolute;
            top: 0;
            right: 0;
            bottom: 0;
            display: flex;
            flex-direction: column;

            .action-btn {
                display: flex;
                width: 26px;
                height: 14px;
                font-size: 20px;
                color: #979ba5;
                cursor: pointer;
                justify-content: center;
                align-items: center;

                &:hover {
                    background: #f4f6fa;
                }
            }
        }
    }
</style>
