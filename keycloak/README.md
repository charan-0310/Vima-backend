# Keycloak Organization IDs Protocol Mapper

Custom Keycloak 26.x Protocol Mapper (Quarkus distribution) that collects the `organization_id` attribute from all user groups and adds them as a multi-valued claim (default: `organization_ids`) to the Access Token and optionally the ID Token.

- **Keycloak:** 26.x (Quarkus)
- **Java:** 21
- **Build:** Maven

## JWT output example

```json
{
  "organization_ids": ["ORG100", "ORG200"]
}
```

## Requirements

- Each group can have an attribute named `organization_id`.
- The mapper collects **all** `organization_id` values from the user's groups, deduplicates them, and adds them to the token.

## Build

### Prerequisites

- JDK 21
- Maven 3.9+

### Build JAR

```bash
cd keycloak
mvn clean package -DskipTests
```

The JAR is produced at: `target/keycloak-organization-ids-mapper-1.0.0.jar`

If `keycloak-services` is not found for your Keycloak version, align the `keycloak.version` in `pom.xml` with your server (e.g. `26.0.7` or `26.2.x`).

## Deploy into Keycloak 26 (Quarkus)

### Option 1: Copy into running container `/opt/keycloak/providers/`

1. Build the JAR (see above).
2. Copy the JAR into the Keycloak providers directory:

   **Docker:**
   ```bash
   docker cp target/keycloak-organization-ids-mapper-1.0.0.jar keycloak:/opt/keycloak/providers/
   docker restart keycloak
   ```

   **Podman / Kubernetes:**
   ```bash
   kubectl cp target/keycloak-organization-ids-mapper-1.0.0.jar <namespace>/<keycloak-pod>:/opt/keycloak/providers/
   kubectl delete pod -l app=keycloak -n <namespace>
   ```

3. If Keycloak is configured to do a **server rebuild** before start, run:

   ```bash
   /opt/keycloak/bin/kc.sh build
   ```

   Then (re)start the server. Otherwise a restart may be enough for Quarkus to load the new provider.

### Option 2: Custom Docker image

Use a Dockerfile that adds the JAR into `/opt/keycloak/providers/`:

```dockerfile
FROM quay.io/keycloak/keycloak:26
COPY target/keycloak-organization-ids-mapper-1.0.0.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]
```

Build and run:

```bash
mvn clean package -DskipTests
docker build -t keycloak-with-org-ids-mapper .
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin keycloak-with-org-ids-mapper start-dev
```

### Option 3: Bake into existing Keycloak image

If you already have a custom Keycloak image:

1. Add the JAR to your image at `/opt/keycloak/providers/`.
2. Run `kc.sh build` in the image if your setup uses it, then start as usual.

## Configure in Keycloak Admin Console

1. Log in to **Keycloak Admin Console**.
2. Select your **realm** (e.g. `vima-dev`).
3. Go to **Clients** → select the client (e.g. `vima-webapp`) → **Client scopes** → choose the scope used for the client (e.g. `vima-webapp-dedicated`), or use **Realm default** client scope.
4. Open **Add mapper** → **By configuration**.
5. Select **Organization IDs from Groups**.
6. Configure:
   - **Token claim name:** e.g. `organization_ids` (default).
   - **Add to ID token:** On/Off (optional).
   - **Add to access token:** On (recommended).
7. Save.

## Group setup

Ensure each group that represents an organization has the attribute `organization_id`:

1. **Groups** → select or create a group.
2. **Attributes** tab → add:
   - **Key:** `organization_id`
   - **Value:** e.g. `ORG100` or a UUID.
3. Save.

Users in that group will get that value in the `organization_ids` claim (when the mapper is assigned to the client scope used for their login).

## Technical details

- Extends `AbstractOIDCProtocolMapper`, implements `OIDCAccessTokenMapper` and `OIDCIDTokenMapper`.
- Reads groups via `user.getGroupsStream()` and `group.getFirstAttribute("organization_id")`.
- Deduplicates with a `Set`; output claim is a JSON array of strings.
- Claim name is configurable in the UI (default: `organization_ids`).
- Access token and ID token inclusion can be toggled in the mapper config.
- Null-safe: skips null user, null groups stream, null group, and null/blank attribute values.

## License

Apache 2.0 (or your project license).
