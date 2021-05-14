package com.mitsui.fastgit.service;

import com.mitsui.fastgit.dto.GitProjectDto;
import com.mitsui.fastgit.exception.CheckoutBranchException;
import org.eclipse.jgit.api.Git;

import java.util.Date;
import java.util.List;

public interface GitService {

    void createBranch(List<GitProjectDto> projectList, String sourceBranchName, String createBranchName, boolean isSkipExistProject, boolean isSkipChangeVersion) throws Exception;

    void createTag(List<GitProjectDto> projectList, String sourceBranchName, String createTagName) throws Exception;

    void merge(List<GitProjectDto> projectList, String sourceBranchName, String targetBranchName, List<String> ignoreFileList) throws Exception;

    List<String> getAllBranch(GitProjectDto project) throws Exception;

    void pull(GitProjectDto project) throws Exception;

    void push(List<GitProjectDto> projectList, String message) throws Exception;

    void deleteBranch(List<GitProjectDto> projectList, String branchName) throws Exception;

    void deleteTag(List<GitProjectDto> projectList, String tagName) throws Exception;

    void checkoutBranch(List<GitProjectDto> projectList, String branchName) throws CheckoutBranchException, InterruptedException;

    Git getGit(GitProjectDto project) throws Exception;

    void deleteOldBranch(List<GitProjectDto> projectList, Date date) throws Exception;

}
