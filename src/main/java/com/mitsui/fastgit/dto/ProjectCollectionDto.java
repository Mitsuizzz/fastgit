package com.mitsui.fastgit.dto;



import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ProjectCollectionDto {
    private String name;
    private List<String> projectList;
}
