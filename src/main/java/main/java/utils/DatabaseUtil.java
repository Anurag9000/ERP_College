package main.java.utils;

import main.java.models.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database utility class for managing data persistence
 * Uses file-based storage for simplicity
 */
public class DatabaseUtil {
    private static final String DATA_DIR = "data/";
    private static final String USERS_FILE = DATA_DIR + "users.dat";
    private static final String STUDENTS_FILE = DATA_DIR + "students.dat";
    private static final String FACULTY_FILE = DATA_DIR + "faculty.dat";
    private static final String COURSES_FILE = DATA_DIR + "courses.dat";
    
    private static Map<String, User> users = new ConcurrentHashMap<>();
    private static Map<String, Student> students = new ConcurrentHashMap<>();
    private static Map<String, Faculty> faculty = new ConcurrentHashMap<>();
    private static Map<String, Course> courses = new ConcurrentHashMap<>();
    
    public static void initializeDatabase() {
        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Load existing data or create sample data
        loadData();
        if (users.isEmpty()) {
            createSampleData();
            saveData();
        }
    }
    
    private static void createSampleData() {
        // Create default admin user
        User admin = new User("admin", "admin123", "Admin", "Administrator", "admin@college.edu");
        users.put(admin.getUsername(), admin);
        
        // Create sample faculty
        Faculty f1 = new Faculty("FAC001", "John", "Smith", "john.smith@college.edu", 
                                "123-456-7890", "Computer Science", "Professor", "Ph.D", 75000);
        f1.getSubjects().addAll(Arrays.asList("Data Structures", "Algorithms", "Java Programming"));
        faculty.put(f1.getFacultyId(), f1);
        
        Faculty f2 = new Faculty("FAC002", "Jane", "Davis", "jane.davis@college.edu", 
                                "123-456-7891", "Mathematics", "Associate Professor", "M.Sc", 65000);
        f2.getSubjects().addAll(Arrays.asList("Calculus", "Linear Algebra", "Statistics"));
        faculty.put(f2.getFacultyId(), f2);
        
        // Create sample courses
        Course c1 = new Course("CSE101", "Computer Science Engineering", "Computer Science", 
                              8, 200000, "4-year undergraduate program in Computer Science", 60);
        c1.getSubjects().addAll(Arrays.asList("Programming", "Data Structures", "Algorithms", 
                                            "Database Systems", "Operating Systems"));
        courses.put(c1.getCourseId(), c1);
        
        Course c2 = new Course("MATH101", "Mathematics", "Mathematics", 
                              6, 150000, "3-year undergraduate program in Mathematics", 40);
        c2.getSubjects().addAll(Arrays.asList("Calculus", "Linear Algebra", "Statistics", 
                                            "Discrete Mathematics"));
        courses.put(c2.getCourseId(), c2);
        
        // Create sample students
        Student s1 = new Student("STU001", "Alice", "Johnson", "alice.johnson@student.college.edu", 
                               "987-654-3210", LocalDate.of(2000, 5, 15), 
                               "123 Main St, City", "CSE101", 3);
        s1.setTotalFees(200000);
        s1.setFeesPaid(150000);
        students.put(s1.getStudentId(), s1);
        
        Student s2 = new Student("STU002", "Bob", "Williams", "bob.williams@student.college.edu", 
                               "987-654-3211", LocalDate.of(1999, 8, 22), 
                               "456 Oak Ave, City", "MATH101", 2);
        s2.setTotalFees(150000);
        s2.setFeesPaid(100000);
        students.put(s2.getStudentId(), s2);
        
        // Update course seats
        c1.setAvailableSeats(c1.getAvailableSeats() - 1);
        c2.setAvailableSeats(c2.getAvailableSeats() - 1);
    }
    
    @SuppressWarnings("unchecked")
    private static void loadData() {
        try {
            if (new File(USERS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILE));
                users = (Map<String, User>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        
        try {
            if (new File(STUDENTS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STUDENTS_FILE));
                students = (Map<String, Student>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading students: " + e.getMessage());
        }
        
        try {
            if (new File(FACULTY_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FACULTY_FILE));
                faculty = (Map<String, Faculty>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading faculty: " + e.getMessage());
        }
        
        try {
            if (new File(COURSES_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(COURSES_FILE));
                courses = (Map<String, Course>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading courses: " + e.getMessage());
        }
    }
    
    public static void saveData() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE));
            oos.writeObject(users);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(STUDENTS_FILE));
            oos.writeObject(students);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(FACULTY_FILE));
            oos.writeObject(faculty);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(COURSES_FILE));
            oos.writeObject(courses);
            oos.close();
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }
    
    // User operations
    public static User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password) && user.isActive()) {
            return user;
        }
        return null;
    }
    
    public static Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    // Student operations
    public static void addStudent(Student student) {
        students.put(student.getStudentId(), student);
        saveData();
    }
    
    public static void updateStudent(Student student) {
        students.put(student.getStudentId(), student);
        saveData();
    }
    
    public static void deleteStudent(String studentId) {
        students.remove(studentId);
        saveData();
    }
    
    public static Student getStudent(String studentId) {
        return students.get(studentId);
    }
    
    public static Collection<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }
    
    // Faculty operations
    public static void addFaculty(Faculty facultyMember) {
        faculty.put(facultyMember.getFacultyId(), facultyMember);
        saveData();
    }
    
    public static void updateFaculty(Faculty facultyMember) {
        faculty.put(facultyMember.getFacultyId(), facultyMember);
        saveData();
    }
    
    public static void deleteFaculty(String facultyId) {
        faculty.remove(facultyId);
        saveData();
    }
    
    public static Faculty getFaculty(String facultyId) {
        return faculty.get(facultyId);
    }
    
    public static Collection<Faculty> getAllFaculty() {
        return new ArrayList<>(faculty.values());
    }
    
    // Course operations
    public static void addCourse(Course course) {
        courses.put(course.getCourseId(), course);
        saveData();
    }
    
    public static void updateCourse(Course course) {
        courses.put(course.getCourseId(), course);
        saveData();
    }
    
    public static void deleteCourse(String courseId) {
        courses.remove(courseId);
        saveData();
    }
    
    public static Course getCourse(String courseId) {
        return courses.get(courseId);
    }
    
    public static Collection<Course> getAllCourses() {
        return new ArrayList<>(courses.values());
    }
    
    public static String generateNextId(String prefix, Collection<?> collection) {
        int maxId = 0;
        for (Object obj : collection) {
            String id = "";
            if (obj instanceof Student) {
                id = ((Student) obj).getStudentId();
            } else if (obj instanceof Faculty) {
                id = ((Faculty) obj).getFacultyId();
            } else if (obj instanceof Course) {
                id = ((Course) obj).getCourseId();
            }
            
            if (id.startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(id.substring(prefix.length()));
                    maxId = Math.max(maxId, num);
                } catch (NumberFormatException e) {
                    // Ignore non-numeric suffixes
                }
            }
        }
        return prefix + String.format("%03d", maxId + 1);
    }
}