package com.mitsui.fastgit.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.api.Git;

@XStreamAlias("project")
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class GitProjectDto {
    @XStreamAlias("name")
    private String name;
    @XStreamAlias("localPath")
    private String localPath;
    @XStreamAlias("remoteUrl")
    private String remoteUrl;
    @XStreamAlias("parentPomVersionKey")
    private String parentPomVersionKey;
    private Git git;
}
