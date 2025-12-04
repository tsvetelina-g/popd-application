# POPd – Movie Review Platform (Spring Boot MVC + Microservices)

POPd is a Spring Boot MVC-based movie platform that allows users to browse movies, submit ratings and reviews, manage watchlists, and receive AI-powered recommendations.
The application integrates with two microservices: one for ratings (MySQL) and one for reviews (MongoDB).

## Core Features

- Movie browsing & search
- Ratings (1–10 scale)
- Reviews with optional rating and title
- Watchlist & watched movies
- User profiles and activity feed
- Admin panel for content and user management
- AI-powered movie recommendations (Groq / LLaMA)
- Caching and scheduled background jobs

## Tech Stack

- Java 17
- Spring Boot 3.4.0
- Spring MVC (Thymeleaf)
- Spring Security
- Spring Data JPA / Hibernate
- Spring Cloud OpenFeign
- MySQL (main database)
- MongoDB (reviews)
- H2 (tests)
- Spring AI (Groq API)

## Architecture Overview

```
┌───────────────────────────────────────────────────────────┐
│                    Client (Browser)                       │
│              (Thymeleaf Templates - HTML/CSS/JS)          │
└───────────────────────────┬───────────────────────────────┘
                            │ HTTP/HTTPS
                            ▼
┌──────────────────────────────────────────────────────────┐
│         Spring Boot MVC Application (Port 8080)          │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐ │
│  │         Controllers (MVC)                           │ │
│  │  • MovieController  • ArtistController              │ │
│  │  • RatingController • ReviewController              │ │
│  └───────────────────────┬─────────────────────────────┘ │
│                          │                               │
│  ┌───────────────────────┴─────────────────────────────┐ │
│  │         Services (Business)                         │ │
│  │  • MovieService  • ArtistService                    │ │
│  │  • RatingService • ReviewService                    │ │
│  │  • AiRecommendationService                          │ │
│  └───────────────────────┬─────────────────────────────┘ │
│                          │                               │
│  ┌───────────────────────┴─────────────────────────────┐ │
│  │         Repositories (Data)                         │ │
│  │  • MovieRepository  • ArtistRepository              │ │
│  │  • UserRepository   • ActivityRepository            │ │
│  └───────┬──────────────────────┬──────┬───────────────┘ │
│          │                      │      │                 │
│          │                      │      │                 │
│          ▼                      │      │                 │
│  ┌──────────────┐               │      │                 │
│  │   MySQL      │               │      │                 │
│  │  Database    │               │      │                 │
│  │  (MVC App)   │               │      │                 │
│  │  • Movies    │               │      │                 │
│  │  • Users     │               │      │                 │
│  │  • Artists   │               │      │                 │
│  └──────────────┘               │      │                 │
│                                 │      │                 │
│  ┌──────────────────────────────┴┐  ┌──┴────────────┐    │
│  │    Feign Clients              │  │  Spring AI    │    │
│  │  • RatingClient               │  │  • ChatModel  │    │
│  │  • ReviewClient               │  │               │    │
│  └───────┬───────────────┬───────┘  └───────┬───────┘    │
└──────────┼───────────────┼──────────────────┼────────────┘
           │               │                  │
           │               │                  │
           ▼               ▼                  ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Rating Service  │  │  Review Service  │  │   Groq AI API    │
│   Port: 8084     │  │   Port: 8085     │  │   (External)     │
│                  │  │                  │  │                  │
│  Database: MySQL │  │ Database: MongoDB│  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘

```

## Prerequisites

- Java 17+
- MySQL running on `localhost:3306`
- MongoDB installed
- Maven or Maven Wrapper
- Rating Service running on port 8084 (recommended)
- Review Service running on port 8085 (recommended)
- Groq API Key (optional)

## Setup Instructions

### 1. Configure MySQL

In `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/popd?createDatabaseIfNotExist=true
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### 2. Configure Microservices 

**Rating Service:**
- Ensure Rating Service is running on `http://localhost:8084/api/v1`
- Feign client configured in `RatingClient.java`

**Review Service:**
- Ensure Review Service is running on `http://localhost:8085/api/v1`
- Feign client configured in `ReviewClient.java`

### 3. Configure AI Recommendations 

A Groq API key is included in `src/main/resources/application.properties` for testing purposes.

If you want to use your own API key:

1. **Sign up for a free account:**
   - Visit [Groq Cloud Console](https://console.groq.com/)
   - Click "Sign Up" and complete the registration process
   - If you already have an account, click "Log In"

2. **Create an API key:**
   - Once logged in, navigate to the [API Keys](https://console.groq.com/keys) section
   - Click "Create API Key"
   - Provide a descriptive name for your key (e.g., "POPd Application")
   - Click "Submit" or "Create" to generate the key

3. **Copy your API key:**
   - Copy the generated key immediately (you won't be able to view it again after leaving the page)
   - Store it securely

4. **Update the configuration:**
   - Open `src/main/resources/application.properties`
   - Replace `YOUR_GROQ_API_KEY` with your actual API key:

```properties
spring.ai.openai.api-key=YOUR_GROQ_API_KEY
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=llama-3.1-8b-instant
```

### 4. Start Required Services

Start services in this order:

1. MySQL
2. MongoDB
3. Rating Service (8084)
4. Review Service (8085)
5. POPd MVC application

### 5. Run the Application

Open and run:

```
src/main/java/app/popdapplication/PopdApplication.java
```

or using Maven:

```bash
mvn spring-boot:run
```

### 6. Open the Application

**http://localhost:8080**

## Testing

The project uses H2 in-memory database for tests.
No external database setup is required.

Test types:
- **Unit tests** - Service layer (`RatingServiceUTest.java`, `ReviewServiceUTest.java`, etc.)
- **API tests** - Controller layer (`RatingControllerApiTest.java`, `ReviewControllerApiTest.java`, etc.)
- **Integration tests** - End-to-end (`RegisterUserITest.java`, `GetMoviePageITest.java`, etc.)

## Default Admin Account

Check `DataInitializer.java` for default admin credentials.

## Notes

- Ratings are integers from 1 to 10
- Each user can submit only one rating and one review per movie
- Microservices are optional but required for full functionality
- AI module is optional and fails gracefully if disabled
- Database schema is auto-generated by Hibernate
- Activity feed reflects user actions (reviews, ratings, watchlist, watched)

## Summary

This project demonstrates:

- Microservices integration using Feign
- MVC architecture with Thymeleaf
- SQL + NoSQL integration
- Authentication and authorization
- AI-based recommendation integration
- Caching and scheduled jobs
- Clean architecture separation
