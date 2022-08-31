package com.eudagama12.example.filewriter.exception;

public class FileCreateException extends RuntimeException{
    public FileCreateException() {
        super();
    }

    public FileCreateException(String message) {
        super(message);
    }
}
