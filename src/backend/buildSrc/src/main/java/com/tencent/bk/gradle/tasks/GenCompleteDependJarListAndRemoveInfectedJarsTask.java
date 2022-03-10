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

package com.tencent.bk.gradle.tasks;

import kotlin.text.Charsets;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class GenCompleteDependJarListAndRemoveInfectedJarsTask extends DefaultTask {

    @InputFiles
    public FileCollection bootJarOutputFiles;
    @Input
    public String completeJarListsRootPath = "../../support-files/dependJarLists";
    @Input
    public String dependJarInfoRootPath = "../../support-files/dependJarInfo";
    @Input
    public String defaultEdition = "ce";
    @Input
    public String defaultPackageType = "allInOne";
    Set<String> patternSet = new HashSet<>();
    private Map<String, DependJarInfo> dependJarInfoMap = null;
    String libJarPathPrefix = "BOOT-INF/lib/";

    @Inject
    public GenCompleteDependJarListAndRemoveInfectedJarsTask() {
    }

    public static List<String> listZipInnerFilePath(String zipFileName, String pathPrefix) throws IOException {
        List<String> filePathList = new ArrayList<>();
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(zipFileName));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    if (entryName.contains("\\")) {
                        pathPrefix = pathPrefix.replace("/", "\\");
                    } else if (entryName.contains("/")) {
                        pathPrefix = pathPrefix.replace("\\", "/");
                    }
                    if (entry.getName().startsWith(pathPrefix)) {
                        filePathList.add(entry.getName());
                    }
                }
            }
            filePathList.sort(String::compareToIgnoreCase);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zis != null)
                zis.close();
        }
        return filePathList;
    }

    private boolean match(String pattern, String targetStr) {
        if (!pattern.startsWith("^")) {
            pattern = "^" + pattern;
        }
        if (!pattern.endsWith("$")) {
            pattern += "$";
        }
        return Pattern.matches(pattern, targetStr);
    }

    @TaskAction
    public void process() {
        Project project = getProject();
        String edition = "";
        String packageType = "";
        if (project.hasProperty("job.edition")) {
            edition = (String) project.property("job.edition");
        } else {
            edition = defaultEdition;
        }
        if (project.hasProperty("job.package.type")) {
            packageType = (String) project.property("job.package.type");
        } else {
            packageType = defaultPackageType;
        }
        System.out.println("edition=" + edition);
        System.out.println("packageType=" + packageType);
        if ("ee".equals(edition) && "release".equals(packageType)) {
            genJarListAndRemoveInjectedJars();
        } else {
            System.out.println("Skip genJarListAndRemoveInjectedJars, set build param edition=ee,packageType=release "
                + "to enable");
        }
    }

    private List<String> readFileAsList(File file) {
        List<String> list = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.replace("\n", "");
                if (!"".equals(line)) {
                    list.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    private void writeListToFile(List<String> list, File file) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            for (int i = 0; i < list.size(); i++) {
                String line = list.get(i);
                if (i == 0) {
                    bw.write(line);
                } else {
                    bw.write("\n" + line);
                }
            }
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, DependJarInfo> constructInfoMap(List<String> nameList, List<String> versionList,
                                                        List<String> orgList, List<String> md5List) {
        int size = nameList.size();
        if (versionList.size() != size) {
            throw new RuntimeException("ResourceInfo not match:nameList.size=" + size
                + ",versionList.size=" + versionList.size());
        }
        if (orgList.size() != size) {
            throw new RuntimeException("ResourceInfo not match:nameList.size=" + size
                + ",orgList.size=" + orgList.size());
        }
        if (md5List.size() != size) {
            throw new RuntimeException("ResourceInfo not match:nameList.size=" + size
                + ",md5List.size=" + md5List.size());
        }
        Map<String, DependJarInfo> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String name = nameList.get(i);
            String version = versionList.get(i);
            String org = orgList.get(i);
            String jarFileName = name + "-" + version + ".jar";
            String downloadUrl =
                "https://repo1.maven.org/maven2/" + org.replace(".", "/")
                    + "/" + name + "/" + version + "/" + name + "-" + version + ".jar";
            String md5 = md5List.get(i);
            map.put(jarFileName, new DependJarInfo(jarFileName, name, version, org, downloadUrl, md5));
        }
        return map;
    }

    private String getAbsolutePath(String parentPath, String relativePath) {
        relativePath = relativePath.replace("/", File.separator);
        relativePath = relativePath.replace("\\", File.separator);
        while (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return CommonUtil.joinFilePath(parentPath, relativePath);
    }

    private String getdependJarInfoDirPath() {
        File rootDirFile = getProject().getRootDir();
        return getAbsolutePath(rootDirFile.getAbsolutePath(), dependJarInfoRootPath);
    }

    private Map<String, DependJarInfo> getDependJarInfoMap() {
        if (dependJarInfoMap == null) {
            String dependJarInfoDirPath = getdependJarInfoDirPath();
            File nameListFile = new File(dependJarInfoDirPath, "nameList.txt");
            File versionListFile = new File(dependJarInfoDirPath, "versionList.txt");
            File orgListFile = new File(dependJarInfoDirPath, "orgList.txt");
            File md5ListFile = new File(dependJarInfoDirPath, "md5List.txt");
            List<String> nameList = readFileAsList(nameListFile);
            List<String> versionList = readFileAsList(versionListFile);
            List<String> orgList = readFileAsList(orgListFile);
            List<String> md5List = readFileAsList(md5ListFile);
            dependJarInfoMap = constructInfoMap(nameList, versionList, orgList, md5List);
        }
        return dependJarInfoMap;
    }

    private List<String> readLibJarNameList(File bootJarFile) {
        List<String> libJarNameList = new ArrayList<>();
        try {
            List<String> libJarPathList = listZipInnerFilePath(bootJarFile.getAbsolutePath(), libJarPathPrefix);
            System.out.println("total " + libJarPathList.size() + "lib jars:");
            for (String libJarPath : libJarPathList) {
                System.out.println(libJarPath);
                libJarNameList.add(libJarPath.replace(libJarPathPrefix, ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return libJarNameList;
    }

    private void genJarList(String jarListDirPath, String projectNameWithoutBoot, List<String> libJarNameList) {
        File jarListDirFile = new File(jarListDirPath);
        if (!jarListDirFile.exists()) {
            boolean created = jarListDirFile.mkdirs();
            if (!created) {
                System.out.println("Cannot create jarListDirFile " + jarListDirFile.getAbsolutePath());
            }
        }
        File jarListFile = new File(jarListDirPath, projectNameWithoutBoot + ".txt");
        if (!jarListFile.exists()) {
            try {
                boolean created = jarListDirFile.createNewFile();
                if (!created) {
                    System.out.println("Cannot create jarListDirFile " + jarListDirFile.getAbsolutePath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 写入Jar列表至文件
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jarListFile), "UTF-8"));
            for (int i = 0; i < libJarNameList.size(); i++) {
                String libJarName = libJarNameList.get(i);
                String libJarPath = libJarPathPrefix + libJarName;
                if (i == 0) {
                    bw.write(libJarPath);
                } else {
                    bw.write("\n" + libJarPath);
                }
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Fail to close writer bw");
            }
        }
    }

    private List<String> genRemoveJarList(List<String> patternList, List<String> libJarNameList) {
        List<String> removeJarNameList = new ArrayList<>();
        for (String libJarName : libJarNameList) {
            System.out.println("begin to check:" + libJarName);
            for (String pattern : patternList) {
                if (match(pattern, libJarName)) {
                    System.out.println(libJarName + " matched pattern:" + pattern);
                    removeJarNameList.add(libJarName);
                }
            }
        }
        return removeJarNameList;
    }

    private void removeInfectedJars(List<String> removeJarNameList, File bootJarFile) {
        for (String removeJarName : removeJarNameList) {
            InputStream ins = null;
            try {
                String command = "zip -d " + bootJarFile.getName() + " " + libJarPathPrefix + removeJarName;
                System.out.println("exec command:\n" + command);
                Process process = Runtime.getRuntime().exec(command, null, bootJarFile.getParentFile());
                ins = process.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(ins, Charsets.UTF_8));
                String line = "";
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void genJarListAndRemoveInjectedJars() {
        String projectName = getProject().getName();
        System.out.println("project.name=" + projectName);
        String projectNameWithoutBoot = projectName;
        if (projectNameWithoutBoot.startsWith("boot-")) {
            projectNameWithoutBoot = projectNameWithoutBoot.replace("boot-", "");
        }
        File bootJarFile = null;
        for (File file : bootJarOutputFiles) {
            String absPath = file.getAbsolutePath();
            System.out.println("check file:" + absPath);
            if (file.getName().startsWith(projectNameWithoutBoot) && file.getName().endsWith(".jar")) {
                System.out.println("choose " + absPath);
                bootJarFile = file;
                break;
            }
        }
        if (bootJarFile == null) {
            System.err.println("Cannot find bootJar file, please check...");
            return;
        }
        // 读取jar中的lib下的第三方包
        List<String> libJarNameList = readLibJarNameList(bootJarFile);
        // 生成完整Jar包清单
        File rootDirFile = getProject().getRootDir();
        String jarListDirPath = getAbsolutePath(rootDirFile.getAbsolutePath(), completeJarListsRootPath);
        System.out.println("jarListDirPath=" + jarListDirPath);
        genJarList(jarListDirPath, projectNameWithoutBoot, libJarNameList);

        // 移除传染包
        List<String> patternList = new ArrayList<>(patternSet);
        patternList.sort(String::compareToIgnoreCase);
        List<String> removeJarNameList = genRemoveJarList(patternList, libJarNameList);
        System.out.println("=====================================");
        Map<String, DependJarInfo> dependJarInfoMap = getDependJarInfoMap();
        List<String> infectedJarsInfoList = new ArrayList<>();
        for (String removeJarName : removeJarNameList) {
            if (dependJarInfoMap.containsKey(removeJarName)) {
                DependJarInfo dependJarInfo = dependJarInfoMap.get(removeJarName);
                infectedJarsInfoList.add(dependJarInfo.getMd5() + " " + removeJarName
                    + ", " + dependJarInfo.getDownloadUrl());
            } else {
                throw new RuntimeException(String.format("Cannot find dependJarInfo of %s, please supplement " +
                    "resource" + " files in %s", removeJarName, getdependJarInfoDirPath()));
            }
        }
        File infectedJarInfoDirFile = new File(jarListDirPath, "infectedJarInfo");
        if (!infectedJarInfoDirFile.exists()) {
            boolean mkdirsSuccess = infectedJarInfoDirFile.mkdirs();
            if (!mkdirsSuccess) {
                System.out.println("Fail to mkdirs:" + infectedJarInfoDirFile.getAbsolutePath());
            }
        }
        writeListToFile(infectedJarsInfoList, new File(infectedJarInfoDirFile, projectNameWithoutBoot +
            "-infectedJars.txt"));
        System.out.println("=====================================");
        removeInfectedJars(removeJarNameList, bootJarFile);
        System.out.println("GenCompleteDependJarListAndRemoveInfectedJarsTask done");
    }

    public void remove(String pattern) {
        patternSet.add(pattern);
    }

    public FileCollection getBootJarOutputFiles() {
        return bootJarOutputFiles;
    }

    public void setBootJarOutputFiles(FileCollection bootJarOutputFiles) {
        this.bootJarOutputFiles = bootJarOutputFiles;
    }

    public String getCompleteJarListsRootPath() {
        return completeJarListsRootPath;
    }

    public void setCompleteJarListsRootPath(String completeJarListsRootPath) {
        this.completeJarListsRootPath = completeJarListsRootPath;
    }

    public String getDependJarInfoRootPath() {
        return dependJarInfoRootPath;
    }

    public void setDependJarInfoRootPath(String dependJarInfoRootPath) {
        this.dependJarInfoRootPath = dependJarInfoRootPath;
    }

    public String getDefaultEdition() {
        return defaultEdition;
    }

    public void setDefaultEdition(String defaultEdition) {
        this.defaultEdition = defaultEdition;
    }

    public String getDefaultPackageType() {
        return defaultPackageType;
    }

    public void setDefaultPackageType(String defaultPackageType) {
        this.defaultPackageType = defaultPackageType;
    }
}
