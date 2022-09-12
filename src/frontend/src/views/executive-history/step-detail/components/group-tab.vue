<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
    <div
        class="step-execute-host-group"
        :class="{ hide: isHide }">
        <div
            ref="groupTab"
            class="group-tab"
            :class="{ toggle: showGroupToggle }">
            <div
                v-for="(item) in renderGroup"
                :key="item.resultType + item.tag"
                class="tab-item"
                :class="{
                    active: value.groupName === item.groupName,
                }"
                @click="handleGroupChange(item)">
                <div
                    v-bk-overflow-tips
                    class="group-name">
                    {{ item.groupName }}
                </div>
                <Icon
                    v-if="item.tagMaxLength"
                    v-bk-tooltips="$t('history.分组标签长度最大支持256，超过会被自动截断，请留意！')"
                    class="max-length-info"
                    type="info" />
                <div class="group-nums">
                    {{ item.agentTaskSize }}
                </div>
            </div>
            <div
                v-if="showGroupToggle"
                class="group-toggle"
                :class="groupToggleClass">
                <div class="tab-more">
                    <div class="group-holder">
                        {{ groupHolder }}
                    </div>
                    <Icon
                        class="toggle-flag"
                        type="arrow-full-right" />
                </div>
                <div class="dropdown-menu">
                    <div
                        v-for="(item, index) in toggleGroup"
                        :key="index"
                        class="dropdowm-item"
                        :class="{
                            active: value.groupName === item.groupName,
                        }"
                        @click="handleGroupChange(item)">
                        <div
                            v-bk-overflow-tips
                            class="group-name">
                            {{ item.groupName }}
                        </div>
                        <Icon
                            v-if="item.tagMaxLength"
                            v-bk-tooltips="$t('history.分组标签长度最大支持256，超过会被自动截断，请留意！')"
                            class="max-length-info"
                            type="info" />
                        <div class="group-nums">
                            {{ item.agentTaskSize }}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import I18n from '@/i18n';

    export default {
        name: '',
        props: {
            value: {
                type: Object,
                default: () => ({}),
            },
            data: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                isHide: true,
                renderGroup: [],
                toggleGroup: [],
            };
        },
        computed: {
            groupHolder () {
                if (this.toggleGroup.find(_ => _.groupName === this.value.groupName)) {
                    return this.value.groupName;
                }
                return I18n.t('history.更多分组');
            },
            showGroupToggle () {
                return this.toggleGroup.length > 0;
            },
            groupToggleClass () {
                if (this.toggleGroup.find(_ => _.groupName === this.value.groupName)) {
                    return 'active';
                }
                return '';
            },
        },
        watch: {
            /**
             * @desc 没有选中的分组和选中的分组不存在了，默认选中第一个
             */
            data: {
                handler (data) {
                    if (this.data.length < 1) {
                        return;
                    }
                    this.isHide = true;
                    this.renderGroup = Object.freeze(data);
                    setTimeout(() => {
                        this.renderGroupItem();
                    });
                },
                immediate: true,
            },
        },
        /**
         * @desc 浏览器宽度变化时，重新计算分组的排版
         */
        mounted () {
            const resizeHandler = _.throttle(() => {
                this.renderGroup = this.data;
                this.isHide = true;
                this.timer = setTimeout(() => {
                    this.renderGroupItem();
                });
            }, 100);
            window.addEventListener('resize', resizeHandler);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', resizeHandler);
                clearTimeout(this.timer);
            });
        },
        methods: {
            /**
             * @desc 计算分组的排版，超过一行的分组聚合到最后
             */
            renderGroupItem () {
                const { width } = this.$refs.groupTab.getBoundingClientRect();
                const allGroup = [...this.$refs.groupTab.querySelectorAll('.tab-item')];
                
                let realTabCotentWidth = 0;
                let realDisplayNum = 0;
                // eslint-disable-next-line no-plusplus
                for (realDisplayNum = 0; realDisplayNum < allGroup.length; realDisplayNum++) {
                    const groupWidth = allGroup[realDisplayNum].getBoundingClientRect().width;
                    if (realTabCotentWidth + groupWidth < width) {
                        realTabCotentWidth += groupWidth;
                        continue;
                    } else {
                        break;
                    }
                }
                const isLast = realDisplayNum === allGroup.length - 1;
                if (!isLast) {
                    if (realTabCotentWidth + 124 > width) {
                        realDisplayNum -= 1;
                    }
                }
                this.renderGroup = Object.freeze(this.data.slice(0, realDisplayNum));
                this.toggleGroup = Object.freeze(this.data.slice(realDisplayNum));
                this.isHide = false;
            },
            /**
             * @desc 切换分组
             * @param {Object} group 最新选中的分组
             */
            handleGroupChange (group) {
                const { resultType, tag } = this.value;
                if (resultType === group.resultType && tag === group.tag) {
                    return;
                }
                this.$emit('on-change', group);
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import "@/css/mixins/scroll";

    .step-execute-host-group {
        width: 100%;
        padding: 0 24px;
        background: #f5f6fa;
        border-bottom: 1px solid #e2e2e2;

        &.hide {
            overflow: hidden;

            .group-tab {
                visibility: hidden;
            }
        }

        .group-tab {
            display: flex;
            height: 40px;
            color: #63656e;
            visibility: visible;
            transition: all 0.15s;

            &.toggle {
                .tab-item {
                    margin-right: 0;
                }
            }

            .tab-item,
            .tab-more {
                position: relative;
                display: flex;
                height: 41px;
                padding: 0 20px;
                font-size: 14px;
                color: #63656e;
                cursor: pointer;
                border: 1px solid transparent;
                border-top-right-radius: 6px;
                border-top-left-radius: 6px;
                align-items: center;
                flex: 0 0 auto;
            }

            .tab-item {
                &:hover,
                &.active {
                    color: #313238;

                    .group-nums {
                        color: #63656e;
                    }
                }

                &.active {
                    background: #fff;
                    border-color: #dcdee5;
                    border-bottom-color: #fff;
                }
            }

            .tab-more {
                justify-content: flex-end;
            }

            .group-name {
                max-width: 225px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .group-nums {
                display: inline-block;
                height: 16px;
                min-width: 16px;
                padding: 0 4px;
                margin-left: 8px;
                font-size: 12px;
                font-weight: 500;
                line-height: 16px;
                color: #979ba5;
                text-align: center;
                background: #e6e7eb;
                border-radius: 8px;
            }

            .max-length-info {
                font-size: 16px;
                color: #ea3636;
            }
        }

        .group-toggle {
            position: relative;
            z-index: 999;

            &:hover,
            &.active {
                .tab-more {
                    color: #3a84ff;
                }
            }

            &:hover {
                .toggle-flag {
                    transform: rotateZ(90deg);
                }

                .dropdown-menu {
                    opacity: 100%;
                    visibility: visible;
                    transform: translateY(0);
                }
            }

            .tab-more {
                position: relative;
                z-index: 99;
                width: 124px;
            }

            .dropdown-menu {
                position: absolute;
                top: 50px;
                right: 0;
                width: 280px;
                max-height: 300px;
                padding: 6px 0;
                overflow-y: scroll;
                font-size: 12px;
                background: #fff;
                border: 1px solid #dcdee5;
                opacity: 0%;
                visibility: hidden;
                transform: translateY(-15px);
                transition: all 0.15s;

                @mixin scroller;

                &::before {
                    position: absolute;
                    top: -5px;
                    right: 0;
                    width: 124px;
                    height: 8px;
                    content: "";
                }

                .group-nums {
                    margin-left: auto;
                }
            }

            .dropdowm-item {
                display: flex;
                height: 32px;
                padding: 0 10px;
                cursor: pointer;
                align-items: center;

                &:hover,
                &.active {
                    color: #3a84ff;

                    .group-nums {
                        color: #fff;
                        background: #3a84ff;
                    }
                }

                &:hover {
                    background: #f4f6fa;
                }

                &.active {
                    background: #eaf3ff;
                }
            }

            .toggle-flag {
                margin-left: 2px;
                transition: transform 0.15s;
            }
        }

        .group-holder {
            height: 19px;
            overflow: hidden;
            text-overflow: ellipsis;
            word-break: break-all;
            white-space: nowrap;
        }
    }
</style>
