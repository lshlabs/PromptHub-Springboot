-- PromptHub Pre-Migration Safety Check
-- Target: Supabase PostgreSQL
-- Purpose: Validate data before running prod_migration_v1.sql
--
-- PASS/FAIL rule:
-- - fail_count = 0   -> PASS
-- - fail_count >= 1  -> FAIL (pre-cleanup required before migration)

-- =========================================================
-- 1) One-shot summary table (recommended)
-- =========================================================
WITH checks AS (
    -- [NOT NULL 대상] NULL 값 점검
    SELECT 'posts.title NULL count' AS check_name, COUNT(*)::bigint AS fail_count
    FROM posts
    WHERE title IS NULL

    UNION ALL
    SELECT 'posts.satisfaction NULL count', COUNT(*)::bigint
    FROM posts
    WHERE satisfaction IS NULL

    UNION ALL
    SELECT 'categories.name NULL count', COUNT(*)::bigint
    FROM categories
    WHERE name IS NULL

    UNION ALL
    SELECT 'ai_models.name NULL count', COUNT(*)::bigint
    FROM ai_models
    WHERE name IS NULL

    -- [UNIQUE 대상] 중복 이름 점검 (trim + lower 기준)
    UNION ALL
    SELECT 'categories.name duplicate(normalized) group count', COUNT(*)::bigint
    FROM (
        SELECT LOWER(TRIM(name)) AS normalized_name
        FROM categories
        GROUP BY LOWER(TRIM(name))
        HAVING COUNT(*) > 1
    ) dup

    UNION ALL
    SELECT 'platforms.name duplicate(normalized) group count', COUNT(*)::bigint
    FROM (
        SELECT LOWER(TRIM(name)) AS normalized_name
        FROM platforms
        GROUP BY LOWER(TRIM(name))
        HAVING COUNT(*) > 1
    ) dup

    -- [FK 안정성] 고아 참조 점검 (RESTRICT 전환 대상 5개)
    UNION ALL
    SELECT 'orphan posts.author_id -> users.id', COUNT(*)::bigint
    FROM posts p
    LEFT JOIN users u ON u.id = p.author_id
    WHERE p.author_id IS NOT NULL
      AND u.id IS NULL

    UNION ALL
    SELECT 'orphan posts.platform_id -> platforms.id', COUNT(*)::bigint
    FROM posts p
    LEFT JOIN platforms pl ON pl.id = p.platform_id
    WHERE p.platform_id IS NOT NULL
      AND pl.id IS NULL

    UNION ALL
    SELECT 'orphan posts.category_id -> categories.id', COUNT(*)::bigint
    FROM posts p
    LEFT JOIN categories c ON c.id = p.category_id
    WHERE p.category_id IS NOT NULL
      AND c.id IS NULL

    UNION ALL
    SELECT 'orphan posts.model_id -> ai_models.id', COUNT(*)::bigint
    FROM posts p
    LEFT JOIN ai_models m ON m.id = p.model_id
    WHERE p.model_id IS NOT NULL
      AND m.id IS NULL

    UNION ALL
    SELECT 'orphan ai_models.platform_id -> platforms.id', COUNT(*)::bigint
    FROM ai_models m
    LEFT JOIN platforms p ON p.id = m.platform_id
    WHERE m.platform_id IS NOT NULL
      AND p.id IS NULL
)
SELECT
    check_name,
    fail_count,
    CASE WHEN fail_count = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
FROM checks
ORDER BY
    CASE WHEN fail_count = 0 THEN 1 ELSE 0 END,
    fail_count DESC,
    check_name;

-- =========================================================
-- 2) Detail drill-down queries (run only when FAIL appears)
-- =========================================================

-- 2-1) categories.name 중복 상세
SELECT LOWER(TRIM(name)) AS normalized_name, COUNT(*) AS row_count,
       ARRAY_AGG(id ORDER BY id) AS ids
FROM categories
GROUP BY LOWER(TRIM(name))
HAVING COUNT(*) > 1
ORDER BY row_count DESC, normalized_name;

-- 2-2) platforms.name 중복 상세
SELECT LOWER(TRIM(name)) AS normalized_name, COUNT(*) AS row_count,
       ARRAY_AGG(id ORDER BY id) AS ids
FROM platforms
GROUP BY LOWER(TRIM(name))
HAVING COUNT(*) > 1
ORDER BY row_count DESC, normalized_name;

-- 2-3) 고아 참조 상세 (posts.author_id)
SELECT p.id AS post_id, p.author_id
FROM posts p
LEFT JOIN users u ON u.id = p.author_id
WHERE p.author_id IS NOT NULL
  AND u.id IS NULL
ORDER BY p.id;

-- 2-4) 고아 참조 상세 (posts.platform_id)
SELECT p.id AS post_id, p.platform_id
FROM posts p
LEFT JOIN platforms pl ON pl.id = p.platform_id
WHERE p.platform_id IS NOT NULL
  AND pl.id IS NULL
ORDER BY p.id;

-- 2-5) 고아 참조 상세 (posts.category_id)
SELECT p.id AS post_id, p.category_id
FROM posts p
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.category_id IS NOT NULL
  AND c.id IS NULL
ORDER BY p.id;

-- 2-6) 고아 참조 상세 (posts.model_id)
SELECT p.id AS post_id, p.model_id
FROM posts p
LEFT JOIN ai_models m ON m.id = p.model_id
WHERE p.model_id IS NOT NULL
  AND m.id IS NULL
ORDER BY p.id;

-- 2-7) 고아 참조 상세 (ai_models.platform_id)
SELECT m.id AS model_id, m.platform_id
FROM ai_models m
LEFT JOIN platforms p ON p.id = m.platform_id
WHERE m.platform_id IS NOT NULL
  AND p.id IS NULL
ORDER BY m.id;

