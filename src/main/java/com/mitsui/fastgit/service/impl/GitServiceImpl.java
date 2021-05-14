package com.mitsui.fastgit.service.impl;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.mitsui.fastgit.dto.AccountConfig;
import com.mitsui.fastgit.dto.GitConfigDto;
import com.mitsui.fastgit.dto.GitProjectDto;
import com.mitsui.fastgit.dto.SshConfigDto;
import com.mitsui.fastgit.exception.*;
import com.mitsui.fastgit.service.GitConfigService;
import com.mitsui.fastgit.service.GitService;
import com.mitsui.fastgit.util.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitServiceImpl implements GitService {
    private static final Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);
    private static final GitConfigService configService = new GitConfigServiceImpl();
    private static GitConfigDto config;
    private static final String MASTER_BRANCH = "master";
    private static SshSessionFactory sshSessionFactory = null;
    private static final String LIST_SPLIT_CHAR = "\n";
    private static final int THREAD_POOL_DEFAULT_SIZE = 1;
    private static ExecutorService threadPoolExecutor;
    private static CredentialsProvider credentialsProvider;

    private static void initCredentialsProvider() {
        AccountConfig accountConfig = config.getAccountConfig();
        if (accountConfig != null) {
            if (!StringUtil.isNullOrEmpty(accountConfig.getUserName()) && !StringUtil.isNullOrEmpty(accountConfig.getPassword())) {
                credentialsProvider = GitUtil.getCredentialsProvider(accountConfig.getUserName(), accountConfig.getPassword());
            }
        }
    }

    private static void initThreadPool() {
        int threadCount = 1;
        if (!StringUtil.isNullOrEmpty(config.getDealBranchThreadCount())) {
            threadCount = Integer.parseInt(config.getDealBranchThreadCount());
        }

        logger.info("开始初始化处理分支线程池，线程数量{}", threadCount);
        threadPoolExecutor = Executors.newFixedThreadPool(threadCount);
    }

    private static void initSshSessionFactory(GitConfigDto config) {
        if (config != null && config.getSshConfig() != null) {
            SshConfigDto sshConfig = config.getSshConfig();
            sshSessionFactory = GitUtil.getCustomSshSessionFactory(sshConfig.getPrivateKeyPath());
        }
    }
    @Override
    public void createBranch(List<GitProjectDto> projectList, String sourceBranchName, String createBranchName, boolean isSkipExistProject, boolean isSkipChangeVersion) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(sourceBranchName)) {
            throw new ParamException("sourceBranchName is not be null");
        } else if (StringUtil.isNullOrEmpty(createBranchName)) {
            throw new ParamException("createBranchName is not be null");
        } else {
            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            List<GitProjectDto> createProjectList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latchCheck = new CountDownLatch(projectList.size());

            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = checkBranchForCreateBranch(project, sourceBranchName, createBranchName, createProjectList, isSkipExistProject);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latchCheck.countDown();
                    }
                });
            });
            latchCheck.await();
            String parentPom;
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                parentPom = "创建分支-项目校验完成，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.error(parentPom);
                throw new CreateBranchException(parentPom);
            } else if (CollectionUtil.isEmpty(createProjectList)) {
                logger.warn("没有待创建分支的项目");
            } else {
                parentPom = config.getParentPomName();
                GitProjectDto parentPomProject = projectList.stream().filter((e) -> {
                    return e.getName().equals(parentPom);
                }).findFirst().orElse(null);
                boolean isSelectParentPom;
                if (parentPomProject != null) {
                    isSelectParentPom = true;
                } else {
                    isSelectParentPom = false;
                }

                CountDownLatch latchCreate = new CountDownLatch(createProjectList.size());

                createProjectList.forEach(project -> {
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            String errorMsg = GitServiceImpl.this.createBranch(project, sourceBranchName, createBranchName, isSkipExistProject, isSelectParentPom, isSkipChangeVersion);
                            if (!StringUtil.isNullOrEmpty(errorMsg)) {
                                errorMsgList.add(errorMsg);
                            }

                            latchCreate.countDown();
                        }
                    });
                });

                latchCreate.await();
                this.updateProjectVersionForParentPom(createBranchName, parentPomProject, createProjectList, isSkipChangeVersion);
                if (!CollectionUtil.isEmpty(errorMsgList)) {
                    String errorMsg = "项目分支创建完成，部分分支创建失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                    logger.error(errorMsg);
                    throw new CreateBranchException(errorMsg);
                }
            }
        }
    }

    private String createBranch(GitProjectDto project, String sourceBranchName, String createBranchName, boolean isSkipExistProject, boolean isSelectParentPom, boolean isSkipChangeVersion) {
        String errorMsg = "";

        try {
            this.createBranch(project, sourceBranchName, createBranchName);
            String parentPom = config.getParentPomName();
            if (!parentPom.equals(project.getName())) {
                this.updateProjectVersionForNormal(createBranchName, project, isSelectParentPom, isSkipChangeVersion);
            }

            return errorMsg;
        } catch (RefAlreadyExistsException e) {
            if (isSkipExistProject) {
                logger.info("分支已存在，自动跳过,project={},branchName={}", project.getName(), createBranchName);
                return errorMsg;
            } else {
                errorMsg = String.format("项目%s %s分支已存在", project.getName(), createBranchName);
                logger.warn(errorMsg);
                return errorMsg;
            }
        } catch (Exception e) {
            errorMsg = String.format("项目%s 创建发生异常,异常信息:", project.getName(), e.getMessage());
            logger.error(errorMsg, e);
            return errorMsg;
        }
    }

    private String checkBranchForCreateBranch(GitProjectDto project, String sourceBranchName, String createBranchName, List<GitProjectDto> createProjectList, boolean isSkipExistProject) {
        logger.info("开始校验项目{} 合法性(是否存在同名分支或tag及本地分支是否存在未提交代码)", project.getName());
        String errorMsg = "";

        try {
            this.pull(project);
            String parentPom = config.getParentPomName();
            boolean sourceBranchIsTag = true;
            if (this.checkBranchIsExist(project, sourceBranchName, sshSessionFactory)) {
                sourceBranchIsTag = false;
            }

            if (!sourceBranchIsTag) {
                try {
                    this.checkoutHaveException(project, sourceBranchName);
                } catch (RefNotFoundException e) {
                    errorMsg = String.format("项目%s 源分支%s不存在，请检查您的分支", project.getName(), sourceBranchName);
                    logger.error(errorMsg, e);
                    return errorMsg;
                } catch (ProjectNotCleanExcption e) {
                    errorMsg = String.format("项目%s存在未提交的更改,请提交完成后再进行操作", project.getName());
                    logger.error(errorMsg, e);
                    return errorMsg;
                } catch (Exception e) {
                    errorMsg = String.format("切换项目%s发生异常,异常信息%s", project.getName(), e.getMessage());
                    logger.error(errorMsg, e);
                    return errorMsg;
                }
            } else if (!this.checkTagIsExist(project, sourceBranchName, sshSessionFactory)) {
                this.pull(project);
                if (!this.checkTagIsExist(project, sourceBranchName, sshSessionFactory)) {
                    errorMsg = String.format("项目%s 源分支%s不存在，请检查您的分支", project.getName(), sourceBranchName);
                    logger.error(errorMsg);
                    return errorMsg;
                }
            }

            boolean isExist = this.checkBranchIsExist(project, createBranchName, sshSessionFactory);
            if (isExist) {
                if (!isSkipExistProject) {
                    errorMsg = String.format("项目%s %s 分支已存在", project.getName(), createBranchName);
                    logger.warn(errorMsg);
                    return errorMsg;
                }

                if (!parentPom.equals(project.getName())) {
                    return errorMsg;
                }
            }

            isExist = this.checkTagIsExist(project, createBranchName, sshSessionFactory);
            if (isExist) {
                errorMsg = String.format("项目%s 已存在同名tag %s", project.getName(), createBranchName);
                logger.warn(errorMsg);
                return errorMsg;
            }

            createProjectList.add(project);
        } catch (Exception e) {
            errorMsg = String.format("校验项目%s 合法性(是否存在同名分支或tag及本地分支是否存在未提交代码)发生未知异常,异常信息:%s", project.getName(), e.toString());
            logger.error(errorMsg, e);
            return errorMsg;
        }

        logger.info("校验项目{} 合法性(是否存在同名分支或tag及本地分支是否存在未提交代码)完成", project.getName());
        return errorMsg;
    }

    private void updateProjectVersionForParentPom(String branchName, GitProjectDto parentPomProject, List<GitProjectDto> projectList, boolean isSkipChangeVersion) throws Exception {
        if (isSkipChangeVersion) {
            if (parentPomProject != null) {
                logger.info("开始更新父pom,branchName={}", branchName);
                this.checkoutHaveException(parentPomProject, branchName);
                String parentPom = config.getParentPomName();
                List<GitProjectDto> normalProjectList = new ArrayList();
                normalProjectList.addAll(projectList);
                normalProjectList = normalProjectList.stream().filter((e) -> {
                    return !e.getName().equals(parentPom);
                }).collect(Collectors.toList());
                String version = this.getVersion(branchName);
                String pomPath = this.getPomPath(parentPomProject);
                PomWriter pomWriter = new PomWriter(pomPath);
                pomWriter.setVersion(version);
                if (!CollectionUtil.isEmpty(normalProjectList)) {
                    Map<String, String> propertiesMap = new HashMap();
                    normalProjectList.forEach((e) -> {
                        String key = e.getParentPomVersionKey();
                        if (!StringUtil.isNullOrEmpty(key)) {
                            propertiesMap.put(key, version);
                        }
                    });
                    pomWriter.setProperties(propertiesMap);
                }

                pomWriter.write();
                String errorMsg = this.push(parentPomProject, this.getCreateBranchMsg(branchName));
                if (!StringUtil.isNullOrEmpty(errorMsg)) {
                    throw new CreateBranchException("更新父pom失败,失败信息" + errorMsg);
                } else {
                    logger.info("父pom更新完成,branchName={}", branchName);
                }
            }
        }
    }

    private void updateProjectVersionForNormal(String branchName, GitProjectDto project, boolean isSelectParentPom, boolean isSkipChangeVersion) throws Exception {
        logger.info("开始更新项目版本号,project={},branchName={}", project.getName(), branchName);
        this.checkoutNotPull(project, branchName);
        String version = this.getVersion(branchName);
        String pomPath = this.getPomPath(project);
        if (!FileUtil.isExist(pomPath) || isSkipChangeVersion) {
            logger.info("未找到pom文件或无需更新pom版本号,project={},branchName={}", project.getName(), branchName);
        } else {
            PomWriter pomWriter = new PomWriter(pomPath);
            pomWriter.setVersion(version);
            if (isSelectParentPom) {
                pomWriter.setParentVersion(version);
            }

            pomWriter.write();
            String errorMsg = this.push(project, this.getCreateBranchMsg(branchName));
            if (!StringUtil.isNullOrEmpty(errorMsg)) {
                throw new CreateBranchException("更新pom失败,失败信息" + errorMsg);
            } else {
                logger.info("更新项目版本号完成,project={},branchName={}", project.getName(), branchName);
            }
        }
    }

    private String getCreateBranchMsg(String branchName) {
        String msg = String.format("%s %s", "create branch", branchName);
        return msg;
    }

    private String getVersion(String branchName) {
        return branchName + "-SNAPSHOT";
    }

    private String getPomPath(GitProjectDto project) {
        String pomPath = project.getLocalPath() + "/" + "pom.xml";
        return pomPath;
    }
    @Override
    public void deleteBranch(List<GitProjectDto> projectList, final String branchName) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(branchName)) {
            throw new ParamException("branchName is not be null");
        } else if ("master".equals(branchName)) {
            throw new Exception("不能删除master分支");
        } else {
            final List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            final CountDownLatch latchCheck = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = deleteBranch(project, branchName);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latchCheck.countDown();
                    }
                });
            });

            latchCheck.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "分支删除完成，部分项目删除失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.warn(errorMsg);
                throw new DeleteBranchException(errorMsg);
            }
        }
    }
    @Override
    public void deleteTag(List<GitProjectDto> projectList, String tagName) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(tagName)) {
            throw new ParamException("tagName is not be null");
        } else {
            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latchCheck = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = GitServiceImpl.this.deleteTag(project, tagName);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latchCheck.countDown();
                    }
                });
            });

            latchCheck.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "tag删除完成，部分项目删除失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.warn(errorMsg);
                throw new DeleteTagException(errorMsg);
            }
        }
    }
    @Override
    public void checkoutBranch(List<GitProjectDto> projectList, String branchName) throws CheckoutBranchException, InterruptedException {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(branchName)) {
            throw new ParamException("branchName is not be null");
        } else {
            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latch = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = checkout(project, branchName);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latch.countDown();
                    }
                });
            });
            latch.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "切换分支完成，部分项目切换失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.warn(errorMsg);
                throw new CheckoutBranchException(errorMsg);
            }
        }
    }

    private String deleteBranch(GitProjectDto project, String branchName) {
        logger.info("开始删除分支 projectName={},branchName={}", project.getName(), branchName);
        String errorMsg = "";

        try {
            Git git = project.getGit();
            if (git == null) {
                this.getGit(project);
            }

            this.pull(project);
            this.checkoutNotPull(project, "master");
            GitUtil.deleteBranch(git, branchName, sshSessionFactory, credentialsProvider);
        } catch (Exception e) {
            errorMsg = String.format("项目%s删除分支发生异常,异常信息:%s", project.getName(), e.getMessage());
            logger.error(errorMsg, e);
        }

        logger.info("分支删除完成 projectName={},branchName={}，结果={}", new Object[]{project.getName(), branchName, StringUtil.isNullOrEmpty(errorMsg) ? "成功" : errorMsg});
        return errorMsg;
    }

    private String deleteTag(GitProjectDto project, String tagName) {
        logger.info("开始删除tag projectName={},tagName={}", project.getName(), tagName);
        String errorMsg = "";

        try {
            this.pull(project);
            Git git = project.getGit();
            if (git == null) {
                this.getGit(project);
            }

            GitUtil.deleteTag(git, tagName, sshSessionFactory, credentialsProvider);
        } catch (Exception e) {
            errorMsg = String.format("项目%s删除tag发生异常,异常信息:%s", project.getName(), e.getMessage());
            logger.error(errorMsg, e);
        }

        logger.info("tag删除完成 projectName={},tagName={}", project.getName(), tagName);
        return errorMsg;
    }

    private void createBranch(GitProjectDto project, String sourceBranchName, String createBranchName) throws Exception {
        if (project == null) {
            throw new ParamException("project is not be null");
        } else if (StringUtil.isNullOrEmpty(sourceBranchName)) {
            throw new ParamException("sourceBranchName is not be null");
        } else if (StringUtil.isNullOrEmpty(createBranchName)) {
            throw new ParamException("createBranchName is not be null");
        } else {
            logger.info("开始创建分支，projectName={},branchName={}", project.getName(), createBranchName);

            try {
                GitUtil.createBranch(project, sourceBranchName, createBranchName, sshSessionFactory, credentialsProvider);
            } catch (RefAlreadyExistsException e) {
                throw e;
            } catch (JGitInternalException e) {
                if (e.getMessage().contains("NO_CHANGE")) {
                    throw new RefAlreadyExistsException("该分支已存在");
                }

                logger.error("项目{}创建分支失败", project.getName(), e);
                throw new CreateBranchException(String.format("项目%s创建分支发生异常,异常信息:%s", project.getName(), e.getMessage()));
            } catch (Exception e) {
                logger.error("项目{}创建分支失败", project.getName(), e);
                throw new CreateBranchException(String.format("项目%s创建分支发生异常,异常信息:%s", project.getName(), e.getMessage()));
            }

            logger.info("分支创建完成，projectName={},branchName={}", project.getName(), createBranchName);
        }
    }
    @Override
    public void createTag(List<GitProjectDto> projectList, String sourceBranchName, String createTagName) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(sourceBranchName)) {
            throw new ParamException("sourceBranchName is not be null");
        } else if (StringUtil.isNullOrEmpty(createTagName)) {
            throw new ParamException("createTagName is not be null");
        } else {
            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latchCheck = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = GitServiceImpl.this.checkBranchForCreateTag(project, sourceBranchName, createTagName);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latchCheck.countDown();
                    }
                });
            });
            latchCheck.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "创建tag-项目校验完成，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.error(errorMsg);
                throw new CreateTagException(errorMsg);
            } else {
                CountDownLatch latchCreate = new CountDownLatch(projectList.size());
                projectList.forEach(project -> {
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            String errorMsg = GitServiceImpl.this.createTag(project, createTagName);
                            if (!StringUtil.isNullOrEmpty(errorMsg)) {
                                errorMsgList.add(errorMsg);
                            }

                            latchCreate.countDown();
                        }
                    });
                });

                latchCreate.await();
                if (!CollectionUtil.isEmpty(errorMsgList)) {
                    String errorMsg = "项目tag创建完成，部分项目创建失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                    logger.error(errorMsg);
                    throw new CreateTagException(errorMsg);
                }
            }
        }
    }

    private String checkBranchForCreateTag(GitProjectDto project, String sourceBranchName, String createTagName) {
        logger.info("开始校验项目{} 合法性(是否存在同名分支或tag及本地分支是否存在未提交代码)", project.getName());
        String errorMsg = "";

        try {
            this.pull(project);

            try {
                this.checkoutHaveException(project, sourceBranchName);
            } catch (RefNotFoundException e) {
                errorMsg = String.format("项目%s 源分支%s不存在，请检查您的分支", project.getName(), sourceBranchName);
                logger.error(errorMsg, e);
                return errorMsg;
            } catch (ProjectNotCleanExcption e) {
                errorMsg = String.format("项目%s存在未提交的更改,请提交完成后再进行操作", project.getName());
                logger.error(errorMsg, e);
                return errorMsg;
            } catch (Exception e) {
                errorMsg = String.format("切换项目%s发生异常,异常信息%s", project.getName(), e.getMessage());
                logger.error(errorMsg, e);
                return errorMsg;
            }

            boolean isExist = this.checkTagIsExist(project, createTagName, sshSessionFactory);
            if (isExist) {
                errorMsg = String.format("项目%s %s tag已存在", project.getName(), createTagName);
                return errorMsg;
            }

            isExist = this.checkBranchIsExist(project, createTagName, sshSessionFactory);
            if (isExist) {
                errorMsg = String.format("项目%s 已存在同名分支%s", project.getName(), createTagName);
                return errorMsg;
            }
        } catch (Exception e) {
            errorMsg = String.format("校验项目%s 合法性(是否存在同名分支或tag及本地分支是否存在未提交代码)发生未知异常,异常信息:%s", project.getName(), e.toString());
            logger.error(errorMsg, e);
            return errorMsg;
        }

        logger.info("校验项目{} 合法性(是否存在同名分支或tag及本地分支是否存在未提交代码)完成", project.getName());
        return errorMsg;
    }

    private boolean checkTagIsExist(GitProjectDto project, String tagName, SshSessionFactory sshSessionFactory) throws Exception {
        try {
            boolean res = GitUtil.checkTagIsExist(project.getGit(), tagName, sshSessionFactory, credentialsProvider);
            return res;
        } catch (Exception e) {
            logger.error("校验{}项目tag名称是否存在发生异常", project.getName(), e);
            throw new CheckTagExistException(String.format("校验%s项目tag名称是否存在发生异常,异常信息:%s", project.getName(), e.getMessage()));
        }
    }

    private boolean checkBranchIsExist(GitProjectDto project, String branchName, SshSessionFactory sshSessionFactory) throws Exception {
        try {
            return GitUtil.checkBranchIsExist(project.getGit(), branchName, sshSessionFactory, credentialsProvider);
        } catch (Exception e) {
            logger.error("校验{}项目分支名称是否存在发生异常", project.getName(), e);
            throw new CheckBranchExistException(String.format("校验%s项目分支名称是否存在发生异常,异常信息:%s", project.getName(), e.getMessage()));
        }
    }

    private String createTag(GitProjectDto project, String createTagName) {
        logger.info("开始创建tag，projectName={},tagName={}", project.getName(), createTagName);
        Git git = project.getGit();
        String errorMsg = "";

        try {
            GitUtil.createTag(git, createTagName, sshSessionFactory, credentialsProvider);
        } catch (RefAlreadyExistsException e) {
            errorMsg = String.format("项目%s %s tag已存在", project.getName(), createTagName);
            logger.error(errorMsg, e);
            return errorMsg;
        } catch (Exception e) {
            errorMsg = String.format("项目%s创建tag失败", project.getName());
            logger.error(errorMsg, e);
            return errorMsg;
        }

        logger.info("tag创建完成，projectName={},tagName={}", project.getName(), createTagName);
        return errorMsg;
    }
    @Override
    public void merge(List<GitProjectDto> projectList, String sourceBranchName, String targetBranchName, List<String> ignoreFileList) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(sourceBranchName)) {
            throw new ParamException("sourceBranchName is not be null");
        } else if (StringUtil.isNullOrEmpty(targetBranchName)) {
            throw new ParamException("targetBranchName is not be null");
        } else {
            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latch = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = GitServiceImpl.this.merge(project, sourceBranchName, targetBranchName, ignoreFileList);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latch.countDown();
                    }
                });
            });

            latch.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "项目merge完成，部分项目merge失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.warn(errorMsg);
                throw new MergeBranchException(errorMsg);
            }
        }
    }

    private String merge(GitProjectDto project, String sourceBranchName, String targetBranchName, List<String> ignoreFileList) {
        logger.info("开始 merge 分支,project={},sourceBranchName={},targetBranchName={}", new Object[]{project.getName(), sourceBranchName, targetBranchName});
        String projectName = project.getName();
        String errorMsg = "";

        try {
            this.pull(project);
            List<String> conflictNameList = new ArrayList<>();
            boolean res = GitUtil.merge(project, sourceBranchName, targetBranchName, sshSessionFactory, ignoreFileList, credentialsProvider, conflictNameList);
            logger.info("分支merge 完成,project={},sourceBranchName={},targetBranchName={}", new Object[]{project.getName(), sourceBranchName, targetBranchName});
            if (!res) {
                errorMsg = String.format("项目%s代码出现冲突,请手动解决, %s", projectName, JSON.toJSONString(conflictNameList));
            }

            return errorMsg;
        } catch (Exception e) {
            errorMsg = projectName + "项目merge失败,错误信息:" + e.getMessage();
            logger.error(errorMsg, e);
            return errorMsg;
        }
    }
    @Override
    public List<String> getAllBranch(GitProjectDto project) throws Exception {
        if (project == null) {
            throw new ParamException("project is not be null");
        } else {
            Git git = this.getGit(project);
            List<String> branchNameList = this.getAllBranch(git);
            return branchNameList;
        }
    }

    private List<String> getAllBranch(Git git) throws Exception {
        logger.info("开始获取所有分支");
        List<String> branchNameList = GitUtil.getAllBranchName(git, credentialsProvider);
        logger.info("所有分支获取完成");
        return branchNameList;
    }
    @Override
    public void pull(GitProjectDto project) throws Exception {
        if (project == null) {
            throw new ParamException("project is not be null");
        } else {
            GitUtil.pull(project, sshSessionFactory, credentialsProvider);
        }
    }
    @Override
    public void push(List<GitProjectDto> projectList, String message) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else if (StringUtil.isNullOrEmpty(message)) {
            throw new ParamException("message is not be null");
        } else {
            projectList.forEach(project -> {
                this.push(project, message);
            });

            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latch = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = GitServiceImpl.this.push(project, message);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latch.countDown();
                    }
                });
            });
            latch.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "分支push完成，部分项目push失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.warn(errorMsg);
                throw new PushBranchException(errorMsg);
            }
        }
    }

    private String push(GitProjectDto project, String message) {
        logger.info("开始push代码，项目" + project.getName());
        String errorMsg = "";

        try {
            if (project == null) {
                errorMsg = "project is not be null";
                return errorMsg;
            }

            if (StringUtil.isNullOrEmpty(message)) {
                errorMsg = "message is not be null";
                return errorMsg;
            }

            Git git = project.getGit();
            if (git == null) {
                git = this.getGit(project);
            }

            GitUtil.pushAll(git, message, sshSessionFactory, credentialsProvider);
        } catch (Exception e) {
            errorMsg = String.format("项目%s push代码发生异常, 异常信息:%s", project.getName(), e.getMessage());
            logger.error(errorMsg, e);
            return errorMsg;
        }

        logger.info("push代码完成，项目名称" + project.getName());
        return errorMsg;
    }

    private String checkout(GitProjectDto project, String branchName) {
        logger.info("开始切换分支,projectName={},branchName={}", project.getName(), branchName);
        String errorMsg = "";

        try {
            this.pull(project);
            GitUtil.checkout(project, branchName, sshSessionFactory, credentialsProvider);
        } catch (Exception e) {
            errorMsg = String.format("项目%s切换分支发生异常,异常信息:%s", project.getName(), e.getMessage());
            logger.error(errorMsg, e);
            return errorMsg;
        }

        logger.info("分支切换完成,projectName={},branchName={}", project.getName(), branchName);
        return errorMsg;
    }

    private void checkoutHaveException(GitProjectDto project, String branchName) throws Exception {
        logger.info("开始切换分支,projectName={},branchName={}", project.getName(), branchName);
        GitUtil.checkout(project, branchName, sshSessionFactory, credentialsProvider);
        logger.info("分支切换完成,projectName={},branchName={}", project.getName(), branchName);
    }

    private void checkoutNotPull(GitProjectDto project, String branchName) throws Exception {
        logger.info("开始切换分支,projectName={},branchName={}", project.getName(), branchName);
        GitUtil.checkout(project, branchName, false, sshSessionFactory, credentialsProvider);
        logger.info("分支切换完成,projectName={},branchName={}", project.getName(), branchName);
    }
    @Override
    public Git getGit(GitProjectDto project) throws Exception {
        return this.getGit(project.getRemoteUrl(), project.getLocalPath());
    }
    @Override
    public void deleteOldBranch(List<GitProjectDto> projectList, Date date) throws Exception {
        if (CollectionUtil.isEmpty(projectList)) {
            throw new ParamException("projectList is not be null");
        } else {
            List<String> errorMsgList = Collections.synchronizedList(new ArrayList());
            CountDownLatch latch = new CountDownLatch(projectList.size());
            projectList.forEach(project -> {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String errorMsg = GitServiceImpl.this.deleteOldBranch(project, date);
                        if (!StringUtil.isNullOrEmpty(errorMsg)) {
                            errorMsgList.add(errorMsg);
                        }

                        latch.countDown();
                    }
                });
            });
            latch.await();
            if (!CollectionUtil.isEmpty(errorMsgList)) {
                String errorMsg = "删除旧分支完成，部分项目删除失败，错误信息:" + this.errorMsgListToString(errorMsgList);
                logger.warn(errorMsg);
                throw new DeleteBranchException(errorMsg);
            }
        }
    }

    private String deleteOldBranch(GitProjectDto project, Date date) {
        logger.info("开始删除旧分支,projectName={},date={}", project.getName(), DateUtil.convertSqlTime(date));
        String errorMsg = "";
        int count = 0;

        try {
            Git git = project.getGit();
            List<Ref> allBranch = GitUtil.getAllBranch(git, credentialsProvider);
            if (CollectionUtil.isEmpty(allBranch)) {
                return errorMsg;
            }

            logger.info("获取分支完成，分支个数:{},projectName={}", allBranch.size(), project.getName());
            Iterator iterator = allBranch.iterator();

            label35:
            while(true) {
                while(true) {
                    if (!iterator.hasNext()) {
                        break label35;
                    }

                    Ref ref = (Ref)iterator.next();
                    String[] names = ref.getName().split("/");
                    String branchName = names[names.length - 1];
                    if (!branchName.contains("master") && !branchName.contains("production")) {
                        RevCommit revCommit = GitUtil.getCommitByObjectId(git, ref.getObjectId(), credentialsProvider);
                        int commitTimeInt = revCommit.getCommitTime();
                        Date commitTime = DateUtil.convert2Date(commitTimeInt);
                        if (commitTime.compareTo(date) <= 0) {
                            logger.info("该分支在指定日期之前，开始删除,projectName={},date={},branchName={},最后提交时间={}", new Object[]{project.getName(), DateUtil.convert2String(date, "yyyy-MM-dd HH:mm:ss"), branchName, DateUtil.convert2String(commitTime, "yyyy-MM-dd HH:mm:ss")});
                            this.deleteBranch(project, branchName);
                            ++count;
                        } else {
                            logger.info("该分支在指定日期之后，跳过不删除,projectName={},date={},branchName={},最后提交时间={}", new Object[]{project.getName(), DateUtil.convert2String(date, "yyyy-MM-dd HH:mm:ss"), branchName, DateUtil.convert2String(commitTime, "yyyy-MM-dd HH:mm:ss")});
                        }
                    } else {
                        logger.info("该分支为master或者production分支，跳过不删除,projectName={},date={},branchName={}", new Object[]{project.getName(), DateUtil.convertSqlTime(date), branchName});
                    }
                }
            }
        } catch (Exception e) {
            errorMsg = String.format("项目%s删除旧分支发生异常,异常信息:%s", project.getName(), e.getMessage());
            logger.error(errorMsg, e);
            return errorMsg;
        }

        logger.info("删除旧分支完成,projectName={},date={},删除数量={}", new Object[]{project.getName(), DateUtil.convert2String(date, "yyyy-MM-dd HH:mm:ss"), count});
        return errorMsg;
    }

    private Git getGit(String remoteUrl, String localPath) throws Exception {
        Git git = GitUtil.getGit(remoteUrl, localPath, sshSessionFactory, credentialsProvider);
        return git;
    }

    private String errorMsgListToString(List list) {
        return CollectionUtil.isEmpty(list) ? "" : "\n" + StringUtil.listToString(list, "\n");
    }

    static {
        config = configService.getConfig();
        initThreadPool();
        initCredentialsProvider();
        initSshSessionFactory(config);
    }
}

