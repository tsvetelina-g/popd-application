# POPd Application

A Spring Boot-based movie review and rating platform that allows users to discover, rate, review, and track movies.

## Features

POPd lets you:
- **Discover & Search Movies** - Browse and search through a comprehensive movie database
- **Rate & Review Films** - Share your opinions by rating movies (1-10) and writing detailed reviews
- **Build Your Watchlist** - Create and manage personalized watchlists to track movies you want to watch
- **Track Watched Movies** - Keep a record of movies you've already watched
- **Explore Artist Profiles** - View detailed information about actors, directors, and other artists

### Additional Features
- User authentication and authorization (USER/ADMIN roles)
- Movie browsing by genre with newest releases
- Artist and movie credit management
- User profiles with activity tracking
- Admin panel for managing users, movies, and artists
- Activity feed tracking user interactions
- Caching for improved performance

## Tech Stack

- **Framework**: Spring Boot 3.4.0
- **Java Version**: 17
- **Database**: MySQL 8 (production), H2 (testing)
- **Template Engine**: Thymeleaf
- **Security**: Spring Security
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Microservices**: Spring Cloud OpenFeign (for Rating and Review microservices)
- **Caching**: Spring Cache
- **Validation**: Jakarta Validation

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+ (for production)
- MySQL server running on `localhost:3306`

## Installation & Setup

1. **Open the project in IntelliJ IDEA**
   - Open IntelliJ IDEA
   - Select `File` → `Open`
   - Navigate to the project directory and select it
   - IntelliJ will automatically detect it as a Maven project and import dependencies

2. **Configure the database**
   
   Update `src/main/resources/application.properties` with your MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/popd?createDatabaseIfNotExist=true
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Wait for Maven to sync**
   - IntelliJ will automatically download dependencies
   - Wait for the Maven sync to complete (check the bottom-right status bar)

## Running the Application

### Using IntelliJ IDEA

1. **Locate the main class**
   - Navigate to `src/main/java/app/popdapplication/PopdApplication.java`

2. **Run the application**
   - Right-click on `PopdApplication.java`
   - Select `Run 'PopdApplication'`
   - Or use the green play button next to the main method
   - Or press `Shift + F10`

3. **Access the application**
   - The application will be available at `http://localhost:8080`
   - Check the IntelliJ console for startup logs and any errors

## Configuration

### Application Properties

Key configuration options in `application.properties`:

- **Database**: MySQL connection settings
- **JPA**: Hibernate DDL auto-update enabled
- **Server**: Runs on all interfaces (0.0.0.0)
- **Caching**: Enabled for top movies
- **Scheduling**: Enabled for background jobs

### External Microservices

The application integrates with external microservices for:
- **Rating Service**: Handles movie ratings
- **Review Service**: Manages movie reviews

Ensure these services are running and accessible if using the full microservices architecture.

## Testing

### Running Tests in IntelliJ IDEA

1. **Run all tests**
   - Right-click on the `src/test/java` folder
   - Select `Run 'All Tests'`
   - Or use `Ctrl + Shift + F10` (Windows/Linux) or `Cmd + Shift + R` (Mac)

2. **Run a specific test class**
   - Open the test file (e.g., `UserServiceUTest.java`)
   - Right-click on the class name or the file
   - Select `Run 'UserServiceUTest'`
   - Or click the green play button next to the class declaration

3. **Run a specific test method**
   - Click the green play button next to the test method
   - Or right-click on the method and select `Run 'methodName()'`

4. **View test results**
   - Test results appear in the `Run` tool window at the bottom
   - Green checkmarks indicate passed tests
   - Red X marks indicate failed tests with error details

The test suite includes:
- Unit tests for services
- Integration tests for controllers
- End-to-end tests for user flows

Test database uses H2 in-memory database (configured in `src/test/resources/application.properties`).

## Project Structure

```
src/
├── main/
│   ├── java/app/popdapplication/
│   │   ├── client/          # Feign clients for microservices
│   │   ├── config/          # Configuration classes
│   │   ├── event/           # Event handling
│   │   ├── exception/       # Custom exceptions
│   │   ├── initialization/  # Data initialization
│   │   ├── job/             # Scheduled jobs
│   │   ├── model/           # Entity models and enums
│   │   ├── repository/      # JPA repositories
│   │   ├── security/        # Security configuration
│   │   ├── service/         # Business logic services
│   │   └── web/             # Controllers and DTOs
│   └── resources/
│       ├── templates/       # Thymeleaf templates
│       ├── static/css/      # CSS stylesheets
│       └── application.properties
└── test/
    └── java/                # Test classes
```

## API Endpoints

### Public Endpoints
- `GET /` - Home page
- `GET /movies` - Browse movies by genre
- `GET /movies/{id}` - Movie details
- `GET /find` - Search movies and artists
- `GET /login` - Login page
- `GET /register` - Registration page

### Authenticated Endpoints
- `GET /profile` - User profile
- `POST /rating/{movieId}/add` - Add rating
- `POST /review/{movieId}` - Submit review
- `GET /user/watchlist` - User watchlist
- `GET /user/watched` - Watched movies

### Admin Endpoints
- `GET /admin` - Admin panel
- `GET /admin/users` - User management
- `POST /movies/add` - Add movie
- `POST /artist/add` - Add artist

## Default Admin Account

On first startup, the application initializes with default data including an admin user. Check `DataInitializer` for default credentials.


