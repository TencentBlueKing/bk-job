<template>
    <div class="render-notify-way-check">
        <bk-checkbox
            class="check-all"
            v-bind="allCheckStatus"
            @click.native="handleCheckToggle">
            {{ $t('notify.全选') }}
        </bk-checkbox>
        <bk-checkbox-group
            class="check-item"
            :value="localValue"
            @change="handleChange">
            <bk-checkbox
                v-for="(channelItem) in channelList"
                :key="channelItem.code"
                :value="channelItem.code">
                {{ channelItem.name }}
            </bk-checkbox>
        </bk-checkbox-group>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            channelList: {
                type: Array,
                default: () => [],
            },
            value: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                localValue: [],
            };
        },
        computed: {
            /**
             * @desc 全选状态
             * @returns { Object }
             */
            allCheckStatus () {
                if (this.localValue.length < 1) {
                    return {
                        checked: false,
                        indeterminate: false,
                    };
                }
                const allChannelList = this.channelList.map(({ code }) => code);
                let checked = true;
                let indeterminate = false;
                allChannelList.forEach((item) => {
                    if (!this.localValue.includes(item)) {
                        checked = false;
                    }
                });
                indeterminate = !checked;
                if (allChannelList.length < 1) {
                    checked = false;
                    indeterminate = false;
                }
                return {
                    checked,
                    indeterminate,
                };
            },
        },
        watch: {
            value: {
                handler (value) {
                    this.localValue = value;
                },
                immediate: true,
            },
        },
        methods: {
            handleCheckToggle () {
                if (this.allCheckStatus.checked) {
                    this.handleChange([]);
                } else {
                    const allChannelCode = this.channelList.map(({ code }) => code);
                    this.handleChange(allChannelCode);
                }
            },
            handleChange (value) {
                this.localValue = value;
                this.$emit('on-change', this.localValue);
            },
        },
    };
</script>
<style lang="postcss" scoped>
    .render-notify-way-check {
        display: flex;
        align-items: center;

        .check-all {
            flex: 0 0 auto;
            margin-right: 25px;
        }

        .check-item {
            flex: 0 0 auto;
        }
    }
</style>
