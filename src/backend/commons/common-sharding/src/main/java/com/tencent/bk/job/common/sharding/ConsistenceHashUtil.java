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

package com.tencent.bk.job.common.sharding;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistenceHashUtil {

    //存储所有节点，按照hash值排序的
    @Getter
    private SortedMap<Long, String> virtualNodes = new TreeMap<>();

    // 设置虚拟节点的个数
    private static final int VIRTUAL_NODES = 3;


    public ConsistenceHashUtil() {
    }


    public ConsistenceHashUtil(SortedMap<Long, String> virtualTableNodes, Collection<String> tableNodes) {
        if (Objects.isNull(virtualTableNodes)) {
            virtualTableNodes = initNodesToHashLoop(tableNodes);
        }

        this.virtualNodes = virtualTableNodes;
    }

    public SortedMap<Long, String> initNodesToHashLoop(Collection<String> tableNodes) {
        SortedMap<Long, String> virtualTableNodes = new TreeMap<>();
        for (String node : tableNodes) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String s = String.valueOf(i);
                String virtualNodeName = node + "-manji" + s;
                long hash = getHash(virtualNodeName);

                virtualTableNodes.put(hash, virtualNodeName);
            }
        }

        return virtualTableNodes;
    }

    /**
     * 通过计算key的hash
     * 计算映射的表节点
     *
     * @param key
     * @return
     */
    public String getTableNode(String key) {
        String virtualNode = getVirtualTableNode(key);
        //虚拟节点名称截取后获取真实节点
        if (!StringUtils.isEmpty(virtualNode)) {
            return virtualNode.substring(0, virtualNode.indexOf("-"));
        }
        return null;
    }

    /**
     * 获取虚拟节点
     *
     * @param key
     * @return
     */
    public String getVirtualTableNode(String key) {
        long hash = getHash(key);
        // 得到大于该Hash值的所有Map
        SortedMap<Long, String> subMap = virtualNodes.tailMap(hash);
        String virtualNode;
        if (subMap.isEmpty()) {
            //如果没有比该key的hash值大的，则从第一个node开始
            Long i = virtualNodes.firstKey();
            //返回对应的服务器
            virtualNode = virtualNodes.get(i);
        } else {
            //第一个Key就是顺时针过去离node最近的那个结点
            Long i = subMap.firstKey();
            //返回对应的服务器
            virtualNode = subMap.get(i);
        }

        return virtualNode;
    }

    /**
     * 使用FNV1_32_HASH算法计算key的Hash值
     * 也可以使用 MurmurHash3 或者别的加密方式
     *
     * @param key
     * @return
     */
    public long getHash(String key) {
//        return MurmurHash3.murmurhash3_x86_32(key);
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < key.length(); i++)
            hash = (hash ^ key.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        // 如果算出来的值为负数则取其绝对值
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }


    public static void main2(String[] args) {

        ConsistenceHashUtil consistenceHashUtil = new ConsistenceHashUtil();
        SortedMap<Long, String> longStringSortedMap = consistenceHashUtil.initNodesToHashLoop(Arrays.asList("user_0",
            "user_1", "user_2"));

        consistenceHashUtil.virtualNodes = longStringSortedMap;

        Map<String, Integer> map = new HashMap<>();

        for (int k = 0; k < 1000; k++) {
            long hash = consistenceHashUtil.getHash(String.valueOf(k));

            SortedMap<Long, String> subMap = consistenceHashUtil.virtualNodes.tailMap(hash);
            String virtualNode;
            if (subMap.isEmpty()) {
                //如果没有比该key的hash值大的，则从第一个node开始
                Long i = consistenceHashUtil.virtualNodes.firstKey();
                //返回对应的服务器
                virtualNode = consistenceHashUtil.virtualNodes.get(i);
            } else {
                //第一个Key就是顺时针过去离node最近的那个结点
                Long i = subMap.firstKey();
                //返回对应的服务器
                virtualNode = subMap.get(i);
            }

            map.merge(virtualNode.split("-")[0], 1, Integer::sum);
        }

        System.out.println(map);
    }

    public static void main(String[] args) {

        ConsistenceHashUtil consistenceHashUtil = new ConsistenceHashUtil();
        SortedMap<Long, String> longStringSortedMap = consistenceHashUtil.initNodesToHashLoop(Arrays.asList("user_0",
            "user_1", "user_2", "user_3"));
        SortedMap<Long, String> longStringSortedMap2 = consistenceHashUtil.initNodesToHashLoop(Arrays.asList("user_0"
            , "user_1", "user_2"));

        consistenceHashUtil.virtualNodes = longStringSortedMap;

        System.out.println(longStringSortedMap);
    }
}
