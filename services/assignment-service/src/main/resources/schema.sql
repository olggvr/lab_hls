CREATE TABLE IF NOT EXISTS assignments
(
    id              BIGSERIAL PRIMARY KEY,
    teacher_id      BIGINT       NOT NULL,
    teacher_user_id BIGINT       NOT NULL,
    teacher_name    VARCHAR(150) NOT NULL,
    class_id        BIGINT       NOT NULL,
    class_name      VARCHAR(20)  NOT NULL,
    subject_id      BIGINT       NOT NULL,
    subject_name    VARCHAR(100) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    due_date        DATE         NOT NULL,
    created_at      TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_assignments_teacher ON assignments (teacher_id);
CREATE INDEX IF NOT EXISTS idx_assignments_class ON assignments (class_id);

CREATE TABLE IF NOT EXISTS submissions
(
    id              BIGSERIAL PRIMARY KEY,
    assignment_id   BIGINT       NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    student_id      BIGINT       NOT NULL,
    student_user_id BIGINT       NOT NULL,
    student_name    VARCHAR(150) NOT NULL,
    content_url     VARCHAR(500) NOT NULL,
    submitted_at    TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('SUBMITTED')),
    CONSTRAINT uq_submission UNIQUE (assignment_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_submissions_student ON submissions (student_id);

INSERT INTO assignments
    (teacher_id, teacher_user_id, teacher_name, class_id, class_name, subject_id, subject_name,
     title, description, due_date, created_at)
SELECT 1, 2, 'Иванова Мария Петровна', 1, '7А', 1, 'Математика',
       'Линейные уравнения', 'Учебник, §12, задачи 1–10', CURRENT_DATE + 7, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE id = 1);

INSERT INTO assignments
    (teacher_id, teacher_user_id, teacher_name, class_id, class_name, subject_id, subject_name,
     title, description, due_date, created_at)
SELECT 2, 3, 'Петров Сергей Иванович', 1, '7А', 2, 'Русский язык',
       'Сочинение «Как я провёл лето»', 'Объём — не менее 1 страницы', CURRENT_DATE + 5, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE id = 2);

INSERT INTO submissions
    (assignment_id, student_id, student_user_id, student_name, content_url, submitted_at, status)
SELECT 1, 1, 4, 'Смирнов Алексей', 'https://files.school.local/solutions/smirnov-linear.pdf',
       CURRENT_TIMESTAMP, 'SUBMITTED'
WHERE NOT EXISTS (SELECT 1 FROM submissions WHERE assignment_id = 1 AND student_id = 1);
