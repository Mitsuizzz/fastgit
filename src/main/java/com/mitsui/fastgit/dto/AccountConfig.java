package com.mitsui.fastgit.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@XStreamAlias("accountConfig")
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class AccountConfig {
    @XStreamAlias("userName")
    private String userName;
    @XStreamAlias("password")
    private String password;
}
