package com.zubair.scribe_app_poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ScribeAppPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScribeAppPocApplication.class, args);
	}

}
