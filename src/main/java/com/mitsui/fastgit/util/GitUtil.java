package com.mitsui.fastgit.util;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mitsui.fastgit.dto.GitProjectDto;
import com.mitsui.fastgit.exception.*;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.DeleteTagCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitUtil {
    private static final Logger logger = LoggerFactory.getLogger(GitUtil.class);


    public static CredentialsProvider getCredentialsProvider(String userName, String password) {
        return !StringUtil.isNullOrEmpty(userName) && !StringUtil.isNullOrEmpty(password) ? new UsernamePasswordCredentialsProvider(userName, password) : null;
    }

    public static SshSessionFactory getCustomSshSessionFactory(final String privateKeyPath) {
        if (StringUtil.isNullOrEmpty(privateKeyPath)) {
            return null;
        } else {
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(Host host, Session session) {
                }
                @Override
                protected JSch getJSch(Host hc, FS fs) throws JSchException {
                    JSch jsch = super.getJSch(hc, fs);
                    jsch.removeAllIdentity();
                    jsch.addIdentity(privateKeyPath);
                    return jsch;
                }
            };
            return sshSessionFactory;
        }
    }

    public static Git getGit(String uri, String localDir, final SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        Git git;
        if ((new File(localDir)).exists()) {
            git = Git.open(new File(localDir));
        } else {
            CloneCommand cmd = Git.cloneRepository();
            if (credentialsProvider != null) {
                cmd.setCredentialsProvider(credentialsProvider);
            }

            if (sshSessionFactory != null) {
                cmd.setTransportConfigCallback(new TransportConfigCallback() {
                    public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport)transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });
            }

            git = cmd.setURI(uri).setDirectory(new File(localDir)).call();
        }

        git.getRepository().getConfig().setInt("http", (String)null, "postBuffer", 536870912);
        return git;
    }

    public static Repository getRepository(Git git) {
        return git.getRepository();
    }

    public static void pull(GitProjectDto project, final SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        logger.info("开始pull最新代码,项目{}", project.getName());
        Git git = project.getGit();
        PullCommand cmd = git.pull();
        if (credentialsProvider != null) {
            cmd.setCredentialsProvider(credentialsProvider);
        }

        if (sshSessionFactory != null) {
            cmd.setTransportConfigCallback(new TransportConfigCallback() {
                @Override
                public void configure(Transport transport) {
                    SshTransport sshTransport = (SshTransport)transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        }

        try {
            cmd.call();
        } catch (NoHeadException e) {
            logger.error("pull 失败，重新切换到develop分支，该项目存在未提交的更改", e);
            checkout(project, "develop", sshSessionFactory, credentialsProvider);
            cmd.call();
        }

        logger.info("pull最新代码完成,项目{}", project.getName());
    }

    public static void push(Git git, String filePattern, String message, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        push(git, filePattern, message, sshSessionFactory, true, credentialsProvider);
    }

    public static void push(Git git, String filePattern, String message, final SshSessionFactory sshSessionFactory, boolean isForcePush, CredentialsProvider credentialsProvider) throws Exception {
        Status status = git.status().call();
        if (!isForcePush && !status.hasUncommittedChanges()) {
            logger.warn("提交的文件内容都没有被修改，不做提交");
        } else {
            List<String> conflictFileList = getConflictFile(git);
            if (!CollectionUtil.isEmpty(conflictFileList)) {
                throw new ExistConflictFileException(String.format("存在未解决冲突的文件,文件列表 %s", conflictFileList.toString()));
            } else {
                git.add().addFilepattern(filePattern).call();
                git.add().setUpdate(true);
                git.commit().setMessage(message).call();
                PushCommand cmd = git.push();
                if (credentialsProvider != null) {
                    cmd.setCredentialsProvider(credentialsProvider);
                }

                if (sshSessionFactory != null) {
                    cmd.setTransportConfigCallback(new TransportConfigCallback() {
                        public void configure(Transport transport) {
                            SshTransport sshTransport = (SshTransport)transport;
                            sshTransport.setSshSessionFactory(sshSessionFactory);
                        }
                    });
                }

                cmd.call();
            }
        }
    }

    private static List<String> getConflictFile(Git git) throws GitAPIException {
        return null;
    }

    public static void pushAll(Git git, String message, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        push(git, ".", message, sshSessionFactory, credentialsProvider);
    }

    public static List<Ref> getAllTag(Git git, CredentialsProvider credentialsProvider) throws Exception {
        ListTagCommand cmd = git.tagList();
        List<Ref> tagList = cmd.call();
        return tagList;
    }

    public static List<Ref> getAllBranch(Git git, CredentialsProvider credentialsProvider) throws Exception {
        ListBranchCommand cmd = git.branchList();
        List<Ref> branchList = cmd.setListMode(ListMode.ALL).call();
        return branchList;
    }

    public static RevCommit getCommitByObjectId(Git git, ObjectId objectId, CredentialsProvider credentialsProvider) throws IOException {
        if (git != null && objectId != null) {
            Repository repo = git.getRepository();
            RevWalk walk = new RevWalk(repo);
            RevCommit commit = walk.parseCommit(objectId);
            return commit;
        } else {
            return null;
        }
    }

    public static List<String> getAllBranchName(Git git, CredentialsProvider credentialsProvider) throws Exception {
        List<Ref> branchList = getAllBranch(git, credentialsProvider);
        List<String> branchNameList = new ArrayList();
        if (CollectionUtil.isEmpty(branchList)) {
            return branchNameList;
        } else {
            branchList.forEach(ref -> {
                String[] names = ref.getName().split("/");
                String name = names[names.length - 1];
                branchNameList.add(name);
            });

            return branchNameList;
        }
    }

    public static List<String> getAllTagName(Git git, CredentialsProvider credentialsProvider) throws Exception {
        List<Ref> tagList = getAllTag(git, credentialsProvider);
        List<String> tagNameList = new ArrayList();
        if (CollectionUtil.isEmpty(tagList)) {
            return tagNameList;
        } else {
            tagList.forEach(ref -> {
                String[] names = ref.getName().split("/");
                String name = names[names.length - 1];
                tagNameList.add(name);
            });
            return tagNameList;
        }
    }

    public static void checkout(GitProjectDto project, String branchName, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        checkout(project, branchName, true, sshSessionFactory, credentialsProvider);
    }

    public static void checkout(GitProjectDto project, String branchName, boolean isPull, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        Git git = project.getGit();
        if (!checkBranchIsClean(git, credentialsProvider)) {
            throw new ProjectNotCleanExcption();
        } else {
            if (checkBranchIsExistInLocal(git, branchName, credentialsProvider)) {
                git.checkout().setCreateBranch(false).setName(branchName).call();
                if (isPull) {
                    pull(project, sshSessionFactory, credentialsProvider);
                }
            } else {
                git.checkout().setCreateBranch(true).setName(branchName).setStartPoint("origin/" + branchName).call();
            }

        }
    }

    public static boolean checkBranchIsClean(Git git, CredentialsProvider credentialsProvider) throws GitAPIException {
        Status status = git.status().call();
        return status.isClean();
    }

    private static void checkout(Git git, List<String> fileList, CredentialsProvider credentialsProvider) throws Exception {
        CheckoutCommand cmd = git.checkout();
        cmd.addPaths(fileList).call();
    }

    public static void createBranchByBranch(GitProjectDto project, String branchName, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        Git git = project.getGit();
        Ref ref = git.branchCreate().setName(branchName).call();
        pushNewBranch(git, branchName, ref, sshSessionFactory, credentialsProvider);
    }

    private static void pushNewBranch(Git git, String branchName, Ref ref, final SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws GitAPIException {
        PushCommand cmd = git.push();
        if (sshSessionFactory != null) {
            cmd.setTransportConfigCallback(new TransportConfigCallback() {
                @Override
                public void configure(Transport transport) {
                    SshTransport sshTransport = (SshTransport)transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        }

        if (credentialsProvider != null) {
            cmd.setCredentialsProvider(credentialsProvider);
        }

        cmd.add(ref).call();
        git.branchCreate().setName(branchName).setForce(true).setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM).setStartPoint("origin/" + branchName).call();
    }

    public static void createBranchByTag(GitProjectDto project, String sourceTagName, String branchName, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        Git git = project.getGit();
        Ref ref = git.branchCreate().setName(branchName).setStartPoint("refs/tags/" + sourceTagName).call();
        pushNewBranch(git, branchName, ref, sshSessionFactory, credentialsProvider);
    }

    public static void createBranch(GitProjectDto project, String sourceBranchName, String createBranchName, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        try {
            boolean sourceBranchIsTag = true;
            if (checkBranchIsExist(project.getGit(), sourceBranchName, sshSessionFactory, credentialsProvider)) {
                sourceBranchIsTag = false;
            }

            if (!sourceBranchIsTag) {
                createBranchByBranch(project, createBranchName, sshSessionFactory, credentialsProvider);
            } else {
                createBranchByTag(project, sourceBranchName, createBranchName, sshSessionFactory, credentialsProvider);
            }

        } catch (RefNotFoundException e) {
            logger.warn("源分支不存在", e);
            throw new BranchNotExistException("源分支不存在，请检查您的分支");
        }
    }

    public static void deleteBranch(Git git, String branchName, final SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        if (!checkBranchIsExist(git, branchName, sshSessionFactory, credentialsProvider)) {
            throw new BranchNotExistException("该分支不存在，请检查您的分支");
        } else {
            git.branchDelete().setBranchNames(new String[]{String.format("refs/heads/%s", branchName)}).setForce(true).call();
            RefSpec refSpec = (new RefSpec()).setSource((String)null).setDestination(String.format("refs/heads/%s", branchName));
            PushCommand cmd = git.push();
            if (credentialsProvider != null) {
                cmd.setCredentialsProvider(credentialsProvider);
            }

            if (sshSessionFactory != null) {
                cmd.setTransportConfigCallback(new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport)transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });
            }

            cmd.setRefSpecs(new RefSpec[]{refSpec}).setRemote("origin").call();
        }
    }

    public static boolean checkBranchIsExist(Git git, String branchName, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        List<String> branchNameList = getAllBranchName(git, credentialsProvider);
        if (CollectionUtil.isEmpty(branchNameList)) {
            return false;
        } else {
            return branchNameList.contains(branchName);
        }
    }

    public static boolean checkTagIsExist(Git git, String tagName, SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        List<String> tagNameList = getAllTagName(git, credentialsProvider);
        if (CollectionUtil.isEmpty(tagNameList)) {
            return false;
        } else {
            return tagNameList.contains(tagName);
        }
    }

    public static boolean checkBranchIsExistInLocal(Git git, String branchName, CredentialsProvider credentialsProvider) throws GitAPIException {
        List<Ref> refs = git.branchList().call();
        Iterator iterator = refs.iterator();

        Ref ref;
        do {
            if (!iterator.hasNext()) {
                return false;
            }

            ref = (Ref)iterator.next();
        } while(!ref.getName().contains(branchName));

        return true;
    }

    public static boolean merge(GitProjectDto project, String sourceBranchName, String targetBranchName, SshSessionFactory sshSessionFactory, List<String> ignoreFileList, CredentialsProvider credentialsProvider, List<String> conflictNameList) throws Exception {
        boolean sourceIsTag = true;
        Git git = project.getGit();
        if (checkBranchIsExist(git, sourceBranchName, sshSessionFactory, credentialsProvider)) {
            sourceIsTag = false;
        }

        if (!sourceIsTag) {
            try {
                checkout(project, sourceBranchName, sshSessionFactory, credentialsProvider);
            } catch (RefNotFoundException e) {
                throw new BranchNotExistException("源分支不存在，请检查您的分支");
            }
        }

        try {
            checkout(project, targetBranchName, sshSessionFactory, credentialsProvider);
        } catch (RefNotFoundException e) {
            throw new BranchNotExistException("目标分支不存在，请检查您的分支");
        }

        if (sourceIsTag && !checkTagIsExist(git, sourceBranchName, sshSessionFactory, credentialsProvider)) {
            throw new BranchNotExistException("源分支不存在，请检查您的分支");
        } else {
            Ref sourceRef = git.getRepository().findRef(sourceBranchName);
            MergeCommand mgCmd = git.merge();
            mgCmd.include(sourceRef);
            MergeResult res = mgCmd.call();
            boolean isForcePush = true;
            if (res.getMergeStatus().equals(MergeStatus.CONFLICTING)) {
                List<String> revertFileList = new ArrayList();
                List<String> conflictFileList = new ArrayList(res.getConflicts().keySet());
                if (!CollectionUtil.isEmpty(ignoreFileList)) {
                    conflictFileList.forEach(str -> {
                        if (ignoreFileList.contains(str)) {
                            revertFileList.add(str);
                        }
                    });
                }

                if (!CollectionUtil.isEmpty(revertFileList)) {
                    logger.info("merge 自动忽略文件 {},开始撤销更改", revertFileList.toString());
                    reset(git, revertFileList, credentialsProvider);
                    checkout(git, revertFileList, credentialsProvider);
                    isForcePush = true;
                    logger.info("merge 自动忽略文件 {},撤销更改完成", revertFileList.toString());
                }

                if (revertFileList.size() != conflictFileList.size()) {
                    conflictNameList.addAll(conflictFileList);
                    return false;
                }
            } else {
                if (res.getMergeStatus().equals(MergeStatus.ALREADY_UP_TO_DATE)) {
                    logger.info("merge 项目{}完成，未合并到新代码", project.getName());
                    return true;
                }

                if (!res.getMergeStatus().isSuccessful()) {
                    throw new MergeBranchException("merge 失败,merge 结果" + res.getMergeStatus().name());
                }

                logger.info("merge 项目{}完成，已合并到新代码,merge 结果:{}", project.getName(), res.getMergeStatus().toString());
            }

            push(git, ".", "merge branch from " + sourceBranchName, sshSessionFactory, isForcePush, credentialsProvider);
            return true;
        }
    }

    private static void reset(Git git, List<String> pathList, CredentialsProvider credentialsProvider) throws GitAPIException {
        ResetCommand cmd = git.reset();

        pathList.forEach(path -> {
            cmd.addPath(path);
        });

        cmd.call();
    }

    public static void createTag(Git git, String tagName, final SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws GitAPIException {
        Ref ref = git.tag().setName(tagName).call();
        PushCommand cmd = git.push();
        if (credentialsProvider != null) {
            cmd.setCredentialsProvider(credentialsProvider);
        }

        if (sshSessionFactory != null) {
            cmd.setTransportConfigCallback(new TransportConfigCallback() {
                @Override
                public void configure(Transport transport) {
                    SshTransport sshTransport = (SshTransport)transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        }

        cmd.add(ref).call();
    }

    public static void deleteTag(Git git, String tagName, final SshSessionFactory sshSessionFactory, CredentialsProvider credentialsProvider) throws Exception {
        if (!checkTagIsExist(git, tagName, sshSessionFactory, credentialsProvider)) {
            throw new TagNotExistException();
        } else {
            DeleteTagCommand deleteTagCommand = git.tagDelete();
            deleteTagCommand.setTags(new String[]{String.format("refs/tags/%s", tagName)}).call();
            RefSpec refSpec = (new RefSpec()).setSource((String)null).setDestination(String.format("refs/tags/%s", tagName));
            PushCommand cmd = git.push();
            if (credentialsProvider != null) {
                cmd.setCredentialsProvider(credentialsProvider);
            }

            if (sshSessionFactory != null) {
                cmd.setTransportConfigCallback(new TransportConfigCallback() {
                    public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport)transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });
            }

            cmd.setRefSpecs(new RefSpec[]{refSpec}).setRemote("origin").call();
        }
    }
}
