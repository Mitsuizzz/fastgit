package com.mitsui.fastgit.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@XStreamAlias("sshConfig")
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class SshConfigDto {
    @XStreamAlias("privateKeyPath")
    private String privateKeyPath;
}
