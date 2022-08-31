package com.eudagama12.example.filewriter.config;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class SSHClientConfig {

    @Value("${file.transfer.remoteHost}")
    String remoteHost;

    @Value("${file.transfer.username}")
    String username;

    @Value("${file.transfer.password}")
    String password;

    @Bean
    public SSHClient setupSshj() {
        try {
            SSHClient client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect(remoteHost);
            client.authPassword(username, password);
            log.info("Successfully created SSH Client");
            return client;
        } catch (IOException e) {
            log.warn("Failed to create SSH Client");
            return new SSHClient();
        }
    }
}
