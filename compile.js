const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function findJavaFiles(dir) {
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

try {
  // Create classes directory
  if (!fs.existsSync('classes')) {
    fs.mkdirSync('classes', { recursive: true });
  }
  
  // Find all Java files
  const javaFiles = findJavaFiles('src/main/java');
  console.log(`Found ${javaFiles.length} Java files`);
  
  if (javaFiles.length === 0) {
    console.log('No Java files found to compile');
    process.exit(1);
  }
  
  // Compile Java files
  const compileCommand = `javac -d classes -cp "." ${javaFiles.join(' ')}`;
  console.log('Compiling Java files...');
  execSync(compileCommand, { stdio: 'inherit' });
  
  console.log('Compilation successful!');
  
  // Run the main class
  console.log('Running the application...');
  execSync('java -cp classes main.java.Main', { stdio: 'inherit' });
  
} catch (error) {
  console.error('Error:', error.message);
  process.exit(1);
}