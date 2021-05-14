package com.mitsui.fastgit.service;

import com.mitsui.fastgit.dto.GitProjectDto;

import java.util.List;

public interface GitProjectService {
    /**
     * 获取项目列表
     * @return
     */
    List<GitProjectDto> getList();

}
