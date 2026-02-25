/*
 * Keycloak 26 Custom Protocol Mapper: Organization IDs from Group Attributes
 *
 * Collects organization_id attribute from all user groups and adds them as a
 * multi-valued claim (e.g. organization_ids) to the Access Token and optionally ID Token.
 *
 * Compatible with Keycloak 26.x (Quarkus distribution), Java 21.
 */

package com.vimainsurance.keycloak.mapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * Protocol mapper that adds a multi-valued claim (default: "organization_ids") to tokens
 * by collecting the "organization_id" attribute from every group the user belongs to.
 * Values are deduplicated. Safe for nulls and missing attributes.
 */
public class OrganizationIdsProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    public static final String PROVIDER_ID = "oidc-organization-ids-mapper";

    /** Config key for the claim name in the token (default: organization_ids). */
    public static final String CLAIM_NAME_CONFIG = "claim.name";
    /** Default claim name in JWT. */
    public static final String DEFAULT_CLAIM_NAME = "organization_ids";
    /** Group attribute name to read (organization_id). */
    public static final String GROUP_ATTR_ORGANIZATION_ID = "organization_id";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        // Configurable claim name (default: organization_ids)
        ProviderConfigProperty claimName = new ProviderConfigProperty();
        claimName.setName(CLAIM_NAME_CONFIG);
        claimName.setLabel("Token claim name");
        claimName.setType(ProviderConfigProperty.STRING_TYPE);
        claimName.setHelpText("Name of the claim added to the token. Default: organization_ids");
        claimName.setDefaultValue(DEFAULT_CLAIM_NAME);
        CONFIG_PROPERTIES.add(claimName);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES, OrganizationIdsProtocolMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Organization IDs from Groups";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Collects the 'organization_id' attribute from all user groups and adds them as a multi-valued claim (e.g. organization_ids).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    /**
     * Adds the organization_ids claim to the token. Called by the base class for both
     * Access Token and ID Token when the mapper is enabled for the respective token type.
     */
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        if (token == null || mappingModel == null || userSession == null) {
            return;
        }

        UserModel user = userSession.getUser();
        if (user == null) {
            return;
        }

        String claimName = getClaimName(mappingModel);
        List<String> organizationIds = collectOrganizationIdsFromGroups(user);

        // Always set claim (multi-valued): empty array when no org IDs, so downstream can rely on claim presence
        token.getOtherClaims().put(claimName, organizationIds);
    }

    /**
     * Reads organization_id from each group the user belongs to and returns a deduplicated list.
     * Uses Set for uniqueness; LinkedHashSet preserves insertion order. Null-safe.
     */
    private List<String> collectOrganizationIdsFromGroups(UserModel user) {
        Set<String> ids = new LinkedHashSet<>();
        Stream<GroupModel> groupStream = user != null ? user.getGroupsStream() : null;
        if (groupStream == null) {
            return new ArrayList<>();
        }

        groupStream
                .filter(group -> group != null)
                .map(group -> group.getFirstAttribute(GROUP_ATTR_ORGANIZATION_ID))
                .filter(value -> value != null && !((String) value).isBlank())
                .map(value -> ((String) value).trim())
                .forEach(ids::add);

        return new ArrayList<>(ids);
    }

    /**
     * Returns the claim name from mapper config, or the default if not set.
     */
    private static String getClaimName(ProtocolMapperModel mappingModel) {
        if (mappingModel == null || mappingModel.getConfig() == null) {
            return DEFAULT_CLAIM_NAME;
        }
        String name = mappingModel.getConfig().get(CLAIM_NAME_CONFIG);
        return (name != null && !name.isBlank()) ? name.trim() : DEFAULT_CLAIM_NAME;
    }

    /**
     * Create a pre-configured mapper model for programmatic use (e.g. realm import).
     */
    public static ProtocolMapperModel create(String name, String tokenClaimName,
            boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(new java.util.HashMap<>());
        mapper.getConfig().put(CLAIM_NAME_CONFIG, tokenClaimName != null ? tokenClaimName : DEFAULT_CLAIM_NAME);
        if (accessToken) {
            mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        }
        if (idToken) {
            mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        }
        return mapper;
    }
}
