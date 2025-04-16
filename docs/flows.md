# Typical Flows

This document describes some common scenarios in the Vocabulary Service.

---

## 1. Teacher Creates a New Word

1. Teacher opens the UI and fills out a "New Word" form with `text` = "example", `translation` = "пример".
2. Frontend calls `POST /api/v1/vocabulary/words`.
3. Vocabulary Service:
    1. Validates the request.
    2. Inserts into `vocabulary_word`.
    3. Returns the newly created entity with ID.

---

## 2. Teacher Assigns Words to a Student

1. Teacher selects student "John Doe" (ID=123) and chooses multiple words `[1001, 1002]`.
2. Frontend calls `POST /api/v1/vocabulary/assignments` with `studentId=123`, `vocabularyWordIds=[1001,1002]`.
3. Vocabulary Service:
    1. Validates the student (by calling Users Service or verifying locally).
    2. Inserts records into `assigned_word`:
        - (student_id=123, vocabulary_word_id=1001, status='IN_PROGRESS')
        - (student_id=123, vocabulary_word_id=1002, status='IN_PROGRESS')
    3. Returns the list of created assignments.

---

## 3. Student Checks Assigned Words

1. Student logs in and requests their assigned words.
2. Frontend calls `GET /api/v1/vocabulary/students/123/assignments`.
3. Vocabulary Service queries `assigned_word` joined with `vocabulary_word`.
4. Returns JSON with each word's text, translation, status, progress, etc.

---

## 4. Teacher Marks a Word as Learned

1. Teacher verifies the student's progress and decides "Word #1001 is learned."
2. Frontend calls `PATCH /api/v1/vocabulary/assignments/501/status` with `{"status":"LEARNED"}`.
3. Vocabulary Service updates the record in `assigned_word`.
4. Returns the updated assignment.

---

## 5. (Optional) Notification Flow

If integrated with a Notification Service:
1. After marking a word as learned, the Vocabulary Service publishes an event (e.g., `WORD_LEARNED`) to a message broker.
2. The Notification Service listens, receives the event, and sends an email or push notification: "Congratulations on learning the word 'example'!"