-- =====================================================================
-- Version 2: intentional denormalization.
-- Adds a cached like counter to posts so the feed can show "12 likes"
-- without counting post_likes rows for every post on every page load.
-- The true source of the number is still the post_likes table; the app
-- updates like_count in the same transaction as each like or unlike
-- (see PostLikeService). Backfill from post_likes for any existing rows.
-- =====================================================================
ALTER TABLE posts ADD COLUMN like_count INT NOT NULL DEFAULT 0;

ALTER TABLE posts ADD CONSTRAINT ck_posts_like_count CHECK (like_count >= 0);

UPDATE posts p
SET like_count = (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id);
