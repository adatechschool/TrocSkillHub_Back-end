-- V13__rename_users_id_to_user_id.sql
-- Align the foreign key column name on the user_id convention already used by
-- user_knowledge and password_reset_request.

ALTER TABLE education  RENAME COLUMN users_id TO user_id;
ALTER TABLE experience RENAME COLUMN users_id TO user_id;
ALTER TABLE project    RENAME COLUMN users_id TO user_id;
