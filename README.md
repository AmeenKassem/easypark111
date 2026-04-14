# EasyPark - Smart Parking Management Platform

<div align="center">
  <h1>EasyPark</h1>
  <p>A full-stack smart parking platform for drivers and parking owners</p>
</div>

![CI Status](https://github.com/AmeenKassem/EasyPark/actions/workflows/ci.yml/badge.svg)

EasyPark is a full-stack parking management system developed as part of the Software Engineering Seminar at Ben-Gurion University.

The platform helps **drivers** search for and book parking spots, while allowing **owners** to create, manage, and monitor their parking listings through a simple and user-friendly interface.

---

## 🌟 Project Overview

EasyPark is designed to simplify urban parking by connecting two main user types:

- **Drivers** who want to search for available parking and make bookings
- **Owners** who want to publish and manage parking spots they control

The system includes:

- User registration and login
- JWT-based authentication
- Google login support
- Password reset by email
- Support for `DRIVER`, `OWNER`, and `BOTH` roles
- Parking spot creation, editing, and deletion
- Parking search and filtering
- Booking and reservation flow
- Payments support
- Ratings support
- Reports and dashboard-related functionality
- Automated tests and CI

---

## 🏗️ Architecture

EasyPark is built as a client-server web application:

- **Frontend**: React + Vite
- **Backend**: Spring Boot REST API
- **Database**: MySQL
- **Authentication**: JWT + Google OAuth support
- **Mail**: Gmail SMTP for password reset emails
- **Containerization**: Docker + Docker Compose

### Backend Structure

- **`controller/`** - REST API endpoints
- **`dto/`** - Request/response objects
- **`model/`** - Core entities and enums
- **`repository/`** - Spring Data JPA repositories
- **`security/`** - JWT filter, JWT service, and security configuration
- **`service/`** - Business logic layer

### Frontend Structure

- **`app/`** - App routing
- **`pages/`** - Main application pages
- **`components/`** - Reusable UI components
- **`services/`** - API/session helpers
- **`styles/`** - CSS files
- **`utils/`** - Utility helpers

---

## 🧰 Tech Stack

### Backend
- Java 21
- Spring Boot 3.5.7
- Spring Security
- Spring Data JPA
- Spring Validation
- MySQL
- JWT (`jjwt`)
- Spring Mail
- Maven
- JUnit 5

### Frontend
- React 19
- Vite
- React Router
- Axios
- `@react-google-maps/api`
- `@react-oauth/google`
- `react-datepicker`
- `react-phone-input-2`
- `libphonenumber-js`

### DevOps / Local Setup
- Docker
- Docker Compose

---

## 📁 Project Structure

```text
EasyPark/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── DemoApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
│       └── java/com/example/demo/
│           ├── controller/
│           └── service/
│
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── styles/
│   │   ├── utils/
│   │   ├── App.jsx
│   │   ├── config.js
│   │   └── main.jsx
│   ├── Dockerfile
│   └── package.json
│
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
└── pom.xml
```

---

## 🚀 Quick Start with Docker

This is the easiest way to run EasyPark locally.

### Prerequisites

Before running the project with Docker, install:

- **Docker Desktop**
- **Git**

### 1. Clone the repository

```bash
git clone https://github.com/AmeenKassem/EasyPark
cd EasyPark
```

### 2. Start the full application

```bash
docker compose up --build
```

This starts:

- **MySQL** database
- **Backend** on `http://localhost:8080`
- **Frontend** on `http://localhost:5173`

### 3. Open the application

```text
http://localhost:5173
```

### 4. Stop the application

```bash
docker compose down
```

To also remove database data volume:

```bash
docker compose down -v
```

Be careful: `-v` deletes the stored MySQL data.

---

## 🐳 Docker Notes

### Why Docker makes setup easier

Without Docker, a new developer needs to install and configure:

- Java 21
- Maven
- Node.js and npm
- MySQL
- database creation
- backend and frontend startup

With Docker, the main prerequisites become:

- Docker Desktop
- Git

and the project can be started with one command:

```bash
docker compose up --build
```

## ⚙️ Docker Configuration Example

A typical backend datasource URL in Docker Compose should look like this:

```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/easypark?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem
```


If port `3306` on your computer is already in use, you can expose MySQL on another host port such as:

```yaml
ports:
  - "3307:3306"
```

That does **not** change how the backend talks to the database inside Docker.

---

## 💻 Manual Setup (Without Docker)

Use this only if you want to run everything manually.

### Prerequisites

Install:

- **Java 21**
- **Maven**
- **Node.js** and **npm**
- **MySQL Server**
- **Git**
- An IDE such as **IntelliJ IDEA** or **VS Code**

Check your installed versions:

```bash
java -version
mvn -version
node -v
npm -v
```

### 1. Clone the repository

```bash
git clone https://github.com/AmeenKassem/EasyPark
cd EasyPark
```

### 2. Create the MySQL database

Open MySQL and create the database:

```sql
CREATE DATABASE easypark;
```

### 3. Configure backend properties

The backend configuration file is located at:

```text
src/main/resources/application.properties
```

A safe example configuration:

```properties
spring.application.name=easypark-backend

spring.datasource.url=jdbc:mysql://localhost:3306/easypark?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

server.address=0.0.0.0
server.port=8080

# JWT configuration
security.jwt.secret=YOUR_JWT_SECRET
security.jwt.expiration-minutes=60

# Google login
google.client-id=YOUR_GOOGLE_CLIENT_ID

# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_GMAIL_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.from=YOUR_EMAIL@gmail.com

# Frontend reset-password page
app.frontend.reset-password-url=http://localhost:5173/reset-password
app.security.reset-token-expiration-minutes=30
```

### 4. Run the backend

From the project root:

```bash
mvn clean install
mvn spring-boot:run
```

The backend runs by default at:

```text
http://localhost:8080
```

### 5. Run the frontend

Open a new terminal and go to the frontend folder:

```bash
cd frontend
npm install
npm run dev
```

The frontend will usually run at:

```text
http://localhost:5173
```

### 6. Frontend API configuration

Frontend API base URL is defined in:

```js
export const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
```

So by default, the frontend expects the backend to run on:

```text
http://localhost:8080
```

If needed, you can override it with a Vite environment variable.

Example `.env` file inside `frontend/`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## 🔐 Authentication and Roles

EasyPark supports these backend roles:

- `DRIVER`
- `OWNER`
- `BOTH`

The frontend stores session data in localStorage using:

- `easypark_user`
- `easypark_token`

The backend returns a single role, and the frontend converts it into UI roles:

- `DRIVER` → `['DRIVER']`
- `OWNER` → `['OWNER']`
- `BOTH` → `['DRIVER', 'OWNER']`

Protected frontend routes are enforced with `RequireRole`.

---

## 🗺️ Frontend Pages and Routes

Current frontend routes include:

- `/`
- `/login`
- `/register`
- `/reset-password`
- `/forgot-password`
- `/dashboard`
- `/driver`
- `/owner`
- `/manage-spots`
- `/manage-profile`
- `/change-password`
- `/my-bookings`
- `/revenues`
- `/expenses`
- `/create-parking`
- `/no-permission`

---

## 📡 Backend API Overview

### Authentication / User-related
Base path:

```text
/api/auth
```

Includes flows such as:

- register
- login
- forgot password
- reset password
- Google login
- user listing

### Main backend controllers

- `AuthController`
- `BookingController`
- `ParkingController`
- `PaymentController`
- `ReportController`
- `UserController`
- `PingController`

### Main backend domains

- User
- Parking
- ParkingAvailability
- Booking
- Payment
- Notification
- PasswordResetToken
- DriverRating
- ParkingRating

---

## 🧪 Testing

### Backend tests

Run backend tests from the project root:

```bash
mvn test
```

Test coverage currently includes controller and service tests such as:

- `AuthControllerTests`
- `BookingControllerTest`
- `ParkingControllerTest`
- `BookingServiceTest`
- `EmailServiceTest`
- `ParkingServiceTest`
- `RatingServiceTest`
- `ReportServiceTest`
- `UserServiceTest`

### Frontend checks

From the `frontend` folder:

```bash
npm run lint
npm run build
```

---

## ✅ Recommended Startup Order

### With Docker
1. Run `docker compose up --build`
2. Open `http://localhost:5173`
3. Register or log in
4. Test owner/driver flows

### Without Docker
1. Start **MySQL**
2. Make sure the `easypark` database exists
3. Start the **backend**
4. Start the **frontend**
5. Open the frontend in the browser
6. Register a user or log in
7. Test owner/driver flows

---

## 🧪 Example Local Flow for New Developers

A new developer can verify the project with this flow:

1. Start the system with Docker
2. Open `http://localhost:5173`
3. Register a new account
4. Log in
5. If the user is an owner, create a parking spot
6. If the user is a driver, search for parking and test booking flows

---

## 🔧 Troubleshooting

### Docker setup does not start
Check:

- Docker Desktop is running
- ports `5173`, `8080`, and the chosen MySQL host port are free
- `docker-compose.yml` is present in the project root
- Docker images built successfully

### Backend does not start
Check:

- datasource URL is correct
- MySQL is running
- `allowPublicKeyRetrieval=true` is included when needed
- JWT/mail/Google config values are valid
- port `8080` is not already in use

### Frontend cannot reach backend
Check:

- backend is running on `http://localhost:8080`
- frontend uses the correct `VITE_API_BASE_URL`
- no proxy/firewall is blocking local requests

### Password reset email does not work
Check:

- Gmail SMTP settings are correct
- you are using a valid Gmail app password
- `spring.mail.*` properties are configured
- reset-password URL points to the frontend route

### Google login does not work
Check:

- the configured Google client ID is valid
- frontend and backend are using the correct client configuration
- local origins are allowed in the Google Cloud Console

### Database issues
Check:

- MySQL is running
- database name is `easypark`
- username/password are correct
- your MySQL user has permission to access the database

---

## 🎓 Academic Context

EasyPark was developed as part of the **Software Engineering Seminar (Final Project)** at **Ben-Gurion University**.

The project demonstrates practical work in:

- full-stack web development
- REST API design
- authentication and authorization
- database persistence with JPA
- role-based access control
- testing and CI
- team-based software engineering

---

## 🤝 Contributors

- **Kfir Shalom**
- **Omar Ben Hamo**
- **Mohammad Ameen Kassem**
- **Ariel Mazhibovsky**

Ben-Gurion University - Software Engineering Seminar

---

## 🐛 Issues and Support

If you encounter issues while running the project:

1. Prefer the Docker setup first
2. Verify that required secrets/config values are present
3. Check that frontend and backend are running on the expected ports
4. Run backend tests with `mvn test`
5. Run frontend checks with `npm run lint` and `npm run build`

---

## 📄 License

This project was developed in an academic context as part of a university seminar project.

If you plan to publish or reuse it, add an explicit license file to the repository.

---

*Built for smarter parking management and hands-on software engineering experience*
