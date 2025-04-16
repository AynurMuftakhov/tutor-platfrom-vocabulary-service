# Architecture Overview

## Purpose

The Vocabulary Service handles all operations related to English vocabulary words, maintaining a clear boundary between other domain contexts such as Lessons or Users.

## Domain Model

- **Teacher**: creates words and assigns them to students.
- **Student**: receives assigned words and tracks progress.
- **VocabularyWord**: A single word entity with text and translation fields, optionally including part of speech, example usage, etc.
- **AssignedWord**: A link between a Student and a VocabularyWord, capturing the student's progress and status.

## Microservice Boundaries

- **Inputs**:
    - Requests from a Teacher to add/edit words.
    - Requests to assign words to a Student.
    - Queries for student's assigned words.
- **Outputs**:
    - JSON responses about words, assigned words, statuses.
    - Potential events or notifications to other services (e.g., if a student completes learning).

## Integration with Other Services

1. **Users Service**:
    - Retrieves information about Teachers and Students (e.g., IDs, names).
    - Ensures that a valid Teacher/Student exists before assigning words.

2. **Lessons Service** 
    - May fetch or update word statuses during lessons.
    - Could trigger the assignment of new words based on lesson outcomes.

3. **Notifications Service**:
    - Could be used to notify students about new words or remind them to practice.

## Technology Stack

- **Spring Boot** for REST controllers, dependency injection, etc.
- **Spring Data JPA** with PostgreSQL for persistence.
- **Possibility** of integrating Kafka for asynchronous messaging