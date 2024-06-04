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

package com.tencent.bk.job.common.sharding.mysql;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

public class PartitionByMurmurHash {
    private static final int DEFAULT_VIRTUAL_BUCKET_TIMES = 16;
    private static final int DEFAULT_WEIGHT = 1;
    private int seed;
    private int count;
    private int virtualBucketTimes = DEFAULT_VIRTUAL_BUCKET_TIMES;
    private Map<Integer, Integer> weightMap = new HashMap<>();

    private HashFunction hash;

    private TreeMap<Integer, Integer> bucketMap;

    public void init() {
        bucketMap = new TreeMap<>();
        generateBucketMap();
    }

    private void generateBucketMap() {
        hash = Hashing.murmur3_32(seed);//计算一致性哈希的对象
        for (int i = 0; i < count; i++) {//构造一致性哈希环，用TreeMap表示
            StringBuilder hashName = new StringBuilder("SHARD-").append(i);
            for (int n = 0, shard = virtualBucketTimes * getWeight(i); n < shard; n++) {
                bucketMap.put(hash.hashUnencodedChars(hashName.append("-NODE-").append(n)).asInt(), i);
            }
        }
        weightMap = null;
    }

    /**
     * 得到桶的权重，桶就是实际存储数据的DB实例
     * 从0开始的桶编号为key，权重为值，权重默认为1。
     * 键值必须都是整数
     *
     * @param bucket
     * @return
     */
    private int getWeight(int bucket) {
        Integer w = weightMap.get(bucket);
        if (w == null) {
            w = DEFAULT_WEIGHT;
        }
        return w;
    }

    /**
     * 虚拟节点倍数，virtualBucketTimes*count就是虚拟结点数量
     *
     * @param virtualBucketTimes
     */
    public void setVirtualBucketTimes(int virtualBucketTimes) {
        this.virtualBucketTimes = virtualBucketTimes;
    }

    /**
     * 计算hash值
     *
     * @param columnValue
     * @return
     */
    public Integer calculate(String columnValue) {
        //返回大于等于这个hash值得key
        SortedMap<Integer, Integer> tail = bucketMap.tailMap(hash.hashUnencodedChars(columnValue).asInt());
        if (tail.isEmpty()) {
            return bucketMap.get(bucketMap.firstKey());
        }
        return tail.get(tail.firstKey());
    }

    private static void hashTest(Integer virtualBucketTimes, int count) throws IOException {
        PartitionByMurmurHash hash = new PartitionByMurmurHash();
        hash.count = count;//分片数
        hash.setVirtualBucketTimes(virtualBucketTimes);
        hash.init();

        int[] bucket = new int[hash.count];

        Map<Integer, List<Integer>> hashed = new HashMap<>();

        int total = 10_000_000;//数据量一千万
        for (int i = 100_000_000; i < total + 100_000_000; i++) {//假设分片键从1亿开始
            //计算hash值
            int h = hash.calculate(Integer.toString(i));
            //更新对应节点的数据量
            bucket[h]++;
            //记录每个节点的数据值
            List<Integer> list = hashed.computeIfAbsent(h, k -> new ArrayList<>());
            list.add(i);
        }
        //节点索引
        int idx = 0;
        System.out.println("节点    数据量   占比");
        for (int i : bucket) {
            //计算每个节点的数据量占比
            double ratio = i / (double) total;
            //计算总比例，所有节点数量加起来，最终应该等于1
            //累加数据量，后面打印出来应该与总数量相等
            //打印每个节点的数据量和数据占比
            System.out.println((idx++) + "  " + i + "   " + ratio);
        }

        List<VirtualNode> virtualNodes = new ArrayList<>();
        //获取虚拟节点key和实际对应的节点
        for (Map.Entry<Integer, Integer> entry : hash.bucketMap.entrySet()) {
            virtualNodes.add(new VirtualNode(entry.getKey(), entry.getValue()));
        }

        System.out.println("*************虚拟节点与物理节点映射关系***************");
        //打印映射关系
        System.out.println(virtualNodes);

    }

    @Getter
    @Setter
    private static class VirtualNode {
        private Integer hash;
        private Integer realNode;

        public VirtualNode(Integer hash, Integer realNode) {
            this.hash = hash;
            this.realNode = realNode;
        }

        @Override
        public String toString() {
            return new StringJoiner("", VirtualNode.class.getSimpleName() + "[", "]")
                .add(String.valueOf(hash))
                .add("=")
                .add(String.valueOf(realNode))
                .toString();
        }
    }

    public static void main(String[] args) throws IOException {
        testNodeDataDistribution();
//        testMigration();
    }
    private static void testNodeDataDistribution() throws IOException {
        System.out.println("******************设虚拟节点数16,物理节点为2测试******************");
        hashTest(16, 2);
        System.out.println("\n\n");
        System.out.println("******************虚拟节点数为0，物理节点为2测试******************");
        hashTest(1, 2);
        System.out.println("\n\n");
        System.out.println("******************虚拟节点数3为，物理节点为3测试******************");
        hashTest(3, 3);
        System.out.println("\n\n");
        System.out.println("******************虚拟节点数为100，物理节点为4测试******************");
        hashTest(100, 4);
    }

    private static void testMigration() throws IOException {
        System.out.println("***********************数据迁移测试******************");
        System.out.println("物理节点为2，数据分布情况");
        hashTest(10, 2);
        System.out.println("\n\n");
        System.out.println("物理节点为3，数据分布情况");
        hashTest(10, 3);

        System.out.println("\n\n");
        System.out.println("物理节点为4，数据分布情况");
        hashTest(10, 4);


    }
}

