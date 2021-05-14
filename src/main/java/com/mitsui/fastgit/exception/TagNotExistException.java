package com.mitsui.fastgit.exception;

public class TagNotExistException extends Exception {
    public TagNotExistException() {
        super("该tag不存在，请检查您的tag");
    }

    public TagNotExistException(String message) {
        super(message);
    }
}
