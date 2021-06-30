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
    <div class="choose-ip-server-ip-input">
        <bk-input
            type="textarea"
            :rows="inputRows"
            :value="ipInput"
            :placeholder="$t('请输入 IP 地址，多IP可用 空格 换行 ; , |分隔 \n带云区域请用冒号分隔，如（ 0:192.168.1.101 ）')"
            @change="handleIPChange" />
        <div class="input-action">
            <div class="input-error">
                <span v-if="isError">{{ $t('以上内容因无法识别导致添加失败，请检查是否格式有误、或者IP不存在业务下导致。') }}</span>
            </div>
            <bk-button class="submit-btn" theme="primary" outline :loading="isLoading" @click="handleAddHost">
                <span>{{ $t('添加到已选择') }}</span>
                <div v-if="searchParams.length > 0" ref="inputNumber" class="server-input-number">
                    <span class="text">{{ searchParams.length }}</span>
                </div>
            </bk-button>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import AppManageService from '@service/app-manage';
    import {
        generateHostRealId,
    } from './utils';

    export default {
        name: '',
        inheritAttrs: false,
        props: {
            previewId: {
                type: String,
                required: true,
            },
            dialogHeight: {
                type: Number,
                required: true,
            },
        },
        data () {
            return {
                ipInput: '',
                searchParams: [],
                isLoading: false,
                isError: false,
            };
        },
        computed: {
            inputRows () {
                const offsetTop = 65;
                const offsetBottom = 130;
                const inputPadding = 15;

                return Math.floor((this.dialogHeight - offsetTop - offsetBottom - inputPadding) / 18);
            },
        },
        deactivated () {
            this.errorMemo = this.isError;
            this.isError = false;
        },
        activated () {
            this.isError = this.errorMemo;
        },
        created () {
            this.errorMemo = false;
        },
        methods: {
            /**
             * @desc 根据输入值获取主机信息
             * @param {Object} params 筛选参数
             */
            fetchHost (params) {
                this.$emit('on-loading', true);
                return AppManageService.fetchHostOfHost(params)
                    .then((data) => {
                        const mapIpRealId = list => list.map(item => ({
                            ...item,
                            realId: generateHostRealId(item),
                        }));
                        const uniqIp = list => _.uniqBy(list, item => item.realId);
                        return _.flow([
                            mapIpRealId, uniqIp,
                        ])(data);
                    })
                    .finally(() => {
                        this.$emit('on-loading', false);
                    });
            },
            /**
             * @desc 输入框值改变
             * @param {String} value 用户输入值
             */
            handleIPChange (value) {
                this.ipInput = value;
                const realValue = value.replace(/\s/g, '');
                if (!realValue) {
                    this.searchParams = [];
                    this.$emit('on-input-change', false);
                } else {
                    this.searchParams = value.split(/[;,；，\n\s|-]+/).filter(_ => !!_);
                    this.$emit('on-input-change', true);
                }
                this.isError = false;
            },
            /**
             * @desc 提交输入值时的抛射动画效果
             * @param {Function} callback 动画结束后的回调
             */
            shot (callback = () => {}) {
                this.$emit('on-input-animate', true);
                const $previewTarget = document.querySelector(`#${this.previewId}`);
                const $moveTarget = this.$refs.inputNumber.cloneNode(true);
                const $moveTargetText = $moveTarget.querySelector('.text');
                const targetPosition = $previewTarget.getBoundingClientRect();
                const movePosition = this.$refs.inputNumber.getBoundingClientRect();

                $moveTarget.style.position = 'absolute';
                $moveTarget.style.top = `${movePosition.top}px`;
                $moveTarget.style.left = `${movePosition.left}px`;
                $moveTargetText.style.position = 'absolute';
                document.body.appendChild($moveTarget);
                let offsetX = movePosition.left - targetPosition.left;
                const offsetY = targetPosition.top - movePosition.top;
                if (targetPosition.width > 40) {
                    offsetX -= 60;
                }
                
                setTimeout(() => {
                    $moveTarget.style.transform = `translate(-${offsetX}px)`;
                    $moveTargetText.style.transform = `translateY(${offsetY}px)`;
                }, 20);
                setTimeout(() => {
                    document.body.removeChild($moveTarget);
                    this.$emit('on-input-animate', false);
                    callback();
                }, 825);
            },
            /**
             * @desc 提交输入结果
             */
            handleAddHost () {
                if (this.searchParams.length < 1) {
                    return;
                }
                const searchMap = this.searchParams.reduce((result, item) => {
                    result[item] = true;
                    return result;
                }, {});
                this.isLoading = true;
                
                this.fetchHost(this.searchParams)
                    .then((data) => {
                        if (data.length > 0) {
                            this.shot(() => {
                                this.$emit('on-change', 'hostInput', data);
                            });
                        }
                        this.$emit('on-input-change', false);
                        this.searchParams = [];
                        // 处理输入的无效值
                        data.forEach((item) => {
                            if (searchMap[item.ip]) {
                                delete searchMap[item.ip];
                            }
                            const cloudAreaIP = `${item.cloudAreaInfo.id}:${item.ip}`;
                            if (searchMap[cloudAreaIP]) {
                                delete searchMap[cloudAreaIP];
                            }
                        });
                        
                        const invalidipInput = Object.keys(searchMap);
                        if (invalidipInput.length > 0) {
                            this.ipInput = invalidipInput.join('\n');
                            this.isError = true;
                        } else {
                            this.ipInput = '';
                            this.isError = false;
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
        },
    };
</script>
<style lang="postcss">
    .choose-ip-server-ip-input {
        padding: 20px 24px;

        .input-action {
            display: flex;
            align-items: center;
            margin-top: 20px;
        }

        .input-error {
            margin-right: auto;
            color: #ea3636;
        }

        .submit-btn {
            &:hover {
                .server-input-number .text {
                    color: #3a84ff;
                    background: #fff;
                }
            }
        }
    }

    .server-input-number,
    .server-input-number .text {
        transition: transform 0.8s;
    }

    .server-input-number {
        position: relative;
        z-index: 999999;
        display: inline-block;
        height: 17px;
        transition-timing-function: cubic-bezier(0.74, 0.95, 0.81, 0.92);
    }

    .server-input-number .text {
        display: inline-block;
        height: 17px;
        min-width: 17px;
        padding: 0 4px;
        font-size: 12px;
        line-height: 17px;
        color: #fff;
        text-align: center;
        background: #3a84ff;
        border-radius: 8px;
        transition-timing-function: cubic-bezier(0, -1.12, 0.94, -1.07);
    }
</style>
