<template>
    <div
        class="ip-selector-panel-tab-item"
        :class="{
            active,
            disabled,
        }"
        @click="handleClick">
        <div class="drag-btn">
            <i class="bk-ipselector-icon bk-ipselector-ketuodong" />
        </div>
        <slot />
    </div>
</template>
<script setup>
    const props = defineProps({
        active: {
            type: Boolean,
            required: true,
        },
        disabled: {
            type: Boolean,
            required: true,
        },
    });

    const emits = defineEmits(['change']);

    const handleClick = () => {
        if (props.disabled) {
            return;
        }
        emits('change');
    };
</script>
