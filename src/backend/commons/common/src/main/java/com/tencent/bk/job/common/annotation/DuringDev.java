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

package com.tencent.bk.job.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开发阶段临时标记注解。
 * <p>
 * 用于标记开发过程中为了调试、验证逻辑而临时修改的代码（如硬编码的参数值、临时的配置、
 * 调试用的日志、测试用的条件分支等），以便在开发完成后能够通过 IDE 的 "Find Usages" 功能
 * 快速定位所有被标记的位置，逐一检查并恢复为生产环境的正式配置。
 * <p>
 * <b>核心使用场景：</b>
 * <ul>
 *   <li>临时修改了某个阈值/参数用于本地调试，上线前需要改回生产值</li>
 *   <li>临时硬编码了某个 ID/URL 用于联调，上线前需要改为配置化</li>
 *   <li>临时添加了调试日志或绕过了某些校验逻辑，上线前需要移除</li>
 *   <li>临时注释掉了某段逻辑用于隔离问题，上线前需要恢复</li>
 * </ul>
 * <p>
 * <b>使用方式：</b>
 * <pre>{@code
 * // 示例1：标记临时修改的参数值
 * @DuringDev(description = "调试用，上线前改回生产值 20000", productionValue = "20000")
 * private int shallowThreshold = 100;
 *
 * // 示例2：标记临时添加的调试代码
 * @DuringDev(description = "调试日志，上线前删除")
 * private void debugPrintContainerInfo(List<ContainerDetailDTO> containers) {
 *     containers.forEach(c -> log.info("container: {}", c));
 * }
 *
 * // 示例3：标记临时硬编码的值
 * @DuringDev(description = "联调用硬编码bizId，上线前改为从请求参数获取", productionValue = "从参数获取")
 * long bizId = 2L;
 * }</pre>
 * <p>
 * <b>⚠️ 重要提醒：</b>
 * <ul>
 *   <li>此注解仅用于开发阶段的临时标记，<b>不应该出现在合入主干的代码中</b></li>
 *   <li>提交 MR / PR 前，请通过 IDE 的 "Find Usages" 搜索此注解的所有引用，
 *       确认每一处都已恢复为生产配置或已移除</li>
 *   <li>Code Review 时如果发现此注解残留，应当要求开发者处理后再合入</li>
 * </ul>
 *
 * @see CompatibleImplementation 类似的标记型注解，用于标识兼容历史版本的实现
 */
@SuppressWarnings("unused")
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({
    ElementType.TYPE,
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.LOCAL_VARIABLE,
    ElementType.PARAMETER,
    ElementType.CONSTRUCTOR,
    ElementType.ANNOTATION_TYPE
})
public @interface DuringDev {

    /**
     * 描述为什么要做这个临时修改，以及上线前需要怎么处理。
     * <p>
     * 建议包含以下信息：
     * <ul>
     *   <li>修改的原因（如：调试XX功能、联调XX接口）</li>
     *   <li>上线前的处理方式（如：改回生产值、删除此方法、恢复被注释的代码）</li>
     * </ul>
     * <p>
     * 示例：{@code "调试容器查询分页逻辑，上线前改回生产值"}
     */
    String description() default "";

}
