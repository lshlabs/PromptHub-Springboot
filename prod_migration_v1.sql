-- PromptHub Production Migration v1
-- Target: Supabase PostgreSQL
-- Purpose:
-- 1) Harden nullability/uniqueness for core domain columns
-- 2) Convert critical FKs to ON DELETE RESTRICT to protect UGC assets
-- 3) Keep interaction cleanup FKs as CASCADE (handled separately, not changed here)
--
-- Safe to run multiple times (idempotent style using IF EXISTS / conditional checks).

BEGIN;

-- =========================================================
-- 0) Optional pre-checks (uncomment to inspect before apply)
-- =========================================================
-- SELECT COUNT(*) AS posts_title_nulls FROM posts WHERE title IS NULL;
-- SELECT COUNT(*) AS posts_satisfaction_nulls FROM posts WHERE satisfaction IS NULL;
-- SELECT COUNT(*) AS categories_name_nulls FROM categories WHERE name IS NULL;
-- SELECT COUNT(*) AS ai_models_name_nulls FROM ai_models WHERE name IS NULL;
-- SELECT LOWER(TRIM(name)) AS normalized_name, COUNT(*)
-- FROM categories
-- GROUP BY LOWER(TRIM(name))
-- HAVING COUNT(*) > 1;
-- SELECT LOWER(TRIM(name)) AS normalized_name, COUNT(*)
-- FROM platforms
-- GROUP BY LOWER(TRIM(name))
-- HAVING COUNT(*) > 1;

-- =========================================================
-- 1) Core NOT NULL hardening
-- =========================================================
ALTER TABLE IF EXISTS posts
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE IF EXISTS posts
    ALTER COLUMN satisfaction SET NOT NULL;

ALTER TABLE IF EXISTS categories
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE IF EXISTS ai_models
    ALTER COLUMN name SET NOT NULL;

-- =========================================================
-- 2) Core UNIQUE hardening
--    (Requested: categories/platforms uniqueness)
-- =========================================================
-- Deduplicate first: keep original *_name_key if both exist.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'categories'::regclass
          AND conname = 'categories_name_key'
          AND contype = 'u'
    ) AND EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'categories'::regclass
          AND conname = 'uk_categories_name'
          AND contype = 'u'
    ) THEN
        ALTER TABLE categories DROP CONSTRAINT uk_categories_name;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'categories'::regclass
          AND c.contype = 'u'
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'name'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT categories_name_key UNIQUE (name);
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'platforms'::regclass
          AND conname = 'platforms_name_key'
          AND contype = 'u'
    ) AND EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'platforms'::regclass
          AND conname = 'uk_platforms_name'
          AND contype = 'u'
    ) THEN
        ALTER TABLE platforms DROP CONSTRAINT uk_platforms_name;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'platforms'::regclass
          AND c.contype = 'u'
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'name'
    ) THEN
        ALTER TABLE platforms
            ADD CONSTRAINT platforms_name_key UNIQUE (name);
    END IF;
END
$$;

-- =========================================================
-- 2.5) Referenced-key hardening for FK creation
--      Supabase/imported schemas may miss PK/UNIQUE on id.
--      Ensure each referenced table has a unique key on id.
-- =========================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'users'::regclass
          AND c.contype IN ('p','u')
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'id'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT users_id_unique UNIQUE (id);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'platforms'::regclass
          AND c.contype IN ('p','u')
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'id'
    ) THEN
        ALTER TABLE platforms
            ADD CONSTRAINT platforms_id_unique UNIQUE (id);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'categories'::regclass
          AND c.contype IN ('p','u')
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'id'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT categories_id_unique UNIQUE (id);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_attribute a
          ON a.attrelid = c.conrelid
         AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = 'ai_models'::regclass
          AND c.contype IN ('p','u')
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'id'
    ) THEN
        ALTER TABLE ai_models
            ADD CONSTRAINT ai_models_id_unique UNIQUE (id);
    END IF;
END
$$;

-- =========================================================
-- 3) FK policy conversion: critical 5 FKs -> RESTRICT
--    운영 정책 명분: UGC(게시글) 자산 보호 최우선
-- =========================================================

-- 3-1) posts.author_id -> users.id (RESTRICT)
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'posts'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = 'posts'::regclass AND attname = 'author_id' AND NOT attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE posts DROP CONSTRAINT %I', r.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_posts_author_restrict'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT fk_posts_author_restrict
            FOREIGN KEY (author_id) REFERENCES users(id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

-- 3-2) posts.platform_id -> platforms.id (RESTRICT)
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'posts'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = 'posts'::regclass AND attname = 'platform_id' AND NOT attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE posts DROP CONSTRAINT %I', r.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_posts_platform_restrict'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT fk_posts_platform_restrict
            FOREIGN KEY (platform_id) REFERENCES platforms(id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

-- 3-3) posts.category_id -> categories.id (RESTRICT)
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'posts'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = 'posts'::regclass AND attname = 'category_id' AND NOT attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE posts DROP CONSTRAINT %I', r.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_posts_category_restrict'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT fk_posts_category_restrict
            FOREIGN KEY (category_id) REFERENCES categories(id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

-- 3-4) posts.model_id -> ai_models.id (RESTRICT)
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'posts'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = 'posts'::regclass AND attname = 'model_id' AND NOT attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE posts DROP CONSTRAINT %I', r.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_posts_model_restrict'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT fk_posts_model_restrict
            FOREIGN KEY (model_id) REFERENCES ai_models(id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

-- 3-5) ai_models.platform_id -> platforms.id (RESTRICT)
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'ai_models'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = 'ai_models'::regclass AND attname = 'platform_id' AND NOT attisdropped)
          ]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE ai_models DROP CONSTRAINT %I', r.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_ai_models_platform_restrict'
          AND conrelid = 'ai_models'::regclass
    ) THEN
        ALTER TABLE ai_models
            ADD CONSTRAINT fk_ai_models_platform_restrict
            FOREIGN KEY (platform_id) REFERENCES platforms(id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

COMMIT;

-- =========================================================
-- 4) Post-check queries (run after commit)
-- =========================================================
-- SELECT conname, conrelid::regclass AS table_name, pg_get_constraintdef(oid) AS definition
-- FROM pg_constraint
-- WHERE conname IN (
--   'categories_name_key',
--   'platforms_name_key',
--   'fk_posts_author_restrict',
--   'fk_posts_platform_restrict',
--   'fk_posts_category_restrict',
--   'fk_posts_model_restrict',
--   'fk_ai_models_platform_restrict'
-- )
-- ORDER BY conname;
