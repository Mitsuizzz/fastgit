package com.mitsui.fastgit.service.impl;

import com.mitsui.fastgit.dto.GitConfigDto;
import com.mitsui.fastgit.exception.ConfigFileFormatException;
import com.mitsui.fastgit.exception.ConfigFileNotExistException;
import com.mitsui.fastgit.service.GitConfigService;
import com.mitsui.fastgit.util.FileUtil;
import com.mitsui.fastgit.util.SystemUtil;
import com.mitsui.fastgit.util.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GitConfigServiceImpl implements GitConfigService {

    private static final Logger logger = LoggerFactory.getLogger(GitConfigServiceImpl.class);

    public GitConfigServiceImpl() {
    }
    @Override
    public GitConfigDto getConfig() {
        try {
            String configFilePath = this.getConfigFilePath();
            GitConfigDto config = XmlUtil.toBeanByXmlFile(configFilePath, GitConfigDto.class);
            return config;
        } catch (Exception e) {
            logger.error("获取git config 发生异常", e);
            return null;
        }
    }
    @Override
    public void validateConfigFile() throws ConfigFileNotExistException, ConfigFileFormatException {
        String configFilePath = this.getConfigFilePath();
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new ConfigFileNotExistException();
        } else {
            GitConfigDto config = this.getConfig();
            if (config == null) {
                throw new ConfigFileFormatException("解析配置文件失败，请检查你的配置");
            }
        }
    }
    @Override
    public void copyConfigFile() throws IOException {
        String configFileName = "config.xml";
        InputStream inputStream = GitConfigServiceImpl.class.getClassLoader().getResourceAsStream(configFileName);
        String targetConfigFilePath = this.getConfigFilePath();
        File targetFile = new File(targetConfigFilePath);
        if (!targetFile.exists()) {
            FileUtil.copyFile(inputStream, targetFile);
        }
    }

    @Override
    public String getConfigFilePath() {
        String applicationRunPath = SystemUtil.getApplicationRunPath();
        String configFile = "config.xml";
        String path = applicationRunPath + "/" + configFile;
        logger.info("获取配置文件路径成功,配置文件路径={}", path);
        return path;
    }
}
