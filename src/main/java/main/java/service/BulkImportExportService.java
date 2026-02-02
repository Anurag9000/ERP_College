package main.java.service;

import main.java.models.*;
import main.java.utils.DatabaseUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for bulk import/export operations using Apache Commons CSV for
 * robustness.
 */
public class BulkImportExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final CSVFormat CSV_DEFAULT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build();

    /**
     * Import students from CSV
     */
    /**
     * Import students from CSV.
     * 
     * @param csvFile the CSV file (must not be null and must exist)
     * @return list of error messages, empty if all successful
     * @throws IOException              if file read fails
     * @throws IllegalArgumentException if csvFile is null or doesn't exist
     */
    public static List<String> importStudents(File csvFile) throws IOException {
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null");
        }
        if (!csvFile.exists()) {
            throw new FileNotFoundException("CSV file not found: " + csvFile.getAbsolutePath());
        }
        List<String> errors = new ArrayList<>();
        List<Student> studentsToInsert = new ArrayList<>();

        try (Reader reader = new FileReader(csvFile, StandardCharsets.UTF_8);
                CSVParser parser = new CSVParser(reader, CSV_DEFAULT)) {

            for (CSVRecord record : parser) {
                try {
                    String studentId = record.get("StudentID");
                    if (studentId == null || studentId.isBlank()) {
                        throw new IllegalArgumentException("StudentID is missing");
                    }

                    Student student = new Student();
                    student.setStudentId(studentId.trim());
                    student.setFirstName(record.get("FirstName"));
                    student.setLastName(record.get("LastName"));
                    student.setEmail(record.get("Email"));
                    student.setPhone(record.get("Phone"));

                    String dobStr = record.get("DOB");
                    if (dobStr != null && !dobStr.isBlank()) {
                        try {
                            student.setDateOfBirth(LocalDate.parse(dobStr.trim(), DATE_FORMAT));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid DOB format. Expected yyyy-MM-dd");
                        }
                    }

                    student.setAddress(record.get("Address"));
                    student.setCourse(record.get("Course"));

                    String semStr = record.get("Semester");
                    if (semStr != null && !semStr.isBlank()) {
                        student.setSemester(Integer.parseInt(semStr.trim()));
                    } else {
                        student.setSemester(1); // Default
                    }

                    studentsToInsert.add(student);
                } catch (Exception e) {
                    errors.add("Line " + record.getRecordNumber() + " (" + record.get("StudentID") + "): "
                            + e.getMessage());
                }
            }
        }

        if (!studentsToInsert.isEmpty()) {
            try {
                DatabaseUtil.bulkAddStudents(studentsToInsert);
            } catch (Exception e) {
                errors.add("Database transaction failed: " + e.getMessage());
            }
        }

        return errors;
    }

    /**
     * Export students to CSV
     */
    /**
     * Export students to CSV.
     * 
     * @param csvFile the CSV file (must not be null)
     * @throws IOException              if file write fails
     * @throws IllegalArgumentException if csvFile is null
     */
    public static void exportStudents(File csvFile) throws IOException {
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null");
        }
        try (Writer writer = new FileWriter(csvFile, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("StudentID", "FirstName", "LastName", "Email", "Phone", "DOB", "Address", "Course",
                                "Semester", "CGPA", "Status")
                        .build())) {

            for (Student s : DatabaseUtil.getAllStudents()) {
                printer.printRecord(
                        s.getStudentId(),
                        s.getFirstName(),
                        s.getLastName(),
                        s.getEmail(),
                        s.getPhone(),
                        s.getDateOfBirth() != null ? s.getDateOfBirth().format(DATE_FORMAT) : "",
                        s.getAddress(),
                        s.getCourse(),
                        s.getSemester(),
                        String.format("%.2f", s.getCgpa()),
                        s.getStatus());
            }
        }
    }

    /**
     * Import faculty from CSV
     */
    /**
     * Import faculty from CSV.
     * 
     * @param csvFile the CSV file (must not be null and must exist)
     * @return list of error messages, empty if all successful
     * @throws IOException              if file read fails
     * @throws IllegalArgumentException if csvFile is null or doesn't exist
     */
    public static List<String> importFaculty(File csvFile) throws IOException {
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null");
        }
        if (!csvFile.exists()) {
            throw new FileNotFoundException("CSV file not found: " + csvFile.getAbsolutePath());
        }
        List<String> errors = new ArrayList<>();
        List<Faculty> facultyToInsert = new ArrayList<>();

        try (Reader reader = new FileReader(csvFile, StandardCharsets.UTF_8);
                CSVParser parser = new CSVParser(reader, CSV_DEFAULT)) {

            for (CSVRecord record : parser) {
                try {
                    String facultyId = record.get("FacultyID");
                    if (facultyId == null || facultyId.isBlank()) {
                        throw new IllegalArgumentException("FacultyID is missing");
                    }

                    Faculty faculty = new Faculty();
                    faculty.setFacultyId(facultyId.trim());
                    faculty.setFirstName(record.get("FirstName"));
                    faculty.setLastName(record.get("LastName"));
                    faculty.setEmail(record.get("Email"));
                    faculty.setPhone(record.get("Phone"));
                    faculty.setDepartment(record.get("Department"));
                    faculty.setDesignation(record.get("Designation"));
                    faculty.setQualification(record.get("Qualification"));

                    String salaryStr = record.get("Salary");
                    if (salaryStr != null && !salaryStr.isBlank()) {
                        try {
                            faculty.setSalary(Double.parseDouble(salaryStr.trim()));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid salary format. Expected numeric value.");
                        }
                    } else {
                        faculty.setSalary(0.0);
                    }

                    facultyToInsert.add(faculty);
                } catch (Exception e) {
                    errors.add("Line " + record.getRecordNumber() + " (" + record.get("FacultyID") + "): "
                            + e.getMessage());
                }
            }
        }

        if (!facultyToInsert.isEmpty()) {
            try {
                DatabaseUtil.bulkAddFaculty(facultyToInsert);
            } catch (Exception e) {
                errors.add("Database transaction failed: " + e.getMessage());
            }
        }

        return errors;
    }

    /**
     * Export faculty to CSV
     */
    public static void exportFaculty(File csvFile) throws IOException {
        try (Writer writer = new FileWriter(csvFile, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("FacultyID", "FirstName", "LastName", "Email", "Phone", "Department", "Designation",
                                "Qualification", "Salary", "Status")
                        .build())) {

            for (Faculty f : DatabaseUtil.getAllFaculty()) {
                printer.printRecord(
                        f.getFacultyId(),
                        f.getFirstName(),
                        f.getLastName(),
                        f.getEmail(),
                        f.getPhone(),
                        f.getDepartment(),
                        f.getDesignation(),
                        f.getQualification(),
                        String.format("%.2f", f.getSalary()),
                        f.getStatus());
            }
        }
    }

    /**
     * Export gradebook for a section to CSV
     */
    /**
     * Export gradebook for a section to CSV.
     * 
     * @param sectionId the section ID (must not be null or empty)
     * @param csvFile   the CSV file (must not be null)
     * @throws IOException              if file write fails
     * @throws IllegalArgumentException if parameters are invalid or section not
     *                                  found
     */
    public static void exportGradebook(String sectionId, File csvFile) throws IOException {
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null");
        }
        Section section = DatabaseUtil.getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }

        List<String> components = new ArrayList<>(section.getAssessmentWeights().keySet());
        List<String> header = new ArrayList<>(Arrays.asList("StudentID", "Name"));
        header.addAll(components);
        header.add("FinalGrade");

        try (Writer writer = new FileWriter(csvFile, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader(header.toArray(new String[0]))
                        .build())) {

            for (String studentId : section.getEnrolledStudentIds()) {
                Student student = DatabaseUtil.getStudent(studentId);
                List<Object> record = new ArrayList<>();
                record.add(studentId);
                record.add(student != null ? student.getFullName() : "Unknown");

                Map<String, Double> grades = DatabaseUtil.getGrades(studentId, sectionId);
                for (String component : components) {
                    Double grade = grades.get(component);
                    record.add(grade != null ? String.format("%.2f", grade) : "");
                }

                Double finalGrade = DatabaseUtil.getFinalGrade(studentId, sectionId);
                record.add(finalGrade != null ? String.format("%.2f", finalGrade) : "");

                printer.printRecord(record);
            }
        }
    }

    /**
     * Export all schedules to CSV
     */
    public static void exportSchedules(File csvFile) throws IOException {
        try (Writer writer = new FileWriter(csvFile, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("SectionID", "CourseID", "Title", "Instructor", "Day", "StartTime", "EndTime",
                                "Location", "Capacity", "Enrolled", "Semester", "Year")
                        .build())) {

            for (Section s : DatabaseUtil.getAllSections()) {
                printer.printRecord(
                        s.getSectionId(),
                        s.getCourseId(),
                        s.getTitle(),
                        s.getFacultyId(),
                        s.getDayOfWeek() != null ? s.getDayOfWeek().name() : "",
                        s.getStartTime() != null ? s.getStartTime().toString() : "",
                        s.getEndTime() != null ? s.getEndTime().toString() : "",
                        s.getLocation(),
                        s.getCapacity(),
                        s.getEnrolledStudentIds().size(),
                        s.getSemester(),
                        s.getYear());
            }
        }
    }

    /**
     * Generate student import template CSV
     */
    public static void generateStudentTemplate(File csvFile) throws IOException {
        try (Writer writer = new FileWriter(csvFile, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("StudentID", "FirstName", "LastName", "Email", "Phone", "DOB", "Address", "Course",
                                "Semester")
                        .build())) {
            printer.printRecord("S101", "John", "Doe", "john.doe@example.com", "1234567890", "2000-01-01",
                    "123 Main St", "CS", "1");
        }
    }

    /**
     * Generate faculty import template CSV
     */
    public static void generateFacultyTemplate(File csvFile) throws IOException {
        try (Writer writer = new FileWriter(csvFile, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("FacultyID", "FirstName", "LastName", "Email", "Phone", "Department", "Designation",
                                "Qualification", "Salary")
                        .build())) {
            printer.printRecord("F101", "Jane", "Smith", "jane.smith@example.com", "0987654321", "CS", "Professor",
                    "PhD", "80000");
        }
    }
}
