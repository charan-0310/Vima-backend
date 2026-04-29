ALTER TABLE cpc.organizations
ADD COLUMN IF NOT EXISTS organization_displayname VARCHAR(255);

UPDATE cpc.organizations
SET organization_displayname = organization_name
WHERE organization_displayname IS NULL OR btrim(organization_displayname) = '';

ALTER TABLE cpc.organizations
ALTER COLUMN organization_displayname SET NOT NULL;

ALTER TABLE cpc.organizations
ADD CONSTRAINT chk_organizations_displayname_not_blank
CHECK (btrim(organization_displayname) <> '');
