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
    <card-layout
        v-bkloading="{ isLoading, opacity: 0.8 }"
        class="tag-dashboard"
        :title="$t('dashboard.标签')">
        <div
            ref="box"
            class="tag-box"
            style="position: releative; width: 100%; height: 240px;" />
    </card-layout>
</template>
<script>
    import _ from 'lodash';

    import StatisticsService from '@service/statistics';

    import CardLayout from '../card-layout';

    export default {
        name: '',
        components: {
            CardLayout,
        },
        data () {
            return {
                isLoading: true,
            };
        },
        watch: {
            date () {
                this.fetchData();
            },
        },

        created () {
            this.textList = [];
        },
        mounted () {
            this.fetchData();
            const resize = _.throttle(() => {
                this.init(this.textList);
            }, 300);
            window.addEventListener('resize', resize);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', resize);
            });
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                StatisticsService.fetchDistributionMetrics({
                    date: this.date,
                    metric: 'TAG',
                }).then((data) => {
                    this.data = data.labelAmountMap;
                    const tagNumList = Object.values(this.data).sort((a, b) => a - b);
                    const [tagNumMin] = tagNumList;
                    const tagNumMax = tagNumList[tagNumList.length - 1];

                    const weightQueue = [];
                    let weightMax = 1;
                    while (weightMax <= 5) {
                        weightQueue.push(weightMax);
                        weightMax = weightMax + 1;
                    }

                    // 判断每个值得显示权重
                    const weigthMap = {};
                    const checkWeight = (max, secondMax, min) => {
                        const maxWeight = weightQueue[weightQueue.length - 1] || 1;
                        weigthMap[max] = maxWeight;

                        let nextMin = min;
                        if (secondMax < min) {
                            weigthMap[max] = maxWeight;
                            weightQueue.pop();
                            nextMin = secondMax - Math.max((secondMax - tagNumMin) / weightQueue.length, 1);
                            // 最大权重间隔维持在两个级别
                            if (nextMin < min) {
                                weightQueue.pop();
                            }
                        }
                        if (tagNumList.length > 0) {
                            checkWeight(secondMax, tagNumList.pop(), nextMin);
                        }
                    };
                    
                    checkWeight(
                        tagNumList.pop(),
                        tagNumList.pop(),
                        tagNumMax - Math.max((tagNumMax - tagNumMin) / weightQueue.length, 1),
                    );

                    const textList = Object.keys(this.data).reduce((result, key) => {
                        const count = this.data[key];
                        result.push({
                            text: key,
                            weight: weigthMap[count],
                            count,
                        });
                        return result;
                    }, []);
                    this.textList = textList;
                    this.init(textList);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            init (wordList) {
                const start = (wordArray) => {
                    // 容器元素的引用
                    const boxClientRect = this.$refs.box.getBoundingClientRect();
                    // 默认选项值
                    const options = {
                        width: boxClientRect.width,
                        height: boxClientRect.height,
                        center: {
                            x: boxClientRect.width / 2.0,
                            y: boxClientRect.height / 2.0,
                        },
                        delayedMode: wordArray.length > 50,
                        shape: false, // 默认为椭圆形状
                    };
                    // Helper函数来测试如果一个元素重叠
                    const hitTest = function (elem, otherElems) {
                        // 两两重叠检测
                        const overlapping = function (a, b) {
                            return !((a.offsetLeft + a.offsetWidth) < b.offsetLeft - 3
                                || (b.offsetLeft + b.offsetWidth) < a.offsetLeft - 3
                                || a.offsetTop + a.offsetHeight < b.offsetTop - 5
                                || b.offsetTop + b.offsetHeight < a.offsetTop - 5);
                        };
                        let i = 0;
                        // 检查元素重叠一个接一个,停止并返回false一旦发现重叠
                        // eslint-disable-next-line no-plusplus
                        for (i = 0; i < otherElems.length; i++) {
                            if (overlapping(elem, otherElems[i])) {
                                return true;
                            }
                        }
                        return false;
                    };

                    const drawWordCloud = () => {
                        // 确保每一个重量之前是一个数字排序
                        wordArray.forEach((item, index) => {
                            wordArray[index].weight = parseFloat(wordArray[index].weight, 10);
                        });

                        // 排序wordArray从最高的词体重最低的一个
                        wordArray.sort((a, b) => {
                            if (a.weight < b.weight) {
                                return 1;
                            } else if (a.weight > b.weight) {
                                return -1;
                            }
                            return 0;
                        });

                        const step = 3.0;
                        const alreadyPlacedWords = [];
                        const aspectRatio = options.width / options.height;

                        // 函数画一词,在螺旋通过移动它,直到找到一个合适的空地方。这将是迭代每个单词。
                        const drawOneWord = (word, index) => {
                            let angle = 6.28 * Math.random();
                            let radius = 0.0;
                            let weight = 5;

                            const wordSpan = document.createElement('span');
                            wordSpan.setAttribute('tippy-tips', `${word.text} ${word.count}`);
                            wordSpan.setAttribute('placement', 'right');

                            // 检查是否min(重量)> max(重量)否则使用默认
                            if (wordArray[0].weight > wordArray[wordArray.length - 1].weight) {
                                // 线性映射原体重一个离散的规模从1到6
                                weight = Math.round((word.weight - wordArray[wordArray.length - 1].weight)
                                    / (wordArray[0].weight - wordArray[wordArray.length - 1].weight) * 4.0) + 1;
                            }
                            const wordStyle = wordSpan.style;
                            wordStyle.lineHeight = 1;
                            wordSpan.classList.add(`w${weight}`);
                            wordSpan.append(word.text);
                            this.$refs.box.append(wordSpan);
                            const { width, height } = wordSpan.getBoundingClientRect();
                            let left = options.center.x - width / 2.0;
                            let top = options.center.y - height / 2.0;

                            // 保存样式属性的引用,获得更好的性能
                            wordStyle.position = 'absolute';
                            wordStyle.left = `${left}px`;
                            wordStyle.top = `${top}px`;

                            while (hitTest(wordSpan, alreadyPlacedWords)) {
                                // 选择形状是矩形的移动这个词在一个矩形螺旋
                                radius += step;
                                angle += (index % 2 === 0 ? 1 : -1) * step;

                                left = options.center.x - (width / 2.0) + (radius * Math.cos(angle)) * aspectRatio;
                                top = options.center.y + radius * Math.sin(angle) - (height / 2.0);
                                wordStyle.left = `${left}px`;
                                wordStyle.top = `${top}px`;
                            }

                            // 移除超过容器范围的词
                            if (left < 0
                                || top < 0
                                || Math.ceil(left + width + 5) > options.width
                                || Math.ceil(top + height + 5) > options.height) {
                                this.$refs.box.removeChild(wordSpan);
                                return;
                            }

                            alreadyPlacedWords.push(wordSpan);
                        };

                        const drawOneWordDelayed = (index = 0) => {
                            if (index < wordArray.length) {
                                drawOneWord(wordArray[index], index);
                                this.drawTimer = setTimeout(() => {
                                    drawOneWordDelayed(index + 1);
                                }, 20);
                            }
                        };

                        // 迭代drawOneWord上每一个字。迭代的方式完成取决于绘图模式(delayedMode是真或假的)
                        if (options.delayedMode) {
                            drawOneWordDelayed();
                        } else {
                            wordArray.forEach(drawOneWord);
                        }
                    };

                    // 延迟执行，降低渲染标签的优先级
                    setTimeout(() => {
                        drawWordCloud();
                    });
                };
                this.$refs.box.innerHTML = '';
                clearTimeout(this.drawTimer);
                start(wordList);
            },
        },
    };
</script>
<style lang='postcss'>
    .tag-dashboard {
        .tag-box {
            position: relative;
            font-size: 10px;

            span {
                z-index: 1;
                line-height: 1;
                white-space: nowrap;
                cursor: pointer;
                transition: font-weight 0.15s, transform 0.15s;

                &:hover {
                    font-weight: bold;
                    opacity: 100%;
                    transform: scale(1.2);
                }

                &.w5 {
                    font-size: 28px;
                    color: #4031a3;
                }

                &.w4 {
                    font-size: 24px;
                    color: #3d6dcc;
                }

                &.w3 {
                    font-size: 20px;
                    color: #4da8d6;
                }

                &.w2 {
                    font-size: 16px;
                    color: #55c29e;
                }

                &.w1 {
                    font-size: 12px;
                    color: #92c288;
                }
            }
        }
    }
</style>
