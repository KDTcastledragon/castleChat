package com.chat.chengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.chat")
public class ChannelEngineApplication {
	public static void main(String[] args) {
		SpringApplication.run(ChannelEngineApplication.class, args);
	}
}


