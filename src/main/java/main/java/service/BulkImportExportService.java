package main.java.service;

import main.java.models.*;
import main.java.utils.DatabaseUtil;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for bulk import/export operations
 */
public class BulkImportExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Import students from CSV
     * Format: studentId,firstName,lastName,email,phone,dob,address,course,semester
     */
    public static List<String> importStudents(File csvFile) throws IOException {
        List<String> errors = new ArrayList<>();
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine(); // Skip header
            lineNum++;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 9) {
                        errors.add("Line " + lineNum + ": Insufficient columns");
                        continue;
                    }

                    Student student = new Student();
                    student.setStudentId(parts[0].trim());
                    student.setFirstName(parts[1].trim());
                    student.setLastName(parts[2].trim());
                    student.setEmail(parts[3].trim());
                    student.setPhone(parts[4].trim());
                    student.setDateOfBirth(LocalDate.parse(parts[5].trim(), DATE_FORMAT));
                    student.setAddress(parts[6].trim());
                    student.setCourse(parts[7].trim());
                    student.setSemester(Integer.parseInt(parts[8].trim()));

                    DatabaseUtil.addStudent(student);

                } catch (Exception e) {
                    errors.add("Line " + lineNum + ": " + e.getMessage());
                }
            }
        }

        return errors;
    }

    /**
     * Export students to CSV
     */
    public static void exportStudents(File csvFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("StudentID,FirstName,LastName,Email,Phone,DOB,Address,Course,Semester,CGPA,Status");

            for (Student s : DatabaseUtil.getAllStudents()) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%d,%.2f,%s%n",
                        s.getStudentId(),
                        s.getFirstName(),
                        s.getLastName(),
                        s.getEmail(),
                        s.getPhone(),
                        s.getDateOfBirth() != null ? s.getDateOfBirth().format(DATE_FORMAT) : "",
                        s.getAddress() != null ? s.getAddress().replace(",", ";") : "",
                        s.getCourse(),
                        s.getSemester(),
                        s.getCgpa(),
                        s.getStatus());
            }
        }
    }

    /**
     * Import faculty from CSV
     * Format:
     * facultyId,firstName,lastName,email,phone,department,designation,qualification,salary
     */
    public static List<String> importFaculty(File csvFile) throws IOException {
        List<String> errors = new ArrayList<>();
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine(); // Skip header
            lineNum++;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 9) {
                        errors.add("Line " + lineNum + ": Insufficient columns");
                        continue;
                    }

                    Faculty faculty = new Faculty();
                    faculty.setFacultyId(parts[0].trim());
                    faculty.setFirstName(parts[1].trim());
                    faculty.setLastName(parts[2].trim());
                    faculty.setEmail(parts[3].trim());
                    faculty.setPhone(parts[4].trim());
                    faculty.setDepartment(parts[5].trim());
                    faculty.setDesignation(parts[6].trim());
                    faculty.setQualification(parts[7].trim());
                    faculty.setSalary(Double.parseDouble(parts[8].trim()));

                    DatabaseUtil.addFaculty(faculty);

                } catch (Exception e) {
                    errors.add("Line " + lineNum + ": " + e.getMessage());
                }
            }
        }

        return errors;
    }

    /**
     * Export faculty to CSV
     */
    public static void exportFaculty(File csvFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println(
                    "FacultyID,FirstName,LastName,Email,Phone,Department,Designation,Qualification,Salary,Status");

            for (Faculty f : DatabaseUtil.getAllFaculty()) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s%n",
                        f.getFacultyId(),
                        f.getFirstName(),
                        f.getLastName(),
                        f.getEmail(),
                        f.getPhone(),
                        f.getDepartment(),
                        f.getDesignation(),
                        f.getQualification(),
                        f.getSalary(),
                        f.getStatus());
            }
        }
    }

    /**
     * Export gradebook for a section to CSV
     */
    public static void exportGradebook(String sectionId, File csvFile) throws IOException {
        Section section = DatabaseUtil.getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Header
            writer.print("StudentID,Name");
            for (String component : section.getAssessmentWeights().keySet()) {
                writer.print("," + component);
            }
            writer.println(",FinalGrade");

            // Data rows
            for (String studentId : section.getEnrolledStudentIds()) {
                Student student = DatabaseUtil.getStudent(studentId);
                writer.print(studentId + "," + (student != null ? student.getFullName() : "Unknown"));

                Map<String, Double> grades = DatabaseUtil.getGrades(studentId, sectionId);
                for (String component : section.getAssessmentWeights().keySet()) {
                    Double grade = grades.get(component);
                    writer.print("," + (grade != null ? String.format("%.2f", grade) : ""));
                }

                Double finalGrade = DatabaseUtil.getFinalGrade(studentId, sectionId);
                writer.println("," + (finalGrade != null ? String.format("%.2f", finalGrade) : ""));
            }
        }
    }

    /**
     * Export all schedules to CSV
     */
    public static void exportSchedules(File csvFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println(
                    "SectionID,CourseID,Title,Instructor,Day,StartTime,EndTime,Location,Capacity,Enrolled,Semester,Year");

            for (Section s : DatabaseUtil.getAllSections()) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%s,%d%n",
                        s.getSectionId(),
                        s.getCourseId(),
                        s.getTitle() != null ? s.getTitle().replace(",", ";") : "",
                        s.getFacultyId(),
                        s.getDayOfWeek() != null ? s.getDayOfWeek().name() : "",
                        s.getStartTime() != null ? s.getStartTime().toString() : "",
                        s.getEndTime() != null ? s.getEndTime().toString() : "",
                        s.getLocation() != null ? s.getLocation().replace(",", ";") : "",
                        s.getCapacity(),
                        s.getEnrolledStudentIds().size(),
                        s.getSemester(),
                        s.getYear());
            }
        }
    }
}
