package com.chat.domserv.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AttachmentResourceConfig implements WebMvcConfigurer {
	@Value("${chat.attachment.upload-dir:uploads/chat}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

		registry.addResourceHandler("/uploads/chat/**").addResourceLocations(uploadPath.toUri().toString());
	}
}