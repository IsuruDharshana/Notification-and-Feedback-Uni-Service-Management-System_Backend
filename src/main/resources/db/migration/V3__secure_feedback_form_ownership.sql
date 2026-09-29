ALTER TABLE feedback_forms
    ADD COLUMN created_by CHAR(36) NULL;

UPDATE feedback_forms
SET created_by = '00000000-0000-0000-0000-000000000000'
WHERE created_by IS NULL;

ALTER TABLE feedback_forms
    MODIFY COLUMN created_by CHAR(36) NOT NULL;
