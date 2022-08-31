package com.eudagama12.example.filewriter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileWriterApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileWriterApplication.class, args);
	}

}
