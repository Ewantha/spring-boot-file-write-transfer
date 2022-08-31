package com.eudagama12.example.filewriter.repository;

import com.eudagama12.example.filewriter.entity.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Date;

@Repository
public interface FileStatusRepository extends JpaRepository<FileStatus, Integer> {

    @Modifying
    @Transactional
    @Query("update FileStatus f set f.fileName=:fileName, f.lastUpdatedTime=:updateTime where f.fileId=:fileId")
    void setFileName(int fileId, String fileName, Date updateTime);

    @Modifying
    @Transactional
    @Query("update FileStatus f set f.processStatus=:processStatus, f.lastUpdatedTime=:updateTime where f.fileId=:fileId")
    void updateProcessStatus(int fileId, String processStatus, Date updateTime);

    FileStatus findByFileNameContainingAndProcessStatus(String date, String processStatus);
}