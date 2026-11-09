-- 保持 V1 不变，以便已部署数据库正常升级。
ALTER TABLE readers ADD COLUMN student_number VARCHAR(64);
-- 旧数据没有真实学号，分配唯一临时编号，保留原 UUID 与全部借阅关联。
UPDATE readers SET student_number = 'LEGACY-' || id::text, update_time = clock_timestamp()
WHERE student_number IS NULL;
ALTER TABLE readers ALTER COLUMN student_number SET NOT NULL;
ALTER TABLE readers ADD CONSTRAINT ck_reader_student_number
    CHECK (student_number = btrim(student_number) AND length(btrim(student_number)) > 0);
ALTER TABLE readers ADD CONSTRAINT uq_reader_student_number UNIQUE (student_number);
