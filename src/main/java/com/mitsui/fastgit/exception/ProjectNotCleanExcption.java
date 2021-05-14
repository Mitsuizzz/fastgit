package com.mitsui.fastgit.exception;

public class ProjectNotCleanExcption extends Exception {

    public ProjectNotCleanExcption(String message) {
        super(message);
    }

    public ProjectNotCleanExcption() {
        super("项目存在未提交的更改,请提交完成后再进行操作");
    }

}
