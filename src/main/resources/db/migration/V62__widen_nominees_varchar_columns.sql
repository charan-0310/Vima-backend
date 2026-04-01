-- Fix: staging nominees table has varchar(10) columns that truncate relationship/gender values
-- (e.g. "FATHER-IN-LAW", "MOTHER-IN-LAW"). Ensure all text columns match intended baseline sizes.
ALTER TABLE cpc.nominees ALTER COLUMN gender TYPE varchar(255);
ALTER TABLE cpc.nominees ALTER COLUMN relationship TYPE varchar(255);
