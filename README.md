# RevHire

> A full-stack job portal web application connecting **Job Seekers** with **Employers**, built with Spring Boot and Thymeleaf.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" alt="Thymeleaf">
  <img src="https://img.shields.io/badge/Oracle%20DB-21c-F80000?style=for-the-badge&logo=oracle&logoColor=white" alt="Oracle">
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven">
</p>

---

## Overview

RevHire is a server-side rendered monolithic web application that enables job seekers to build profiles, create and upload resumes, search for openings with advanced filters, and track their applications in real time. Employers can register their companies, post jobs, manage the hiring pipeline, and monitor recruitment metrics through a dedicated dashboard. The platform features role-based access control, session-based authentication, OTP-based verification flows, password recovery via email, and an in-app notification system.

---

## Features

### For Job Seekers

- Register and manage a professional profile (education, experience, skills, certifications)
- Build a structured textual resume or upload one in PDF/DOCX format
- Search jobs with filters - role, location, experience, salary range, job type, company, date posted
- One-click apply with saved resume and optional cover letter
- Track application status - `Applied -> Under Review -> Shortlisted -> Rejected -> Withdrawn`
- Save jobs to favorites for later
- Receive in-app notifications on status updates and job recommendations

### For Employers

- Create and manage a company profile (industry, size, website, description)
- Post, edit, close, reopen, and manage job listings
- View applicant details - profile, resume, cover letter, application date
- Shortlist or reject candidates individually or in bulk with optional comments
- Filter applicants by experience, skills, education, status, and date
- Add internal notes to applications for team tracking
- Dashboard with key metrics - total jobs, active postings, applications, pending reviews

---

## Tech Stack

| | Technology |
|---|-----------|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | Spring Security - session-based auth, BCrypt |
| **Data Access** | Spring Data JPA |
| **View Layer** | Thymeleaf, Bootstrap 5 |
| **Database** | Oracle 21c XE |
| **Build** | Maven |
| **Testing** | JUnit 4 |
| **Logging** | Log4J2 |
| **Utilities** | Lombok |

---

## Architecture

```text
                          ┌────────────┐
                          │  Browser   │
                          └─────┬──────┘
                                │
                   ┌────────────▼────────────┐
                   │    Spring Boot App      │
                   │                         │
                   │  ┌───────────────────┐  │
                   │  │   Controllers     │──┼──► Thymeleaf Templates
                   │  └────────┬──────────┘  │
                   │           │             │
                   │  ┌────────▼──────────┐  │
                   │  │  Service Layer    │  │
                   │  └────────┬──────────┘  │
                   │           │             │
                   │  ┌────────▼──────────┐  │
                   │  │  JPA Repositories │  │
                   │  └────────┬──────────┘  │
                   │           │             │
                   │  ┌────────▼──────────┐  │
                   │  │ Oracle Database   │  │
                   │  └───────────────────┘  │
                   │                         │
                   │  Spring Security        │
                   │  Log4J2 · Lombok        │
                   │  Global Exception Hdlr  │
                   └─────────────────────────┘
```

---

## Database Schema

The application uses **13 tables** with the following relationships:

```text
User ---- JobSeekerProfile ---- Education, Experience, Skill, Certification, Resume
User ---- Employer ---- Job ---- Application ---- ApplicationNote
User ---- Favorite ---- Job
User ---- Notification
```

| Table | Description |
|-------|-------------|
| `users` | All registered users (seekers and employers) |
| `job_seeker_profiles` | Seeker headline, summary, employment status |
| `educations` | Degrees, institutions, field of study |
| `experiences` | Work history with company, title, dates |
| `skills` | Technical and soft skills with proficiency |
| `certifications` | Professional certifications |
| `resumes` | Uploaded files (PDF/DOCX) and textual resume data |
| `employers` | Company name, industry, size, website |
| `jobs` | Job postings with type, salary, status, deadline |
| `applications` | Job applications with status and cover letter |
| `application_notes` | Internal employer notes per application |
| `favorites` | Bookmarked jobs by seekers |
| `notifications` | In-app notifications with read status |

---

## Project Structure

```text
src/main/java/com/revhire/
├── RevHireApplication.java       # Entry point
├── auth/                         # Authentication and user management
├── config/                       # Security and app configuration
├── profile/                      # Seeker profiles and resumes
├── employer/                     # Employer profiles and dashboard
├── job/                          # Job posting and lifecycle
├── application/                  # Search, apply, favorites
├── notification/                 # Notification system
├── exception/                    # Global exception handling
└── common/                       # Shared DTOs and utilities

src/main/resources/
├── application.properties
├── application-local.properties
├── log4j2.xml
├── static/                       # CSS, JS, images
└── templates/                    # Thymeleaf pages and fragments
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.x
- Oracle Database XE / Oracle 21c-compatible instance

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/kunald08/RevHire_P2.git
cd RevHire_P2

# 2. Update Oracle datasource values in src/main/resources/application-local.properties
#    spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
#    spring.datasource.username=YOUR_ORACLE_USERNAME
#    spring.datasource.password=YOUR_ORACLE_PASSWORD
#    spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# 3. Update mail values if OTP / forgot-password flows are required
#    MAIL_USERNAME=your_email
#    MAIL_PASSWORD=your_app_password
#    mail.from.address=your_email

# 4. Build and run
mvn clean install
mvn spring-boot:run
```

The application will be available at **http://localhost:8080**

### Running Tests

```bash
mvn test
```

---

## API Endpoints

| Area | Endpoints |
|------|-----------|
| Authentication | `/auth/*` |
| Seeker Profiles | `/profile/*`, `/resume/*` |
| Employer & Jobs | `/employers/*`, `/jobs/*` |
| Job Search | `/jobs/search` |
| Applications | `/applications/*` |
| Favorites | `/favorites/*` |
| Employer Dashboard | `/employer/dashboard`, `/employer/applicants/*` |
| Notifications | `/notifications/*` |

---

<p align="center">
  <sub>Built with Spring Boot, Thymeleaf, and Oracle Database</sub>
</p>
