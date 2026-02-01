const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function findJavaFiles(dir) {
    if (!fs.existsSync(dir)) return [];
    const files = [];
    const items = fs.readdirSync(dir);
    for (const item of items) {
        const fullPath = path.join(dir, item);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            files.push(...findJavaFiles(fullPath));
        } else if (item.endsWith('.java')) {
            files.push(fullPath);
        }
    }
    return files;
}

const repoBase = 'C:\\Users\\DELL\\.m2\\repository';
const jars = [
    'org\\junit\\jupiter\\junit-jupiter-api\\5.10.2\\junit-jupiter-api-5.10.2.jar',
    'org\\junit\\jupiter\\junit-jupiter-engine\\5.10.2\\junit-jupiter-engine-5.10.2.jar',
    'org\\junit\\platform\\junit-platform-commons\\1.10.2\\junit-platform-commons-1.10.2.jar',
    'org\\junit\\platform\\junit-platform-engine\\1.10.2\\junit-platform-engine-1.10.2.jar',
    'org\\opentest4j\\opentest4j\\1.3.0\\opentest4j-1.3.0.jar',
    'org\\apiguardian\\apiguardian-api\\1.1.2\\apiguardian-api-1.1.2.jar',
    'org\\assertj\\assertj-core\\3.26.0\\assertj-core-3.26.0.jar',
    'org\\mockito\\mockito-core\\5.12.0\\mockito-core-5.12.0.jar',
    'net\\bytebuddy\\byte-buddy\\1.14.15\\byte-buddy-1.14.15.jar',
    'net\\bytebuddy\\byte-buddy-agent\\1.14.15\\byte-buddy-agent-1.14.15.jar',
    'org\\objenesis\\objenesis\\3.3\\objenesis-3.3.jar',
    'org\\slf4j\\slf4j-api\\2.0.13\\slf4j-api-2.0.13.jar',
    'ch\\qos\\logback\\logback-classic\\1.5.6\\logback-classic-1.5.6.jar',
    'ch\\qos\\logback\\logback-core\\1.5.6\\logback-core-1.5.6.jar',
    'com\\zaxxer\\HikariCP\\5.1.0\\HikariCP-5.1.0.jar',
    'org\\mariadb\\jdbc\\mariadb-java-client\\3.3.3\\mariadb-java-client-3.3.3.jar',
    'org\\flywaydb\\flyway-core\\10.12.0\\flyway-core-10.12.0.jar',
    'org\\apache\\commons\\commons-csv\\1.11.0\\commons-csv-1.11.0.jar',
    'org\\apache\\pdfbox\\pdfbox\\2.0.32\\pdfbox-2.0.32.jar',
    'org\\apache\\pdfbox\\fontbox\\2.0.32\\fontbox-2.0.32.jar',
    'commons-logging\\commons-logging\\1.2\\commons-logging-1.2.jar',
    'com\\formdev\\flatlaf\\3.5\\flatlaf-3.5.jar'
];

const classpath = [
    'target/classes',
    'target/test-classes',
    'src/test/resources',
    ...jars.map(j => path.join(repoBase, j))
].join(';');

function runCommand(cmd) {
    console.log(`Executing: ${cmd}`);
    try {
        execSync(cmd, { stdio: 'inherit' });
    } catch (e) {
        console.error(`Command failed: ${cmd}`);
        process.exit(1);
    }
}

// 1. Ensure directories exist
if (!fs.existsSync('target/classes')) fs.mkdirSync('target/classes', { recursive: true });
if (!fs.existsSync('target/test-classes')) fs.mkdirSync('target/test-classes', { recursive: true });

// 2. Compile main classes
const mainFiles = findJavaFiles('src/main/java');
fs.writeFileSync('main_files.txt', mainFiles.join(' '));
runCommand(`javac -d target/classes -cp "${classpath}" @main_files.txt`);

// 3. Compile tests
const testFiles = findJavaFiles('src/test/java');
fs.writeFileSync('test_files.txt', testFiles.join(' '));
runCommand(`javac -d target/test-classes -cp "${classpath}" @test_files.txt`);

// 4. Run tests using ManualTestRunner
runCommand(`java -cp "${classpath}" main.java.utils.ManualTestRunner`);

// Cleanup temp files
if (fs.existsSync('main_files.txt')) fs.unlinkSync('main_files.txt');
if (fs.existsSync('test_files.txt')) fs.unlinkSync('test_files.txt');
