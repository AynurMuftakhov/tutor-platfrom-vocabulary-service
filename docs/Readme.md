# Vocabulary Service

The **Vocabulary Service** is a standalone microservice responsible for managing English vocabulary words, assigning them to students, and tracking their learning progress. It is designed to integrate seamlessly with other services in the tutor platform

## Key Features

1. **Vocabulary Management**
    - Create, update, and delete words in the global vocabulary.
    - Handle translations and optional metadata (e.g., part of speech, example usage).

2. **Assignment & Progress**
    - Assign words to specific students (one-to-many).
    - Track each student's progress (in progress, learned).
    - Allow teachers to review and update statuses.

3. **Search & Filtering**
    - Basic search by word text, translation, etc.
    - Filter words by student, status, or teacher.

## Technologies

- **Language**: Java 21
- **Framework**: Spring Boot
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Containerization**: Docker
- **Communication**: REST