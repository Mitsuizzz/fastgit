package com.mitsui.fastgit.constant;

import lombok.NoArgsConstructor;

/**
 * @author mitsui
 */
@NoArgsConstructor
public class GlobalConstant {
    public static final String CONFIG_FILE = "config.xml";
    public static final String OPERATE_TYPE_CREATE_BRANCH = "创建分支";
    public static final String OPERATE_TYPE_MERGE_BRANCH = "merge分支";
    public static final String OPERATE_TYPE_DELETE_BRANCH = "删除分支";
    public static final String OPERATE_TYPE_PUSH_BRANCH = "push分支";
    public static final String OPERATE_TYPE_CREATE_TAG = "创建tag";
    public static final String OPERATE_TYPE_DELETE_TAG = "删除tag";
    public static final String OPERATE_TYPE_CHECKOUT_BRANCH = "切换分支";
    public static final String OPERATE_TYPE_DELETE_OLD_BRANCH = "删除一个月之前的分支";
    public static final String POM_FILE = "pom.xml";
    public static final String POM_VERSION_SUFFIX = "-SNAPSHOT";
    public static final String CREATE_BRANCH_MSG = "create branch";
    public static final String MASTER_BRANCH = "master";
    public static final String PROJECT_COLLECTION_CONFIG_FILE = "ProjectCollectionConfig.json";
}
