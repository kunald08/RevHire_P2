# RevHire

> A full-stack job portal web application connecting Job Seekers and Employers, built with Spring Boot, Thymeleaf, and Oracle Database.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" alt="Thymeleaf">
  <img src="https://img.shields.io/badge/Oracle%20DB-21c-F80000?style=for-the-badge&logo=oracle&logoColor=white" alt="Oracle Database">
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven">
</p>

## Overview

RevHire is a server-side rendered recruitment platform designed around two user roles:

- Job Seekers can register, build professional profiles, upload or create resumes, search jobs with filters, apply, save favorites, and track application progress.
- Employers can create company profiles, publish jobs, review applicants, take bulk actions, add internal notes, and monitor hiring activity through a dashboard.

The project uses role-based access control with Spring Security, session-based authentication, in-app notifications, OTP-based verification flows, and Oracle-backed persistence with Spring Data JPA.

## Why This Project Stands Out

- Separate job seeker and employer workflows
- Registration OTP verification and OTP-based login flow
- Forgot-password and reset-password support via email
- Resume builder and resume upload support
- Advanced job search, favorites, and application tracking
- Employer applicant filtering, bulk actions, and internal notes
- Scheduled auto-closing of expired jobs
- Thymeleaf UI with reusable layout fragments
- Unit tests across auth, profile, application, employer, job, and notification modules

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| UI | Thymeleaf, Bootstrap 5, custom JS/CSS |
| Security | Spring Security, BCrypt, session auth |
| Persistence | Spring Data JPA, Hibernate |
| Database | Oracle Database XE / Oracle 21c-compatible setup |
| Mail | Spring Boot Mail |
| Logging | Log4j2 |
| Build Tool | Maven |
| Testing | JUnit 4, Spring Boot Test, Spring Security Test |

## Core Modules

```text
src/main/java/com/revhire/
├── auth/          Authentication, OTP, password flows, user management
├── profile/       Job seeker profile, resume upload, resume builder
├── employer/      Employer profile, applicants, notes, dashboard
├── job/           Job CRUD, filters, lifecycle, scheduled expiry handling
├── application/   Job applications, favorites, job search
├── notification/  In-app notification system
├── config/        Security, MVC, CORS, app configuration
├── exception/     Global exception handling
└── common/        Shared DTOs and enums
```

## Feature Summary

### Job Seekers

- Register as a seeker and verify account with OTP
- Maintain profile details including education, experience, skills, and certifications
- Build a resume in-app or upload a resume file
- Search jobs by keyword and filters such as location, salary, job type, and experience
- Apply to jobs and monitor status updates
- Save jobs to favorites
- View notifications related to applications and platform activity

### Employers

- Register as an employer and manage company details
- Create, edit, publish, close, reopen, and manage job postings
- Review applicants and inspect profile, resume, and cover letter data
- Filter applicants and apply bulk status actions
- Add internal notes for recruiting workflow management
- View dashboard stats for jobs and applications

## Architecture Snapshot

```text
                         +-------------------+
                         |   Client Browser  |
                         +---------+---------+
                                   |
                                   v
                    +--------------+---------------+
                    | Thymeleaf Templates / Static |
                    | CSS, JS, Fragments, Views    |
                    +--------------+---------------+
                                   |
                                   v
                    +--------------+---------------+
                    |      Spring MVC Controllers  |
                    | Auth, Profile, Job, Employer |
                    | Application, Notification    |
                    +--------------+---------------+
                                   |
                                   v
                    +--------------+---------------+
                    |         Service Layer        |
                    | Business rules and workflows |
                    | OTP, resumes, jobs, apps,    |
                    | dashboard, notifications     |
                    +--------------+---------------+
                                   |
                                   v
                    +--------------+---------------+
                    |    Spring Data JPA Layer     |
                    | Repositories and persistence |
                    +--------------+---------------+
                                   |
                                   v
                    +--------------+---------------+
                    |     Oracle Database XE       |
                    +------------------------------+

   Cross-cutting: Spring Security | Log4j2 | Scheduler | Global Exception Handler
```

## Security and Workflow Notes

- Role-based route protection is configured in Spring Security for `SEEKER` and `EMPLOYER`
- The app uses form login with a custom success handler
- OTP flows are used for registration verification and login verification
- Password reset is email-driven
- Uploaded files are stored under the configured upload directory, which defaults to `uploads`
- A scheduled task runs daily to auto-close expired active jobs

## Project Structure

```text
src/main/resources/
├── application.properties
├── application-local.properties
├── log4j2.xml
├── static/
│   ├── css/
│   ├── js/
│   └── favicon files
└── templates/
    ├── auth/
    ├── employer/
    ├── job/
    ├── profile/
    ├── application/
    ├── notification/
    └── fragments/
```

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.x
- Oracle Database XE or another Oracle instance

### 1. Default Local Profile

The application starts with:

```properties
spring.profiles.active=local
```

The local profile expects Oracle settings from `src/main/resources/application-local.properties`.

### 2. Oracle Database Setup

The current local datasource is configured for:

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

Create a database user/schema that matches your local setup, then update:

```properties
spring.datasource.username=YOUR_ORACLE_USERNAME
spring.datasource.password=YOUR_ORACLE_PASSWORD
```

### 3. Mail Configuration

Email flows depend on these values:

```properties
MAIL_USERNAME=your_email
MAIL_PASSWORD=your_app_password
mail.from.address=your_email
```

If you do not want to use the checked-in local values, replace them in `application-local.properties` or externalize them through environment-aware configuration before running the app.

### 4. Run the Project

```bash
git clone https://github.com/kunald08/RevHire_P2.git
cd RevHire_P2
mvn clean install
mvn spring-boot:run
```

Application URL:

```text
http://localhost:8080
```

### 5. Run Tests

```bash
mvn test
```

## Important Routes

| Area | Route Pattern |
|---|---|
| Home | `/` |
| Authentication | `/auth/*` |
| Profiles and Resume | `/profile/*`, `/resume/*` |
| Jobs | `/jobs/*` |
| Applications | `/applications/*` |
| Favorites | `/favorites/*` |
| Employer | `/employer/*`, `/employers/*` |
| Notifications | `/notifications/*` |

## Development Notes

- JPA schema generation is enabled with `spring.jpa.hibernate.ddl-auto=update`
- SQL logging is enabled in the default configuration
- Thymeleaf caching is disabled for local development
- Session timeout is configured to `30m`
- Multipart upload limit is set to `5MB`

<p align="center">
  <sub>Built with Spring Boot, Thymeleaf, and Oracle Database</sub>
</p>
