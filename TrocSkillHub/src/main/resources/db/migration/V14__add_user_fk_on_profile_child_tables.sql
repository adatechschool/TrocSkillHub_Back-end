-- V14__add_user_fk_on_profile_child_tables.sql
-- V10 created these columns without any constraint: rows could reference a
-- missing user, and deleting a user in raw SQL left orphans behind.

-- Orphans are unreachable from the API (these entities are only loaded through
-- the User collections), so they are safe to drop before enforcing the FK.
DELETE FROM education  WHERE user_id IS NULL OR user_id NOT IN (SELECT id FROM "users");
DELETE FROM experience WHERE user_id IS NULL OR user_id NOT IN (SELECT id FROM "users");
DELETE FROM project    WHERE user_id IS NULL OR user_id NOT IN (SELECT id FROM "users");

ALTER TABLE education  ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE experience ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE project    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE education
    ADD CONSTRAINT fk_education_user
    FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE CASCADE;

ALTER TABLE experience
    ADD CONSTRAINT fk_experience_user
    FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE CASCADE;

ALTER TABLE project
    ADD CONSTRAINT fk_project_user
    FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE CASCADE;

-- PostgreSQL does not index the referencing side of a foreign key: without
-- these, each cascading user deletion scans the whole child table.
CREATE INDEX idx_education_user_id  ON education(user_id);
CREATE INDEX idx_experience_user_id ON experience(user_id);
CREATE INDEX idx_project_user_id    ON project(user_id);
