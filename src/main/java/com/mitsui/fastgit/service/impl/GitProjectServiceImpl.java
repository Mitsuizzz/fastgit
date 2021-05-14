package com.mitsui.fastgit.service.impl;

import com.mitsui.fastgit.dto.GitConfigDto;
import com.mitsui.fastgit.dto.GitProjectDto;
import com.mitsui.fastgit.service.GitConfigService;
import com.mitsui.fastgit.service.GitProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GitProjectServiceImpl implements GitProjectService {

    private static final Logger logger = LoggerFactory.getLogger(GitProjectServiceImpl.class);
    private static final GitConfigService gitConfigService = new GitConfigServiceImpl();

    public GitProjectServiceImpl() {
    }
    @Override
    public List<GitProjectDto> getList() {
        GitConfigDto config = gitConfigService.getConfig();
        if (config == null) {
            logger.error("未获取到git config 内容，请检查你的配置");
            return null;
        } else {
            return config.getProjectList();
        }
    }
}
