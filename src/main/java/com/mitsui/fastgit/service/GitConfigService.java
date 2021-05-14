package com.mitsui.fastgit.service;

import com.mitsui.fastgit.dto.GitConfigDto;
import com.mitsui.fastgit.exception.ConfigFileFormatException;
import com.mitsui.fastgit.exception.ConfigFileNotExistException;

import java.io.IOException;

public interface GitConfigService {
    /**
     * 获取配置
     * @return
     */
    GitConfigDto getConfig();

    /**
     * 校验配置文件
     * @throws ConfigFileNotExistException
     * @throws ConfigFileFormatException
     */
    void validateConfigFile() throws ConfigFileNotExistException, ConfigFileFormatException;

    /**
     * 复制配置
     * @throws IOException
     */
    void copyConfigFile() throws IOException;

    /**
     * 获取配置路径
     * @return
     */
    String getConfigFilePath();

}
