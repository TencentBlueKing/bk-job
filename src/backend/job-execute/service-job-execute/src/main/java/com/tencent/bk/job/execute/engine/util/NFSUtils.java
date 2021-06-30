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

package com.tencent.bk.job.execute.engine.util;

import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;

/**
 * NFS切换工具
 */
public class NFSUtils {

//    private static Logger LOG = LoggerFactory.getLogger(NFSUtils.class);
//
//    private static Map<String, MS> map = Maps.newHashMap();
//
//    private static final String MASTER_SUF = ".master";
//    private static final String SLAVE_SUF = ".slave";
//
//    private static class MS {
//        private FileDirTypeConf tag;
//        private String masterPath;
//        private String slavePath;
//
//        private MS(FileDirTypeConf tag) {
//            this.tag = tag;
//            try {
////                String singleDog = IJOBSProps.getInstance().getString(tag.getName());
//                String singleDog = "";
//                if (singleDog != null && ((new File(singleDog)).exists() || (new File(singleDog)).mkdirs())) {
//                    masterPath = singleDog;
//                    slavePath = singleDog;
//                    fixPath();
//                    LOG.info("{}| masterPath={}| slavePath [{}] ", tag.getName(), masterPath, slavePath);
//                    return;
//                }
//            } catch (NoSuchElementException ignored) {
//
//            }
//
//            masterPath = IJOBSProps.getInstance().getString(tag.getName() + MASTER_SUF, "/FILE_DATA_2/localupload/");
//            if (!(new File(masterPath)).exists() && !(new File(masterPath)).mkdirs()) {
//                LOG.error("masterPath [{}] is unusable! ", masterPath);
//                System.exit(1);
//            }
//            slavePath = IJOBSProps.getInstance().getString(tag.getName() + SLAVE_SUF, "/FILE_DATA_3/localupload/");
//            if (slavePath.trim().equals("")) { //如果没有Slave的直接取主配置
//                slavePath = masterPath;
//            } else {
//                if (!(new File(slavePath)).exists() && !(new File(slavePath)).mkdirs()) {
//                    LOG.error("slavePath [{}] is unusable! ", slavePath);
//                    slavePath = masterPath;
//                }
//            }
//            fixPath();
//            LOG.info("{}| masterPath={}| slavePath [{}] ", tag.getName(), masterPath, slavePath);
//        }
//
//        private void fixPath() {
//            if (!masterPath.endsWith(File.separator) && masterPath.contains(File.separator)) {
//                if (masterPath.endsWith("/")) {
//                    masterPath = masterPath.replace('/', File.separatorChar);
//                } else {
//                    masterPath += File.separator;
//                }
//            }
//
//            if (!slavePath.endsWith(File.separator) && slavePath.contains(File.separator)) {
//                if (slavePath.endsWith("/")) {
//                    slavePath = slavePath.replace('/', File.separatorChar);
//                } else {
//                    slavePath += File.separator;
//                }
//            }
//        }
//    }
//
//    static {
//        map.put(FileDirTypeConf.UPLOAD_FILE_DIR.name(),
//                new MS(FileDirTypeConf.UPLOAD_FILE_DIR));
//
//        map.put(FileDirTypeConf.JOB_INSTANCE_PATH.name(),
//                new MS(FileDirTypeConf.JOB_INSTANCE_PATH));
//    }

    public static String getFileDir(String rootPath, FileDirTypeConf typeConf) {
        if (typeConf == FileDirTypeConf.UPLOAD_FILE_DIR) {
            return rootPath + "/localupload/";
        } else if (typeConf == FileDirTypeConf.JOB_INSTANCE_PATH) {
            return rootPath + "/filedata/";
        } else {
            return rootPath + "/";
        }
//        MS ms = map.get(typeConf.name());
//        if (ms == null) {
//            throw new RuntimeException("filePathConf " + typeConf.getName() + " is not initialize!");
//        }
//        try {
//            if (!new File(ms.masterPath).canExecute()) {
//                switchMS(ms);
//            }
//        } catch (Exception e) {
//            LOG.error("nfs file error", e);
//            switchMS(ms);
//        }
//
//        return ms.masterPath;
    }

//    /**
//     * 主备交换
//     *
//     * @param ms
//     */
//    private synchronized static void switchMS(MS ms) {
//        String temp = ms.masterPath;
//        ms.masterPath = ms.slavePath;
//        ms.slavePath = temp;
//        LOG.info(ms.tag.getName() + " change to: " + ms.masterPath);
//    }
}
