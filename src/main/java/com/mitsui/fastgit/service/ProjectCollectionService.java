package com.mitsui.fastgit.service;

import com.mitsui.fastgit.dto.ProjectCollectionDto;

import java.util.List;

public interface ProjectCollectionService {

    List<ProjectCollectionDto> getList();

    void save(List<ProjectCollectionDto> projectCollectionDtoList);

}
