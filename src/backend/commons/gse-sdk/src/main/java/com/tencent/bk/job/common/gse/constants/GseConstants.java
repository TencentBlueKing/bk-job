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

package com.tencent.bk.job.common.gse.constants;

public interface GseConstants {
    /**
     * 直连云区域ID
     */
    int DEFAULT_CLOUD_ID = 0;

    /**
     * GSE 获取文件任务执行结果协议版本V2 - 解除valuekey依赖版本
     */
    int GSE_FILE_PROTOCOL_VERSION_V2 = 2;

    /**
     * GSE 公钥证书BASE64，固定值
     */
    String publicKeyPermBase64 =
        "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlHZk1BMEdDU3FHU0liM0RRRUJBUVVBQTRHTkFEQ0JpUUtCZ1FETEVXZk9" +
            "YU2VvMXpNQ1JpRVNFTWs3OXo0cwpHYkw4VmIvZXg5K1RaR2VyN255bEh5Y0Vtb2o5aWE4K2daTmVQOFRRVmRyTExhSz" +
            "IzektiT3lja2FiVE5QS0VZCmhQY0NlellEQVdleTZBS2ZHSCtYZGV0MnJDOWtzRWhrM1BqcDVuZDk4QW1KZ0VJeSt6S" +
            "0FhaVZEazFvdG5Jc0EKRWxucUdXL24zaWVuN0hmSXN3SURBUUFCCi0tLS0tRU5EIFBVQkxJQyBLRVktLS0tLQo=";
}
