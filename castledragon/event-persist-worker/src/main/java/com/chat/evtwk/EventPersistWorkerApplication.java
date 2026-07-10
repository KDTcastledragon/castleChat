// kafka 이벤트를 소비해서 db에 반영하는 독립 worker 애플리케이션이다.
package com.chat.evtwk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventPersistWorkerApplication {
	public static void main(String[] args) {
		SpringApplication.run(EventPersistWorkerApplication.class, args);
	}
}
