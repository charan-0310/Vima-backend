package com.vimainsurance.vimaadmin.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers custom argument resolvers (e.g. {@link CurrentOrganization}) so controllers
 * receive validated, context-bound values without duplicating validation or audit setup.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentOrganizationArgumentResolver currentOrganizationArgumentResolver;

    public WebConfig(CurrentOrganizationArgumentResolver currentOrganizationArgumentResolver) {
        this.currentOrganizationArgumentResolver = currentOrganizationArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentOrganizationArgumentResolver);
    }
}
