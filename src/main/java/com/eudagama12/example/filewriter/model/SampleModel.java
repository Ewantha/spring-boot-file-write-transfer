package com.eudagama12.example.filewriter.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SampleModel implements Serializable {

    private String field1;
    private String field2;
    private String field3;

    @Override
    public String toString() {
        return String.join("|", field1, field2, field3);
    }
}
