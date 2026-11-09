INSERT INTO readers(id,name,student_number)
VALUES ('22222222-2222-4222-8222-222222222222','示例读者','20260001')
ON CONFLICT DO NOTHING;
INSERT INTO books(id,title,author,total_copies,available_copies)
VALUES ('11111111-1111-4111-8111-111111111111','数据库系统概念','Abraham Silberschatz 等',3,3)
ON CONFLICT DO NOTHING;
