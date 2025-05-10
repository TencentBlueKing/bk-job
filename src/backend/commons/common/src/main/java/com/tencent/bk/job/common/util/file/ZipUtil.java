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

package com.tencent.bk.job.common.util.file;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 解压缩工具
 */
@Slf4j
public final class ZipUtil {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private ZipUtil() {
    }

    /**
     * 压缩文件
     *
     * @param filePath 待压缩的文件路径
     * @return 压缩后的文件
     */
    public static File zip(String filePath) {
        File source = new File(filePath);
        return zip(source);
    }

    private static File zip(File source) {
        File target = null;
        if (source.exists()) {
            // 压缩文件名=源文件名.zip
            String zipName = source.getName() + ".zip";
            target = new File(source.getParent(), zipName);
            if (target.exists()) {
                if (!target.delete()) {
                    target.deleteOnExit();
                } // 删除旧的文件
            }

            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)),
                StandardCharsets.UTF_8)) {
                // 添加对应的文件Entry
                addEntry("", source, zos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return target;
    }

    public static File zip(String fileName, File... sources) {
        File target = null;
        if (sources != null && sources.length > 0) {
            target = new File(fileName);
            if (target.exists()) {
                if (!target.delete()) {
                    target.deleteOnExit();
                } // 删除旧的文件
            }
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)),
                StandardCharsets.UTF_8)) {
                for (File source : sources) {
                    if (source.exists()) {
                        // 添加对应的文件Entry
                        addEntry("", source, zos);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return target;
    }

    /**
     * 扫描添加文件Entry
     *
     * @param base   基路径
     * @param source 源文件
     * @param zos    Zip文件输出流
     * @throws IOException IOException
     */
    private static void addEntry(String base, File source, ZipOutputStream zos) throws IOException {
        // 按目录分级，形如：/aaa/bbb.txt
        String entry = base + source.getName();
        if (source.isDirectory()) {
            File[] files = source.listFiles();
            if (files != null) for (File file : files) {
                // 递归列出目录下的所有文件，添加文件Entry
                addEntry(entry + "/", file, zos);
            }
        } else {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(source), buffer.length)) {
                int read;
                zos.putNextEntry(new ZipEntry(entry));
                while ((read = bis.read(buffer, 0, buffer.length)) != -1) {
                    zos.write(buffer, 0, read);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 解压文件
     *
     * @param filePath 压缩文件路径
     */
    public static List<File> unzip(String filePath) {
        return unzip(new File(filePath));
    }

   public static List<File> unzip(File source) {
    List<File> unzipFiles = new ArrayList<>();
    if (source.exists()) {
        // Create a target directory in the same parent directory as the source
        File targetDir = new File(source.getParent());
        
        ZipInputStream zis = null;
        try {
            // Using try-with-resources would be better, but keeping close() pattern for compatibility
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(source)), StandardCharsets.UTF_8);
            
            // Delegate to secure unzip implementation
            unzipInternal(targetDir, zis, unzipFiles);
        } catch (IOException e) {
            log.error("Failed to unzip file: " + source.getAbsolutePath(), e);
            throw new RuntimeException(e);
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    log.error("Failed to close zip stream", e);
                }
            }
        }
    }
    return unzipFiles;
}

private static void unzipInternal(File targetDir, ZipInputStream zis, List<File> unzippedFiles) throws IOException {
    ZipEntry entry;
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    
    while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
            continue;
        }
        
        // Security check - Prevent path traversal attacks
        String entryName = entry.getName();
        File targetFile = new File(targetDir, entryName);
        
        // Validate file path is within target directory
        String canonicalDestinationPath = targetFile.getCanonicalPath();
        String canonicalTargetDirPath = targetDir.getCanonicalPath();
        
        if (!canonicalDestinationPath.startsWith(canonicalTargetDirPath + File.separator)) {
            throw new SecurityException("Zip entry is outside of target directory: " + entryName);
        }
        
        // Create parent directories if needed
        File parentDir = targetFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir);
        }
        
        // Extract file
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            int read;
            while ((read = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            bos.flush();
        }
        
        unzippedFiles.add(targetFile);
        zis.closeEntry();
    }
}

// Existing close method
private static void close(Closeable... closeables) {
    if (closeables != null) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    log.error("Failed to close resource", e);
                }
            }
        }
    }
}


    /**
     * 关闭一个或多个流对象
     *
     * @param closeables 可关闭的流对象列表
     */
    public static void close(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException ignored) {
                        // do nothing
                    }
                }
            }
        }
    }
}
