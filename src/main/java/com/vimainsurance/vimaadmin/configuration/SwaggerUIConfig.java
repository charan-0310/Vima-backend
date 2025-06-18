package com.vimainsurance.vimaadmin.configuration;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration
public class SwaggerUIConfig implements WebMvcConfigurer {


	@Value("${auth.type}")
	private String BEARER_AUTHENTICATION;
	final String GOOGLE_AUTH = "GoogleOAuth";


	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI().addSecurityItem(new SecurityRequirement().addList(BEARER_AUTHENTICATION))
				.components(new Components().addSecuritySchemes(BEARER_AUTHENTICATION, createAPIKeyScheme()))
				.info(new Info().title("Vima API").description("Vima Backend API.").version("1.0")
						.contact(new Contact().name("Vima Insurance")));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

    private SecurityScheme createAPIKeyScheme() {
		return new SecurityScheme().type(SecurityScheme.Type.HTTP).bearerFormat("JWT").scheme("bearer");
	}

	/**
	 * Authentication -> User Controller-Default Definition filter for Swagger-ui.
	 */
	@Bean
	public GroupedOpenApi userAuthenticationGroup() {
		return GroupedOpenApi.builder().group("Authentication").pathsToMatch("/api/v1/login/**").build();
	}

	/**
	 * Main - All Controllers -> Fetches all controllers in Swagger-ui.
	 */
	@Bean
	public GroupedOpenApi allControllersGroup() {
		return GroupedOpenApi.builder().group("Main - All Controllers").pathsToMatch("/api/**").build();
	}



}
