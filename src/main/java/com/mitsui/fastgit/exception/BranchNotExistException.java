package com.mitsui.fastgit.exception;

public class BranchNotExistException extends Exception{
    public BranchNotExistException() {
        super("该分支不存在，请检查您的分支");
    }

    public BranchNotExistException(String message) {
        super(message);
    }
}
