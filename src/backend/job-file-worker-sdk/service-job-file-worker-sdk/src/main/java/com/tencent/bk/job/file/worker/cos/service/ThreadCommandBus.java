/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.file.worker.cos.service;

import com.tencent.bk.job.file_gateway.consts.TaskCommandEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class ThreadCommandBus {


    private static final ConcurrentHashMap<String, LinkedBlockingQueue<Command>> map = new ConcurrentHashMap<>();

    public static void destroyCommandQueue(String key) {
        map.remove(key);
    }

    public static Queue<Command> getCommandQueue(String key) {
        return map.computeIfAbsent(key, s -> new LinkedBlockingQueue<>());
    }

    public static void sendCommand(String key, Command command) {
        Queue<Command> queue = getCommandQueue(key);
        if (queue.size() > 100) {
            throw new RuntimeException("Too many commands for " + key);
        } else {
            log.debug("add {} to queue {}", command, key);
            queue.add(command);
            log.debug("queue.size={}", queue.size());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Command {
        // 指令
        TaskCommandEnum cmd;
        // 数据
        Object data;
    }
}
