package com.eudagama12.example.filewriter.service;

import com.eudagama12.example.filewriter.exception.FileCreateException;
import com.eudagama12.example.filewriter.exception.FileTransferException;
import com.eudagama12.example.filewriter.exception.FileWriteException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Setter
@Slf4j
public class FileProcessService {

    @Value("${file.allCxTransaction.localDirectory}")
    private String fileLocalDirectory;

    @Value("${file.allCxTransaction.namePrefix}")
    private String fileNamePrefix;

    @Value("${file.allCxTransaction.extension}")
    private String fileExtension;

    @Value("${file.allCxTransaction.transfer.remoteHost}")
    private String remoteHost;

    @Value("${file.allCxTransaction.transfer.username}")
    private String username;

    @Value("${file.allCxTransaction.transfer.password}")
    private String password;

    @Value("${file.allCxTransaction.transfer.remoteDirectory}")
    private String fileRemoteDirectory;

    @Value("${file.allCxTransaction.transfer2.remoteHost}")
    private String remoteHost2;

    @Value("${file.allCxTransaction.transfer2.username}")
    private String username2;

    @Value("${file.allCxTransaction.transfer2.password}")
    private String password2;

    @Value("${file.allCxTransaction.transfer2.remoteDirectory}")
    private String fileRemoteDirectory2;

    /**
     * Scheduler trigger method.
     */
    public void generateReport() {
        log.info("Initiating summary report scheduler");

        String fileName = getFileName();
        List<String> stringList = new ArrayList<>();
        writeToFile(fileLocalDirectory + fileName, stringList);
        sendFile(remoteHost, username, password, fileLocalDirectory + fileName, fileRemoteDirectory + fileName);
        sendFile(remoteHost2, username2, password2, fileLocalDirectory + fileName, fileRemoteDirectory2 + fileName);

        log.info("Completed summary report scheduler");
    }


    /**
     * Create BU requested dump file name.
     * @return Dump file name.
     */
    protected String getFileName() {
        try {
            log.debug("Creating file name");

            String fileName = fileNamePrefix +
                    new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) +
                    fileExtension;

            log.info("File name: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to create file name:{}", e.getMessage());
            throw new FileCreateException();
        }
    }

    /**
     * Write data in POJO to file.
     * @param fullFileName File name of file to be created.
     * @param stringList Data POJO.
     */
    protected void writeToFile(String fullFileName, List<String> stringList) {
        try (RandomAccessFile stream = new RandomAccessFile(fullFileName, "rw")) {

            log.debug("Initialize file write to file:{}", fullFileName);


            FileChannel channel = stream.getChannel();
            FileLock lock = channel.tryLock();

            byte[] strBytes = String.join("|", stringList).getBytes();

            ByteBuffer buffer = ByteBuffer.allocate(strBytes.length);
            buffer.put(strBytes);
            buffer.flip();
            channel.write(buffer);

            lock.release();
            channel.close();
            log.info("File write successful");
        } catch (IOException e) {
            log.error("Error:{}", e.getMessage());
            throw new FileWriteException();
        }
    }

    /**
     * Send file to location.
     * @param localFile Local file.
     * @param remoteFile Remote file.
     */
    protected void sendFile(String remoteHost, String username, String password, String localFile, String remoteFile) {
        log.debug("Initiating SSH Client");

        try (SSHClient client = new SSHClient()) {
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect(remoteHost);
            client.authPassword(username, password);

            sftpFile(localFile, remoteFile, client);
        } catch (Exception e) {
            log.error("Failed to establish SSH Client:{}", e.getMessage());
            throw new FileTransferException("Failed to establish SSH Client");
        }
    }

    /**
     * Send file - Sub method SFTP client
     * @param localFile Local file.
     * @param remoteFile Remote file.
     * @param client SSH client.
     */
    protected void sftpFile(String localFile, String remoteFile, SSHClient client) {

        log.debug("Initiating SFTP Client");


        try (SFTPClient sftpClient = client.newSFTPClient()) {
            log.debug("Attempting to SFTP Local:{} to Remote:{}", localFile, remoteFile);


            sftpClient.put(localFile, remoteFile);
            log.info("Successfully put file to {} {}", client.getRemoteHostname(), remoteFile);
        } catch (IOException e) {
            log.error("Failed to SFTP file:{}", e.getMessage());
            throw new FileTransferException("Failed to SFTP file");
        }
    }
}
