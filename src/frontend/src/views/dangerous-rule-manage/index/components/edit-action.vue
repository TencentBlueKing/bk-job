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
    <div class="dangerous-rule-edit-mode">
        <bk-select
            ref="select"
            :clearable="false"
            :value="value"
            @change="handleChange"
            @toggle="handleSelectToggle">
            <bk-option
                v-for="item in actionList"
                :id="item.id"
                :key="item.id"
                :name="item.name" />
        </bk-select>
        <div
            v-show="!isEditing"
            class="value-box"
            @click.stop="handleEdit">
            <div
                class="action-text"
                :class="textClass">
                {{ text }}
            </div>
            <i class="bk-icon icon-angle-down value-box-arrow" />
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
                type: Number,
                require: true,
            },
        },
        data () {
            return {
                isEditing: false,
            };
        },
        computed: {
            text () {
                return _.find(this.actionList, _ => _.id === this.value).name;
            },
            textClass () {
                const classMap = {
                    1: 'normal',
                    2: 'hight',
                };
                return classMap[this.value];
            },
        },
        created () {
            this.actionList = [
                {
                    id: 1,
                    name: I18n.t('dangerousRule.扫描'),
                },
                {
                    id: 2,
                    name: I18n.t('dangerousRule.拦截'),
                },
            ];
        },
        methods: {
            /**
             * @desc 开始编辑
             */
            handleEdit () {
                this.isEditing = true;
                this.$nextTick(() => {
                    this.$refs.select.$el.querySelector('.bk-select-name').click();
                });
            },
            /**
             * @desc 下拉面板收起，取消编辑状态
             */
            handleSelectToggle (toggle) {
                if (!toggle) {
                    this.isEditing = false;
                }
            },
            /**
             * @desc 触发change 事件
             */
            handleChange (value) {
                this.$emit('on-change', value);
            },
        },
    };
</script>
<style lang='postcss'>
    .dangerous-rule-edit-mode {
        position: relative;
        padding-left: 10px;
        margin-left: -10px;

        .value-box {
            position: absolute;
            top: 0;
            left: 0;
            z-index: 1;
            display: flex;
            width: 100%;
            height: 100%;
            padding-left: 10px;
            cursor: pointer;
            background: #fff;
            align-items: center;

            &:hover {
                background: #f0f1f5;

                .action-text {
                    &.normal {
                        color: #63656e;
                        background: #e1e3eb;
                    }

                    &.hight {
                        color: #e63535;
                        background: #fdd;
                    }
                }

                .value-box-arrow {
                    display: block;
                }
            }

            .action-text {
                display: inline-block;
                height: 18px;
                padding: 0 5px;
                font-size: 12px;
                line-height: 18px;
                cursor: pointer;
                border-radius: 2px;

                &.normal {
                    color: #979ba5;
                    background: #f0f1f5;
                }

                &.hight {
                    color: #ea3636;
                    background: #ffebeb;
                }
            }

            .value-box-arrow {
                position: absolute;
                top: 2px;
                right: 2px;
                display: none;
                font-size: 22px;
                color: #979ba5;
            }
        }
    }
</style>
