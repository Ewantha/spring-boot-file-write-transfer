package com.eudagama12.example.filewriter.exception;

public class FileTransferException extends RuntimeException{
    public FileTransferException() {
        super();
    }

    public FileTransferException(String message) {
        super(message);
    }
}
