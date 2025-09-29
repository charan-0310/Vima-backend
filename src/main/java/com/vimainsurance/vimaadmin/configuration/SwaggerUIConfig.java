package com.vimainsurance.vimaadmin.configuration;

import java.util.List;

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
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerUIConfig implements WebMvcConfigurer {

	@Value("${auth.type}")
	private String BEARER_AUTHENTICATION;
	final String GOOGLE_AUTH = "GoogleOAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Vima Insurance API")
						.description("API documentation for Vima Insurance Backend")
						.version("1.0")
						.contact(new Contact()
								.name("Vima Insurance")
								.email("support@vimainsurance.com")
								.url("https://vimainsurance.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0")))
				.servers(List.of(
					new Server()
								.url("http://localhost:7219")
								.description("Local Server"),
						new Server()
								.url("https://api.vimainsurance.com")
								.description("Production Server"),
						new Server()
								.url("https://dev-api.vimainsurance.com")
								.description("Development Server")
				))
				.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
				.components(new Components()
						.addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

	private SecurityScheme createAPIKeyScheme() {
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.bearerFormat("JWT")
				.scheme("bearer")
				.description("Enter the JWT token in the format: Bearer <token>");
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
