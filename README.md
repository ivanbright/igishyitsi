# Igishyitsi - E-Submission System

A premium dark-themed e-submission system built with Java, JSP, and Hibernate.

## Features
- **Teacher Dashboard**: Manage assignments, view student progress, and monitor submissions.
- **Student Dashboard**: Track courses, submit assignments, and view grades.
- **Admin Dashboard**: Track pretty much everything and acts as bridge between students and teachers and oversees the system security.
- **Security**: 2FA via Email, CAPTCHA on all entry points, and input sanitization.
- **Password Recovery**: Secure "Forgot Password" flow with email integration.
- **Premium UI**: Modern dark-mode aesthetic with smooth animations and toasters.

## Tech Stack
- **Backend**: Java Servlets, Hibernate ORM, PostgreSQL.
- **Frontend**: JSP, Vanilla CSS (Connet Design System), JavaScript.
- **Dependencies**: Jakarta Mail, Jakarta Persistence, Hibernate Core, PostgreSQL Driver.

## Getting Started

### Prerequisites
- JDK 17 or higher
- PostgreSQL 14+
- Apache Tomcat 10.1+
- Maven 3.8+

### Database Setup
1. Create a database named `furnitdb` in PostgreSQL.
2. Update `src/main/resources/hibernate.cfg.xml` with your database credentials.

### Configuration
1. Update `SMTP_USER` and `SMTP_PASS` in `EmailService.java` with your Gmail App Password.
2. Update `SECRET_KEY` in `JwtUtil.java` for production use.

### Building and Running
```bash
# Clean and package
mvn clean package
powershell -ExecutionPolicy Bypass -File .\run.ps1
# Deploy the generated WAR file (target/furnitBackend.war) to your Tomcat webapps directory.
```

## Authors
- Mugisha Ivan Bright
