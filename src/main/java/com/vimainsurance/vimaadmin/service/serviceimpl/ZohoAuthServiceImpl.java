// package com.vimainsurance.vimaadmin.service.serviceimpl;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import com.vimainsurance.vimaadmin.service.IZohoAuthService;
// import com.zoho.api.authenticator.OAuthToken;
// import com.zoho.api.authenticator.Token;
// import com.zoho.api.authenticator.store.FileStore;
// import com.zoho.api.authenticator.store.TokenStore;
// import com.zoho.crm.api.Initializer;
// import com.zoho.crm.api.SDKConfig;
// import com.zoho.crm.api.UserSignature;
// import com.zoho.crm.api.dc.DataCenter;
// import com.zoho.crm.api.dc.INDataCenter;
// import com.zoho.crm.api.dc.USDataCenter;

// @Service
// public class ZohoAuthServiceImpl implements IZohoAuthService {
    
//     // @Autowired
//     // private  UserSignature userSignature;
//     // @Autowired
//     // private  Token token;
//     // @Autowired
//     // private  TokenStore tokenStore;
//     // @Autowired
//     // private  SDKConfig sdkConfig;

//     @Value("${zoho.crm.client-id}")
//     private String clientId;

//     @Value("${zoho.crm.client-secret}")
//     private String clientSecret;

//     @Value("${zoho.crm.redirect-url}")
//     private String redirectUrl;

//     @Value("${zoho.crm.user-email}")
//     private String userEmail;

//     // public ZohoAuthServiceImpl(
//     //         @Qualifier("zohoUserSignature") UserSignature userSignature,
//     //         @Qualifier("zohoToken") Token token,
//     //         @Qualifier("zohoTokenStore") TokenStore tokenStore,
//     //         @Qualifier("zohoSDKConfig") SDKConfig sdkConfig) {
//     //     this.userSignature = userSignature;
//     //     this.token = token;
//     //     this.tokenStore = tokenStore;
//     //     this.sdkConfig = sdkConfig;
//     // }

//     @Override
//     public void initializeZohoCRM() throws Exception {
//         // if (Initializer.getInitializer() == null) {
//         //     DataCenter.Environment environment = USDataCenter.PRODUCTION;
//         //     Initializer.initialize(userSignature, environment, token, tokenStore, sdkConfig, "sdk_logger");
//         // }
//     }

//     @Override
//     public void refreshZohoToken() throws Exception {
//         // if (Initializer.getInitializer() == null) {
//         //     initializeZohoCRM();
//         // } else {
//         //     // Re-initialize with fresh configuration
//         //     DataCenter.Environment environment = USDataCenter.PRODUCTION;
//         //     Initializer.initialize(userSignature, environment, token, tokenStore, sdkConfig, "sdk_logger");
//         // }
//     }
//     @Override
//     public void initializeZohoManually() throws Exception {
//         UserSignature user = new UserSignature(userEmail);
//         var environment = INDataCenter.PRODUCTION;
//         var token = new OAuthToken(clientId, clientSecret, redirectUrl, OAuthToken.TokenType.GRANT);
//         var store = new FileStore("tokens.txt");
//         var config = new SDKConfig.Builder().build();

//         Initializer.initialize(user, environment, token, store, config, "zoho_sdk_logger");

//         System.out.println("✅ Zoho SDK manually initialized.");
//     }

// } 