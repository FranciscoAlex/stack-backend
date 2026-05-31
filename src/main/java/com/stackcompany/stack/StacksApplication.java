package com.stackcompany.stack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StacksApplication {

	public static void main(String[] args) {
		SpringApplication.run(StacksApplication.class, args);
	}

}
