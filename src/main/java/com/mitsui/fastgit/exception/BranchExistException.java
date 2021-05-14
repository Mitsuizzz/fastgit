package com.mitsui.fastgit.exception;

public class BranchExistException extends Exception{

    public BranchExistException() {
        super("该分支已存在，请检查您的分支");
    }

    public BranchExistException(String message) {
        super(message);
    }
}
