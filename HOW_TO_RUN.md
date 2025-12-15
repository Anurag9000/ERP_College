# How to Run the College ERP

## Prerequisites
-   **Java 17** (or higher) installed.
-   **MariaDB/MySQL** running locally (Port 3306).
-   **Maven** (Optional, for building).

## Quick Start (Pre-compiled)
If you have the JAR file:
```bash
java -jar erp-college.jar
```

## Running from Source

### Option 1: Using Maven (Recommended)
1.  Open terminal in project root.
2.  Run:
    ```bash
    mvn clean compile exec:java
    ```

### Option 2: Using Node Helper
1.  Ensure Node.js is installed.
2.  Run:
    ```bash
    npm run run-java
    ```

## Default Credentials
The system comes pre-seeded with these accounts:

| Role | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` |
| **Student** | `stu1` | `pass123` |
| **Faculty** | `prof1` | `pass123` |

## Troubleshooting
-   **Database Error:** Check `src/main/resources/application.properties` to ensure your DB username/password matches your local MariaDB setup.
-   **UI Glitches:** Ensure you are not running in "Headless" mode. This is a Swing GUI app.
