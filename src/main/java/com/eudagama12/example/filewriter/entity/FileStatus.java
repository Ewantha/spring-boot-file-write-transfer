package com.eudagama12.example.filewriter.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "file")
@Getter
@Setter
public class FileStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILE_ID", nullable = false)
    private Integer fileId;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "PROCESS_STATUS")
    private String processStatus;

    @Column(name = "FILE_RETRY_COUNT")
    private Integer fileRetryCount;

    @Column(name = "SFTP_RETRY_COUNT")
    private Integer sftpRetryCount;

    @Column(name = "RECORD_COUNT")
    private Integer recordCount;

    @Column(name = "LAST_UPDATED_TIME")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "${app.timezone}")
    private Date lastUpdatedTime;

}