package com.mitsui.fastgit.exception;

public class TagExistException extends Exception {
    public TagExistException() {
        super("该tag已存在，请检查您的tag");
    }

    public TagExistException(String message) {
        super(message);
    }
}
