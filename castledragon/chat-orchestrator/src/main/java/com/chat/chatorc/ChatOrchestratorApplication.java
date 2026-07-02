package com.chat.chatorc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.chat")
public class ChatOrchestratorApplication {
	public static void main(String[] args) {
		SpringApplication.run(ChatOrchestratorApplication.class, args);
	}
}
