<template>
    <div class="batch-box">
        <div class="batch-pre">
            <Icon type="arrow-full-down" />
        </div>
        <div
            ref="list"
            class="batch-list">
            <div class="batch-item" key="all">全部批次</div>
            <div
                v-for="i in 32"
                class="batch-item"
                :class="{ active: selectBatchIndex === i }"
                :key="i"
                @click="handleSelectBatch(i)">
                第 {{ i }} 批
            </div>
        </div>
        <div class="batch-next">
            <Icon type="arrow-full-down" />
        </div>
        <div class="batch-more">
            <Icon type="more" />
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            data: Object,
        },
        data () {
            return {
                selectBatchIndex: 1,
            };
        },
        mounted () {
            this.init();
        },
        methods: {
            init () {
                const $list = this.$refs.list;
                const $itemList = $list.querySelectorAll('.batch-item');
                const maxWidth = $list.getBoundingClientRect().width;

                let itemMaxWidth = 0;
                let renderItemNums = 0;
                $itemList.forEach((item) => {
                    const {
                        width,
                    } = item.getBoundingClientRect();
                    itemMaxWidth += width;
                    if (itemMaxWidth <= maxWidth) {
                        renderItemNums += 1;
                    }
                });

                console.log('asdadad == ', renderItemNums);
            },
            handleSelectBatch (selectBatchIndex) {
                this.selectBatchIndex = selectBatchIndex;
            },
        },
    };
</script>
<style lang="postcss">
    .batch-box {
        display: flex;
        padding: 20px 24px 12px;
        background: #f5f6fa;

        .batch-pre,
        .batch-next,
        .batch-more {
            display: flex;
            flex: 1 0 auto;
            width: 28px;
            height: 28px;
            color: #c4c6cc;
            cursor: pointer;
            background: #e8e9f0;
            border-radius: 50%;
            justify-content: center;
            align-items: center;

            &:hover {
                background: #f0f1f5;
            }
        }

        .batch-pre {
            margin-right: 8px;

            i {
                transform: rotateZ(90deg);
            }
        }

        .batch-next {
            margin-left: 8px;

            i {
                transform: rotateZ(-90deg);
            }
        }

        .batch-more {
            margin-left: 6px;
        }

        .batch-list {
            display: inline-flex;
            padding: 2px;
            overflow: hidden;
            font-size: 12px;
            color: #63656e;
            background: #e4e6ed;
            border-radius: 18px;
            user-select: none;

            .batch-item {
                display: flex;
                flex: 1 0 auto;
                align-items: center;
                justify-content: center;
                height: 24px;
                padding: 0 16px;
                cursor: pointer;
                transition: all 0.15s;

                &:hover {
                    background: #f0f1f5;
                    border-radius: 12px;
                }

                &.active {
                    cursor: default;
                    background: #fff;
                    border-radius: 12px;
                    box-shadow: 0 1px 2px 0 rgb(0 0 0 / 10%);
                }
            }
        }
    }
</style>
