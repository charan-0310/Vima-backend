package com.vimainsurance.vimaadmin.configuration;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.ZohoToken;
import com.vimainsurance.vimaadmin.repository.ITokenRepository;
import com.zoho.api.authenticator.OAuthToken;
import com.zoho.api.authenticator.Token;
import com.zoho.crm.api.Initializer;
import com.zoho.crm.api.UserSignature;
import com.zoho.crm.api.dc.DataCenter.Environment;
import com.zoho.crm.api.dc.INDataCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class ZohoConfig {

    private static final Logger logger = LoggerFactory.getLogger(ZohoConfig.class);

    @Value("${zoho.crm.client-id}")
    private String clientId;

    @Value("${zoho.crm.client-secret}")
    private String clientSecret;

    @Value("${zoho.crm.redirect-url}")
    private String redirectUrl;

    @Value("${zoho.crm.user-email}")
    private String userEmail;

    @Value("${zoho.crm.refresh-token}")
    private String refreshToken;

    @Autowired
    private ITokenRepository tokenRepository;

    // @Bean
    // public UserSignature userSignature() throws SDKException {
    //     return new UserSignature(userEmail);
    // }

    // @Bean
    // public Environment environment() {
    //     return INDataCenter.PRODUCTION;
    // }

    // @Bean
    // public Token token() {
    //     return new OAuthToken(clientId, clientSecret, redirectUrl, OAuthToken.TokenType.GRANT);
    // }

    // @Bean
    // public TokenStore tokenStore() throws Exception {
    //     return new FileStore("tokens.txt");
    // }

    // @Bean
    // public SDKConfig sdkConfig() {
    //     return new SDKConfig.Builder().build();
    // }

    // @Bean
    // public Initializer initializer(UserSignature userSignature, Environment environment, Token token, SDKConfig sdkConfig,
    //         TokenStore tokenStore) throws Exception {
    //     Initializer.initialize(userSignature, environment, token, tokenStore, sdkConfig, "zoho_crm_logger");
    //     return Initializer.getInitializer();
    // }

    public void initializeZohoManually(String code) throws Exception {
        UserSignature user = new UserSignature(userEmail);
        Environment environment = INDataCenter.PRODUCTION;
    
        Optional<ZohoToken> zohotoken = tokenRepository.findById(1L);
        logger.info("refresh token: " + zohotoken.isPresent());
        Token token = new OAuthToken.Builder().clientID(clientId).clientSecret(clientSecret).refreshToken(zohotoken.get().getRefreshToken()).redirectURL(redirectUrl).build();
        
        new Initializer.Builder().environment(environment).token(token).initialize();
        // initializer.initialize(user, environment, token, store, config, "zoho_sdk_logger/");
        // initializer.initialize(user, environment, token, store, config, "zoho_sdk_logger/");
        
        // System.out.println("✅ Zoho SDK successfully initialized.");
    }
} 