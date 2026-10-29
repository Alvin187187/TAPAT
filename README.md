# TAPAT

Community Financial Assistance and Transparency Management System

This is a Java desktop project. The screens are built with JavaFX. The records are stored in a local SQLite database.

The main flow of the system is:

Apply -> Approve -> Release -> Verify

## Roles

Each role is a subclass of `User`. The screens a person can open depend on the role.

- Guest
- Applicant
- Administrator
- Finance Officer
- Auditor

## How to run

Install JDK 17 or newer. From the project folder:

```
mvnw.cmd javafx:run
```

On first run the program creates `data/tapat.db` and the demo accounts.

## Demo accounts

| Username | Password | Role |
| --- | --- | --- |
| applicant | applicant123 | Applicant |
| admin | admin123 | Administrator |
| finance | finance123 | Finance Officer |
| auditor | auditor123 | Auditor |
| juan | juan1234 | Applicant |
| juanm | juan1234 | Applicant (duplicate check demo) |

A guest can browse programs without signing in.

## Folders

- `model` - objects such as User, Application, and Program
- `ui` - JavaFX screens
- `service` - business rules
- `repository` - SQLite save and load
- `src/test/java` - JUnit tests

## Tests

```
mvnw.cmd test
```
