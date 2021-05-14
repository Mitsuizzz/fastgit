package com.mitsui.fastgit.service.impl;




import java.io.IOException;
import java.util.List;

import com.mitsui.fastgit.dto.ProjectCollectionDto;
import com.mitsui.fastgit.service.ProjectCollectionService;
import com.mitsui.fastgit.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectCollectionServiceImpl implements ProjectCollectionService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectCollectionServiceImpl.class);
    private String configFilePath = "";

    public ProjectCollectionServiceImpl() {
        this.configFilePath = SystemUtil.getApplicationRunPath() + "/" + "ProjectCollectionConfig.json";
    }
    @Override
    public List<ProjectCollectionDto> getList() {
        if (!FileUtil.isExist(this.configFilePath)) {
            return null;
        } else {
            try {
                String fileContent = FileUtil.readFile(this.configFilePath);
                if (StringUtil.isNullOrEmpty(fileContent)) {
                    return null;
                } else {
                    List<ProjectCollectionDto> list = JsonUtil.parseArray(fileContent, ProjectCollectionDto.class);
                    return list;
                }
            } catch (IOException e) {
                logger.error("读取项目集合配置文件发生异常", e);
                return null;
            }
        }
    }
    @Override
    public void save(List<ProjectCollectionDto> list) {
        String fileContent = "";

        try {
            if (!CollectionUtil.isEmpty(list)) {
                fileContent = JsonUtil.listToJSONString(list);
            }

            FileUtil.saveFile(this.configFilePath, fileContent, false);
        } catch (IOException e) {
            logger.error("保存项目集合配置文件发生异常", e);
        }

    }
}
