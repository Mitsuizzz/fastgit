package com.mitsui.fastgit.exception;

public class ConfigFileNotExistException extends Exception{

    public ConfigFileNotExistException() {
        super("配置文件不存在");
    }

}
