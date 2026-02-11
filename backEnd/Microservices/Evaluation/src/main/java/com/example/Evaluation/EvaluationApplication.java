package com.example.Evaluation;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.io.File;

@SpringBootApplication
@EnableDiscoveryClient
public class EvaluationApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(EvaluationApplication.class, args);
	}

	private static void loadEnv() {
		System.out.println("Current working directory: " + new File(".").getAbsolutePath());
		String[] paths = {"backEnd/.env", ".env", "../../.env", "../../../.env"};
		for (String path : paths) {
			File file = new File(path);
			if (file.exists()) {
				Dotenv dotenv = Dotenv.configure()
						.directory(file.getParent())
						.filename(file.getName())
						.ignoreIfMissing()
						.load();
				dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
				System.out.println("Loaded .env from: " + file.getAbsolutePath());
				break;
			}
		}
	}

}
