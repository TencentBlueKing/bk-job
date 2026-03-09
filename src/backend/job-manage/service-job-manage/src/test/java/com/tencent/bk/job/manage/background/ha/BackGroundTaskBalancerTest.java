/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.manage.background.ha;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 后台任务均衡器单元测试 - 对核心逻辑needToBalance和chooseTaskForThreadCostFromTail方法进行测试
 */
@ExtendWith(MockitoExtension.class)
public class BackGroundTaskBalancerTest {

    @Mock
    private BackGroundTask backGroundTask1;

    @Mock
    private BackGroundTask backGroundTask2;

    @Mock
    private BackGroundTask backGroundTask3;

    private BackGroundTaskBalancer backGroundTaskBalancer;

    @BeforeEach
    public void init() {
        backGroundTaskBalancer = new BackGroundTaskBalancer(
            null,
            null,
            null,
            null,
            null
        );
    }

    @Test
    @DisplayName("needToBalance - 任务列表为空时返回false")
    public void testNeedToBalanceWhenEmptyTaskList() {
        // 准备测试数据 - 空任务列表
        List<BackGroundTask> taskList = Collections.emptyList();
        // 执行测试
        boolean result = backGroundTaskBalancer.needToBalance(10, 0, taskList);
        // 验证结果
        assertFalse(result);
    }

    @Test
    @DisplayName("needToBalance - 当前实例线程消耗低于平均值，不需要负载均衡")
    public void testNeedToBalanceWhenCurrentCostLowerThanAverage() {
        // 准备测试数据
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2);
        // 执行测试
        boolean result = backGroundTaskBalancer.needToBalance(10, 5, taskList);
        // 验证结果
        assertFalse(result);
    }

    @Test
    @DisplayName("needToBalance - 当前实例线程消耗高于平均值，但是移除最后一个任务后消耗小于平均值，处于临界状态")
    public void testNeedToBalanceWhenCurrentCostHigherThanAverageButCannotBalance() {
        // 准备测试数据
        when(backGroundTask2.getThreadCost()).thenReturn(3);
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2);
        // 执行测试
        boolean result = backGroundTaskBalancer.needToBalance(10, 12, taskList);
        // 验证结果 - 12 - 3 = 9 < 10，不需要均衡
        assertFalse(result);
    }

    @Test
    @DisplayName("needToBalance - 当前实例线程消耗高于平均值，并且移除最后一个任务后消耗大于等于平均值，需要均衡")
    public void testNeedToBalanceWhenCurrentCostHigherThanAverageAndCanBalance() {
        // 准备测试数据
        when(backGroundTask3.getThreadCost()).thenReturn(3);
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2, backGroundTask3);
        // 执行测试
        boolean result = backGroundTaskBalancer.needToBalance(10, 15, taskList);
        // 验证结果 - 15 - 3 = 12 >= 10，需要均衡
        assertTrue(result);
    }

    @Test
    @DisplayName("chooseTaskForThreadCostFromTail - 目标消耗为0时返回空列表")
    public void testNeedToBalanceWhenTargetThreadCostIsZero() {
        // 准备测试数据
        when(backGroundTask3.getThreadCost()).thenReturn(3);
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2, backGroundTask3);
        // 执行测试
        List<BackGroundTask> result = backGroundTaskBalancer.chooseTaskForThreadCostFromTail(taskList, 0);
        // 验证结果
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("chooseTaskForThreadCostFromTail - 从尾部选择1个任务，精确匹配目标消耗")
    public void testNeedToBalanceWhenChooseOneTaskForExactMatch() {
        // 准备测试数据
        when(backGroundTask3.getThreadCost()).thenReturn(3);
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2, backGroundTask3);
        // 执行测试
        List<BackGroundTask> result = backGroundTaskBalancer.chooseTaskForThreadCostFromTail(taskList, 3);
        // 验证结果 - 应该只选择最后一个任务
        assertEquals(1, result.size(), "应该只选择1个任务");
        assertTrue(result.contains(backGroundTask3), "应该选择消耗为3的任务");
    }

    @Test
    @DisplayName("chooseTaskForThreadCostFromTail - 从尾部选择多个任务，精确匹配目标消耗")
    public void testNeedToBalanceWhenChooseMultiTasksForExactMatch() {
        // 准备测试数据
        when(backGroundTask2.getThreadCost()).thenReturn(1);
        when(backGroundTask3.getThreadCost()).thenReturn(3);
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2, backGroundTask3);
        // 执行测试
        List<BackGroundTask> result = backGroundTaskBalancer.chooseTaskForThreadCostFromTail(taskList, 4);
        // 验证结果 - 总消耗1+3=4，所以选择这两个任务
        assertEquals(2, result.size(), "应该选择2个任务");
        assertTrue(result.contains(backGroundTask3), "应该选择消耗为3的任务");
        assertTrue(result.contains(backGroundTask2), "应该选择消耗为1的任务");
    }

    @Test
    @DisplayName("chooseTaskForThreadCostFromTail - 从尾部选择多个任务，但目标消耗值未精确匹配，需去除最后选择的一个")
    public void testNeedToBalanceWhenChooseMultiTasksForNotExactMatch() {
        // 准备测试数据
        when(backGroundTask1.getThreadCost()).thenReturn(4);
        when(backGroundTask2.getThreadCost()).thenReturn(1);
        when(backGroundTask3.getThreadCost()).thenReturn(3);
        List<BackGroundTask> taskList = Arrays.asList(backGroundTask1, backGroundTask2, backGroundTask3);
        // 执行测试
        List<BackGroundTask> result = backGroundTaskBalancer.chooseTaskForThreadCostFromTail(taskList, 5);
        // 验证结果 - 最后3个任务总消耗：4+1+3=8>5，最后2个任务总消耗：1+3=4<5，所以选择最后两个任务
        assertEquals(2, result.size(), "应该选择2个任务");
        assertTrue(result.contains(backGroundTask3), "应该选择消耗为3的任务");
        assertTrue(result.contains(backGroundTask2), "应该选择消耗为1的任务");
    }
}
