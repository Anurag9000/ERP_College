package main.java.utils;

import org.junit.jupiter.api.Test;
import main.java.service.AdminServiceTest;
import main.java.service.EnrollmentServiceTest;
import main.java.service.GradebookServiceTest;
import main.java.service.StudentServiceTest;
import main.java.service.FacultyServiceTest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ManualTestRunner {
    public static void main(String[] args) {
        List<Class<?>> testClasses = new ArrayList<>();
        testClasses.add(PasswordUtilTest.class);
        testClasses.add(PasswordPolicyTest.class);
        testClasses.add(AdminServiceTest.class);
        testClasses.add(EnrollmentServiceTest.class);
        testClasses.add(GradebookServiceTest.class);
        testClasses.add(StudentServiceTest.class);
        testClasses.add(FacultyServiceTest.class);

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        for (Class<?> testClass : testClasses) {
            System.out.println("Running tests in " + testClass.getSimpleName());
            try {
                Object testInstance = testClass.getDeclaredConstructor().newInstance();
                Method beforeEach = null;
                for (Method m : testClass.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)) {
                        beforeEach = m;
                        break;
                    }
                }

                for (Method method : testClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Test.class)) {
                        totalTests++;
                        try {
                            System.out.print("  - " + method.getName() + ": ");
                            if (beforeEach != null) {
                                beforeEach.setAccessible(true);
                                beforeEach.invoke(testInstance);
                            }
                            method.setAccessible(true);
                            method.invoke(testInstance);
                            System.out.println("PASSED");
                            passedTests++;
                        } catch (Exception e) {
                            System.out.println("FAILED");
                            if (e.getCause() != null) {
                                e.getCause().printStackTrace();
                            } else {
                                e.printStackTrace();
                            }
                            failedTests++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to instantiate test class: " + testClass.getName());
                e.printStackTrace();
            }
        }

        System.out.println("\nTest Results:");
        System.out.println("Total:  " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + failedTests);

        if (failedTests > 0) {
            System.exit(1);
        }
    }
}
