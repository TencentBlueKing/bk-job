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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceMetaData;
import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import com.tencent.bk.job.file_gateway.model.resp.common.FileTreeNodeDef;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class MetaDataService {

    private String loadResizedBase64ImageFromResource(String path) {
        InputStream ins = this.getClass().getClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream tmpBos = new ByteArrayOutputStream();
        String suffix = "png";
        int i = path.lastIndexOf(".");
        if (i >= 0 && i < path.length() - 1) {
            suffix = path.substring(i + 1);
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(ins);
            // 将图片压缩至64*64
            Thumbnails.of(bufferedImage)
                .size(64, 64)
                .outputFormat(suffix)
                .toOutputStream(tmpBos);
            ins = new ByteArrayInputStream(tmpBos.toByteArray());
            // 对字节数组Base64编码
            BASE64Encoder encoder = new BASE64Encoder();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            encoder.encode(ins, bos);
            return "data:image/" + suffix + ";base64," + bos.toString(Charsets.UTF_8.name()).replace("\n", "");
        } catch (IOException e) {
            log.warn("Fail to read and encode image from path:{}", path, e);
        }
        return null;
    }

    private FileWorkerConfig parseFileSourceIcon(FileWorkerConfig fileWorkerConfig) {
        List<FileSourceMetaData> fileSourceMetaDataList = fileWorkerConfig.getFileSourceMetaDataList();
        for (FileSourceMetaData fileSourceMetaData : fileSourceMetaDataList) {
            fileSourceMetaData.setIconBase64(loadResizedBase64ImageFromResource(fileSourceMetaData.getIconPath()));
        }
        return fileWorkerConfig;
    }

    public FileWorkerConfig getFileWorkerConfig() {
        InputStream ins = null;
        BufferedReader br = null;
        try {
            ins = this.getClass().getClassLoader().getResourceAsStream("FileWorkerConfig.json");
            if (ins == null) {
                log.error("Cannot find FileWorkerConfig.json from classpath");
                return null;
            }
            br = new BufferedReader(new InputStreamReader(ins, StandardCharsets.UTF_8));
            StringBuilder jsonStrBuilder = new StringBuilder();
            String line = null;
            do {
                line = br.readLine();
                jsonStrBuilder.append(line);
            } while (line != null);
            br.close();
            String jsonStr = jsonStrBuilder.toString();
            // TODO:jsonStr国际化替换
            log.debug("=================================");
            log.debug(jsonStr);
            FileWorkerConfig fileWorkerConfig = JsonUtils.fromJson(jsonStr, new TypeReference<FileWorkerConfig>() {
            });
            log.debug("=================================");
            log.debug(JsonUtils.toJson(fileWorkerConfig));
            log.debug("=================================");
            parseFileSourceIcon(fileWorkerConfig);
            return fileWorkerConfig;
        } catch (IOException e) {
            log.error("Fail to load fileWorkerConfig", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (ins != null) {
                    ins.close();
                }
            } catch (IOException e) {
                log.warn("Exception occurred when close stream", e);
            }
        }
        return null;
    }

    /**
     * 根据根节点类型查询子节点元信息
     *
     * @param fileSourceTypeCode
     * @param parentNodeType
     * @return
     */
    public FileTreeNodeDef getChildFileNodeMetaDataByParent(String fileSourceTypeCode, String parentNodeType) {
        if (StringUtils.isBlank(fileSourceTypeCode)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                new String[]{"fileSourceTypeCode"});
        }
        FileWorkerConfig fileWorkerConfig = getFileWorkerConfig();
        List<FileSourceMetaData> fileSourceMetaDataList = fileWorkerConfig.getFileSourceMetaDataList();
        for (FileSourceMetaData fileSourceMetaData : fileSourceMetaDataList) {
            if (fileSourceTypeCode.equals(fileSourceMetaData.getFileSourceTypeCode())) {
                List<FileTreeNodeDef> fileTreeNodeDefList = fileSourceMetaData.getFileTreeNodeDefList();
                String childNodeType = null;
                for (FileTreeNodeDef fileTreeNodeDef : fileTreeNodeDefList) {
                    String nodeType = fileTreeNodeDef.getNodeType();
                    // 对比子节点
                    if (nodeType != null && nodeType.equals(childNodeType)) {
                        return fileTreeNodeDef;
                    }
                    // 根据父节点找子节点
                    if (nodeType != null && nodeType.equals(parentNodeType)) {
                        childNodeType = fileTreeNodeDef.getChildNodeType();
                        if (childNodeType != null && childNodeType.equals(parentNodeType)) {
                            // 子节点与父节点类型相同
                            return fileTreeNodeDef;
                        }
                    }
                }
            }
        }
        log.warn("Cannot find FileTreeNodeDef of type {} in fileSource {}", parentNodeType, fileSourceTypeCode);
        return null;
    }
}
