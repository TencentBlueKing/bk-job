<template>
    <span v-if="value" style="font-size: 20px;">
        <svg style="width: 1em; height: 1em; margin-right: 5px; vertical-align: middle;">
            <use
                v-if="value === 'new'"
                xlink:href="#bk-ipselector-xinzeng" />
            <use
                v-if="value === 'remove'"
                xlink:href="#bk-ipselector-shanchu" />
            <use
                v-if="value === 'invalid'"
                xlink:href="#bk-ipselector-shixiao" />
            <use
                v-if="value === 'repeat'"
                xlink:href="#bk-ipselector-miaobianqianhuangse" />
        </svg>
    </span>
</template>
<script setup>
    defineProps({
        value: {
            type: String,
        },
    });

</script>
