-- PromptHub Post-Migration Schema Check
-- Target: Supabase PostgreSQL
-- Purpose: Verify constraints from prod_migration_v1.sql are actually registered
--
-- PASS rule: all checks = PASS
-- FAIL rule: any check = FAIL

WITH checks AS (
    -- =====================================================
    -- 1) NOT NULL checks (4 columns)
    -- =====================================================
    SELECT
        'NOT NULL: posts.title' AS check_name,
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'posts'
                  AND column_name = 'title'
                  AND is_nullable = 'NO'
            ) THEN 'PASS' ELSE 'FAIL'
        END AS verdict

    UNION ALL
    SELECT
        'NOT NULL: posts.satisfaction',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'posts'
                  AND column_name = 'satisfaction'
                  AND is_nullable = 'NO'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'NOT NULL: categories.name',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'categories'
                  AND column_name = 'name'
                  AND is_nullable = 'NO'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'NOT NULL: ai_models.name',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'ai_models'
                  AND column_name = 'name'
                  AND is_nullable = 'NO'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    -- =====================================================
    -- 2) UNIQUE checks (2 constraints)
    -- =====================================================
    UNION ALL
    SELECT
        'UNIQUE: categories.name',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = 'public'
                  AND t.relname = 'categories'
                  AND c.contype = 'u'
                  AND pg_get_constraintdef(c.oid) ILIKE 'UNIQUE (name)%'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'UNIQUE: platforms.name',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = 'public'
                  AND t.relname = 'platforms'
                  AND c.contype = 'u'
                  AND pg_get_constraintdef(c.oid) ILIKE 'UNIQUE (name)%'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    -- =====================================================
    -- 3) FK delete_rule checks (5 RESTRICT targets)
    --    information_schema maps RESTRICT as 'RESTRICT'
    -- =====================================================
    UNION ALL
    SELECT
        'FK RESTRICT: posts.author_id -> users.id',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = rc.constraint_name
                 AND kcu.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = rc.unique_constraint_name
                 AND ccu.constraint_schema = rc.unique_constraint_schema
                WHERE rc.constraint_schema = 'public'
                  AND kcu.table_name = 'posts'
                  AND kcu.column_name = 'author_id'
                  AND ccu.table_name = 'users'
                  AND ccu.column_name = 'id'
                  AND rc.delete_rule = 'RESTRICT'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'FK RESTRICT: posts.platform_id -> platforms.id',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = rc.constraint_name
                 AND kcu.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = rc.unique_constraint_name
                 AND ccu.constraint_schema = rc.unique_constraint_schema
                WHERE rc.constraint_schema = 'public'
                  AND kcu.table_name = 'posts'
                  AND kcu.column_name = 'platform_id'
                  AND ccu.table_name = 'platforms'
                  AND ccu.column_name = 'id'
                  AND rc.delete_rule = 'RESTRICT'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'FK RESTRICT: posts.category_id -> categories.id',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = rc.constraint_name
                 AND kcu.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = rc.unique_constraint_name
                 AND ccu.constraint_schema = rc.unique_constraint_schema
                WHERE rc.constraint_schema = 'public'
                  AND kcu.table_name = 'posts'
                  AND kcu.column_name = 'category_id'
                  AND ccu.table_name = 'categories'
                  AND ccu.column_name = 'id'
                  AND rc.delete_rule = 'RESTRICT'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'FK RESTRICT: posts.model_id -> ai_models.id',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = rc.constraint_name
                 AND kcu.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = rc.unique_constraint_name
                 AND ccu.constraint_schema = rc.unique_constraint_schema
                WHERE rc.constraint_schema = 'public'
                  AND kcu.table_name = 'posts'
                  AND kcu.column_name = 'model_id'
                  AND ccu.table_name = 'ai_models'
                  AND ccu.column_name = 'id'
                  AND rc.delete_rule = 'RESTRICT'
            ) THEN 'PASS' ELSE 'FAIL'
        END

    UNION ALL
    SELECT
        'FK RESTRICT: ai_models.platform_id -> platforms.id',
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = rc.constraint_name
                 AND kcu.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = rc.unique_constraint_name
                 AND ccu.constraint_schema = rc.unique_constraint_schema
                WHERE rc.constraint_schema = 'public'
                  AND kcu.table_name = 'ai_models'
                  AND kcu.column_name = 'platform_id'
                  AND ccu.table_name = 'platforms'
                  AND ccu.column_name = 'id'
                  AND rc.delete_rule = 'RESTRICT'
            ) THEN 'PASS' ELSE 'FAIL'
        END
)
SELECT check_name, verdict
FROM checks
ORDER BY
    CASE WHEN verdict = 'FAIL' THEN 0 ELSE 1 END,
    check_name;

-- Overall gate (one row): PASS only if every row passes
WITH checks AS (
    SELECT
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'posts'
                  AND column_name = 'title'
                  AND is_nullable = 'NO'
            ) THEN 1 ELSE 0
        END AS ok
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='posts' AND column_name='satisfaction' AND is_nullable='NO'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='categories' AND column_name='name' AND is_nullable='NO'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='ai_models' AND column_name='name' AND is_nullable='NO'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname='public' AND t.relname='categories' AND c.contype='u'
          AND pg_get_constraintdef(c.oid) ILIKE 'UNIQUE (name)%'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname='public' AND t.relname='platforms' AND c.contype='u'
          AND pg_get_constraintdef(c.oid) ILIKE 'UNIQUE (name)%'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = rc.constraint_name
         AND kcu.constraint_schema = rc.constraint_schema
        JOIN information_schema.constraint_column_usage ccu
          ON ccu.constraint_name = rc.unique_constraint_name
         AND ccu.constraint_schema = rc.unique_constraint_schema
        WHERE rc.constraint_schema='public'
          AND kcu.table_name='posts' AND kcu.column_name='author_id'
          AND ccu.table_name='users' AND ccu.column_name='id'
          AND rc.delete_rule='RESTRICT'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = rc.constraint_name
         AND kcu.constraint_schema = rc.constraint_schema
        JOIN information_schema.constraint_column_usage ccu
          ON ccu.constraint_name = rc.unique_constraint_name
         AND ccu.constraint_schema = rc.unique_constraint_schema
        WHERE rc.constraint_schema='public'
          AND kcu.table_name='posts' AND kcu.column_name='platform_id'
          AND ccu.table_name='platforms' AND ccu.column_name='id'
          AND rc.delete_rule='RESTRICT'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = rc.constraint_name
         AND kcu.constraint_schema = rc.constraint_schema
        JOIN information_schema.constraint_column_usage ccu
          ON ccu.constraint_name = rc.unique_constraint_name
         AND ccu.constraint_schema = rc.unique_constraint_schema
        WHERE rc.constraint_schema='public'
          AND kcu.table_name='posts' AND kcu.column_name='category_id'
          AND ccu.table_name='categories' AND ccu.column_name='id'
          AND rc.delete_rule='RESTRICT'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = rc.constraint_name
         AND kcu.constraint_schema = rc.constraint_schema
        JOIN information_schema.constraint_column_usage ccu
          ON ccu.constraint_name = rc.unique_constraint_name
         AND ccu.constraint_schema = rc.unique_constraint_schema
        WHERE rc.constraint_schema='public'
          AND kcu.table_name='posts' AND kcu.column_name='model_id'
          AND ccu.table_name='ai_models' AND ccu.column_name='id'
          AND rc.delete_rule='RESTRICT'
    ) THEN 1 ELSE 0 END
    UNION ALL
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints rc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = rc.constraint_name
         AND kcu.constraint_schema = rc.constraint_schema
        JOIN information_schema.constraint_column_usage ccu
          ON ccu.constraint_name = rc.unique_constraint_name
         AND ccu.constraint_schema = rc.unique_constraint_schema
        WHERE rc.constraint_schema='public'
          AND kcu.table_name='ai_models' AND kcu.column_name='platform_id'
          AND ccu.table_name='platforms' AND ccu.column_name='id'
          AND rc.delete_rule='RESTRICT'
    ) THEN 1 ELSE 0 END
)
SELECT
    CASE
        WHEN MIN(ok) = 1 THEN 'PASS'
        ELSE 'FAIL'
    END AS overall_verdict,
    SUM(CASE WHEN ok = 1 THEN 1 ELSE 0 END) AS pass_count,
    SUM(CASE WHEN ok = 0 THEN 1 ELSE 0 END) AS fail_count,
    COUNT(*) AS total_checks
FROM checks;
