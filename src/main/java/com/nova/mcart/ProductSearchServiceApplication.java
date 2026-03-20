package com.nova.mcart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ProductSearchServiceApplication {

	@Value("${server.port:8081}")
	private String port;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ProductSearchServiceApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(2048));
		application.run(args);
	}

	@Bean
	public CommandLineRunner startupMessage() {
		return args -> {
			System.out.println("\n" + "=".repeat(50));
			System.out.println("MCart Product Search Service is now ONLINE");
			System.out.println("Access URL: http://localhost:" + port);
			System.out.println("=".repeat(50) + "\n");
		};
	}

}


