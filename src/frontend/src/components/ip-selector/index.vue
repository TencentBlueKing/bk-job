<template>
    <div>asdasdas--{{ test }}</div>
</template>
<script setup>
    import { ref } from 'vue';

    const test = ref('a');
</script>
<style lang="postcss">
    .root {
        display: block;
    }
</style>
