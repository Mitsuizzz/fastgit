package com.mitsui.fastgit.exception;

public class ConfigFileFormatException extends Exception{

    public ConfigFileFormatException(String msg) {
        super(msg);
    }

    public ConfigFileFormatException() {
        super("配置文件格式异常");
    }

}
