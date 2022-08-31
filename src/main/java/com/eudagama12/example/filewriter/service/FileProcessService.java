package com.eudagama12.example.filewriter.service;

import com.eudagama12.example.filewriter.entity.FileStatus;
import com.eudagama12.example.filewriter.exception.FileCreateException;
import com.eudagama12.example.filewriter.exception.FileStatusRecordException;
import com.eudagama12.example.filewriter.exception.FileTransferException;
import com.eudagama12.example.filewriter.exception.FileWriteException;
import com.eudagama12.example.filewriter.model.SampleModel;
import com.eudagama12.example.filewriter.repository.FileStatusRepository;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileProcessService {

    @Value("${file.localDirectory}")
    String fileLocalDirectory;

    @Value("${file.gsm.commonName}")
    String gsmFileName;

    @Value("${file.extension}")
    String fileExtension;

    @Value("${file.transfer.remoteDirectory}")
    String remoteDirectory;

    @Autowired
    FileStatusRepository fileStatusRepository;

    @Autowired
    SSHClient sshClient;

    @Scheduled(cron = "${file.process.cron}")
    public void scheduledFileProcess() {
        try {
            createFileJob();
            writeToFileJob();
            sendFileJob();
        } catch (Exception e) {
            log.error("Failed to complete file process:{}", e.getMessage());
        }
    }

    public void createFileJob() {
        try {
            int fileId = initiateFileStatusRecord();
            String fileName = createFileName(fileId);
            String filePath = createFilePath(fileName);
            createFile(filePath);
            setFileName(fileId, fileName);
            log.info("Successfully created file id:{}", fileId);

        } catch (Exception e) {
            log.error("Failed to create file:{}", e.getMessage());
        }
    }

    public void writeToFileJob() {
        try {
            FileStatus fileStatus = getTodayFile("Initial");
            updateFileProcessStatus(fileStatus.getFileId(), "Processing");
            String filePath = createFilePath(fileStatus.getFileName());
            writeToFile(filePath, getSampleModels());
            int lineCount = countLines(filePath);
            if (getSampleModels().size() == lineCount) {
                updateFileProcessStatus(fileStatus.getFileId(), "File Write Success");
            } else {
                updateFileProcessStatus(fileStatus.getFileId(), "File Write Failed");
            }
            log.info("File write successful to file id:{}", fileStatus.getFileId());
        } catch (Exception e) {
            log.error("Failed to write to file:{}", e.getMessage());
        }
    }

    public void sendFileJob() {
        FileStatus fileStatus = getTodayFile("File Write Success");
        try {
            updateFileProcessStatus(fileStatus.getFileId(), "Processing");
            String filePath = createFilePath(fileStatus.getFileName());
            sendFile(filePath, fileStatus.getFileName());
            updateFileProcessStatus(fileStatus.getFileId(), "File Push Success");
        } catch (Exception e) {
            log.error("Failed to push file:{}", e.getMessage());
            updateFileProcessStatus(fileStatus.getFileId(), "File Push Failed");
        }
    }

    public void sendFile(String localFile, String remoteFileName) {
        try(SFTPClient sftpClient = sshClient.newSFTPClient()) {
            sftpClient.put(localFile, remoteDirectory + remoteFileName);
            log.info("Successfully pushed file <<{}>> to <<{}>>", localFile, remoteDirectory);
        } catch (Exception e) {
            log.error("Failed to SFTP file:{}", e.getMessage());
            throw new FileTransferException();
        }
    }

    public int countLines(String filePath) {
        try {
            List<String> fileStream = Files.readAllLines(Paths.get(filePath));
            return fileStream.size();
        } catch (IOException e) {
            log.error("Failed to count lines in file:{}", e.getMessage());
            return 0;
        }
    }

    public FileStatus getTodayFile(String processStatus) {
        try {
            String todayFileDatePattern = new SimpleDateFormat("yyyyMMdd").format(new Date());
            return fileStatusRepository
                    .findByFileNameContainingAndProcessStatus(todayFileDatePattern, processStatus);
        } catch (Exception e) {
            log.error("Failed to get today's {} file:{}", processStatus, e.getMessage());
            throw new FileStatusRecordException();
        }
    }

    public int initiateFileStatusRecord() {
        try {
            FileStatus fileStatus = new FileStatus();
            fileStatus.setProcessStatus("Initial");
            fileStatus.setFileRetryCount(0);
            fileStatus.setSftpRetryCount(0);
            fileStatus.setLastUpdatedTime(new Date());

            return fileStatusRepository.save(fileStatus).getFileId();
        } catch (Exception e) {
            log.error("Failed to create table record:{}", e.getMessage());
            throw new FileStatusRecordException();
        }
    }

    public void setFileName(int fileId, String fileName) {
        try {
            fileStatusRepository.setFileName(fileId, fileName, new Date());
        } catch (Exception e) {
            log.error("Failed to set file name in table record:{}", e.getMessage());
            throw new FileStatusRecordException();
        }
    }



    public void updateFileProcessStatus(int fileId, String processStatus) {
        try {
            fileStatusRepository.updateProcessStatus(fileId, processStatus, new Date());
        } catch (Exception e) {
            log.error("Failed to update process status in table record:{}", e.getMessage());
            throw new FileStatusRecordException();
        }
    }

    public String createFilePath(String fileName) {
        try {
            return fileLocalDirectory +
                    fileName;
        } catch (Exception e) {
            log.error("Failed to create file path:{}", e.getMessage());
            throw new FileCreateException();
        }
    }

    public String createFileName(int fileId) {
        try {
            String fileCreateDateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String sequenceNo = String.format("%05d", fileId);

            return gsmFileName +
                    "_" +
                    fileCreateDateTime +
                    "_" +
                    sequenceNo +
                    fileExtension;
        } catch (Exception e) {
            log.error("Failed to create file name:{}", e.getMessage());
            throw new FileCreateException();
        }
    }

    public void createFile(String fileName) {
        try {
            log.info("Creating file:{}", fileName);
            File file = new File(fileName);
            boolean success = file.createNewFile();
            log.info("Create file status:{}", success);
            if (!success) {
                log.warn("File with same name already exist");
                throw new FileCreateException();
            }
        } catch (Exception e) {
            log.error("Error:{}", e.getMessage());
            throw new FileCreateException();
        }
    }

    public void writeToFile(String fullFileName, List<SampleModel> sampleModelList) {
        try(RandomAccessFile stream = new RandomAccessFile(fullFileName, "rw")) {
            log.info("Initialize file write to file:{}", fullFileName);

            FileChannel channel = stream.getChannel();
            FileLock lock = channel.tryLock();

            byte[] strBytes = sampleModelList.stream()
                    .map(SampleModel::toString)
                    .collect(Collectors.joining("\r\n"))
                    .getBytes();

            ByteBuffer buffer = ByteBuffer.allocate(strBytes.length);
            buffer.put(strBytes);
            buffer.flip();
            channel.write(buffer);
            
            lock.release();
            channel.close();
        } catch (IOException e) {
            log.error("Error:{}", e.getMessage());
            throw new FileWriteException();
        }

    }

    private List<SampleModel> getSampleModels() {   //Mock
        SampleModel sampleModel = new SampleModel();
        sampleModel.setField1("hello1");
        sampleModel.setField2("hello2");
        sampleModel.setField3("hello3");

        SampleModel sampleModel2 = new SampleModel();
        sampleModel2.setField1("hello1");
        sampleModel2.setField2("hello2");
        sampleModel2.setField3("hello3");

        List<SampleModel> sampleModelList = new ArrayList<>();
        sampleModelList.add(sampleModel);
        sampleModelList.add(sampleModel2);
        return sampleModelList;
    }
}
