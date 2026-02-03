CREATE TABLE public.person (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    age INTEGER,
    city VARCHAR(255)
);