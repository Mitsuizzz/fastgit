package com.mitsui.fastgit.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private static final String NEW_LINE = "\r\n";
    private static final String DEFAULT_ENCODE = "utf-8";
    private static final String FILE_SUFFIX_SPLIT = ".";

    public FileUtil() {
    }

    public static String getSuffixFromFileName(String fileName) {
        if (!StringUtil.isNullOrEmpty(fileName) && fileName.contains(".")) {
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
            return suffix;
        } else {
            return null;
        }
    }

    public static String readFile(String path, String encode) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = getBufferedReader(path, encode);

        String tempStr;
        while((tempStr = br.readLine()) != null) {
            sb.append(tempStr + "\r\n");
        }

        return sb.toString();
    }

    public static String readFile(String path) throws IOException {
        return readFile(path, "utf-8");
    }

    public static List<String> readFileByList(String path, String encode) throws IOException {
        List<String> lines = new ArrayList();
        BufferedReader br = getBufferedReader(path, encode);

        String tempStr;
        while((tempStr = br.readLine()) != null) {
            lines.add(tempStr);
        }

        return lines;
    }

    public static List<String> readFileByList(String path) throws IOException {
        return readFileByList(path, "utf-8");
    }

    private static BufferedReader getBufferedReader(String path, String encode) throws FileNotFoundException, UnsupportedEncodingException {
        if (!StringUtil.isNullOrEmpty(path) && !StringUtil.isNullOrEmpty(encode)) {
            File file = new File(path);
            if (!file.exists()) {
                throw new FileNotFoundException();
            } else {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis, "utf-8"));
                return br;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void copyFile(InputStream inputStream, File targetFile) throws IOException {
        if (inputStream != null && targetFile != null) {
            BufferedInputStream inBuff = new BufferedInputStream(inputStream);
            FileOutputStream output = new FileOutputStream(targetFile);
            BufferedOutputStream outBuff = new BufferedOutputStream(output);
            byte[] b = new byte[5120];

            int len;
            while((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }

            outBuff.flush();
            inBuff.close();
            outBuff.close();
            output.close();
            inputStream.close();
        }
    }

    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath);
            File myFilePath = new File(folderPath);
            myFilePath.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean isExist(String filePath) {
        File file = new File(filePath);
        return file.exists() || file.isDirectory();
    }

    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        } else if (!file.isDirectory()) {
            return flag;
        } else {
            String[] tempList = file.list();
            File temp = null;

            for(int i = 0; i < tempList.length; ++i) {
                if (path.endsWith(File.separator)) {
                    temp = new File(path + tempList[i]);
                } else {
                    temp = new File(path + File.separator + tempList[i]);
                }

                if (temp.isFile()) {
                    temp.delete();
                }

                if (temp.isDirectory()) {
                    delAllFile(path + "/" + tempList[i]);
                    delFolder(path + "/" + tempList[i]);
                    flag = true;
                }
            }

            return flag;
        }
    }

    public static void saveFile(String path, String content, boolean append) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(file, append);
        out.write(content.getBytes("utf-8"));
        out.close();
    }
}
