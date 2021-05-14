package com.mitsui.fastgit.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@XStreamAlias("git")
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class GitConfigDto {
    @XStreamAlias("sshConfig")
    private SshConfigDto sshConfig;
    @XStreamAlias("accountConfig")
    private AccountConfig accountConfig;
    @XStreamAlias("dealBranchThreadCount")
    private String dealBranchThreadCount;
    @XStreamAlias("parentPomName")
    private String parentPomName;
    @XStreamAlias("projects")
    private List<GitProjectDto> projectList;
}
