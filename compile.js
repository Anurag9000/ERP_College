const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const M2_REPO = 'C:/Users/DELL/.m2/repository';

function findJavaFiles(dir) {
  const files = [];
  try {
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
  } catch (e) { }
  return files;
}

function findJar(group, artifact, version) {
  const groupPath = group.replace(/\./g, '/');
  const dir = path.join(M2_REPO, groupPath, artifact, version);
  if (fs.existsSync(dir)) {
    const files = fs.readdirSync(dir);
    const jar = files.find(f => f.endsWith('.jar') && !f.endsWith('-sources.jar') && !f.endsWith('-javadoc.jar'));
    if (jar) return path.join(dir, jar);
  }
  return null;
}

const dependencies = [
  { g: 'org.slf4j', a: 'slf4j-api', v: '2.0.13' },
  { g: 'ch.qos.logback', a: 'logback-classic', v: '1.5.6' },
  { g: 'ch.qos.logback', a: 'logback-core', v: '1.5.6' },
  { g: 'com.zaxxer', a: 'HikariCP', v: '5.1.0' },
  { g: 'org.mariadb.jdbc', a: 'mariadb-java-client', v: '3.3.3' },
  { g: 'org.flywaydb', a: 'flyway-core', v: '10.12.0' },
  { g: 'com.formdev', a: 'flatlaf', v: '3.5' },
  { g: 'org.apache.pdfbox', a: 'pdfbox', v: '2.0.32' },
  { g: 'org.apache.pdfbox', a: 'fontbox', v: '2.0.32' },
  { g: 'org.apache.commons', a: 'commons-csv', v: '1.11.0' },
  { g: 'com.google.code.gson', a: 'gson', v: '2.10.1' },
  { g: 'com.fasterxml.jackson.core', a: 'jackson-core', v: '2.15.2' },
  { g: 'com.fasterxml.jackson.core', a: 'jackson-databind', v: '2.15.2' },
  { g: 'com.fasterxml.jackson.core', a: 'jackson-annotations', v: '2.15.2' }
];

try {
  if (!fs.existsSync('classes')) {
    fs.mkdirSync('classes', { recursive: true });
  }

  const javaFiles = findJavaFiles('src/main/java');
  console.log(`Found ${javaFiles.length} Java files`);

  const jars = dependencies.map(d => findJar(d.g, d.a, d.v)).filter(Boolean);
  console.log(`Found ${jars.length} of ${dependencies.length} required JARs in .m2 repo`);

  const cp = ['src/main/java', 'src/main/resources', 'classes', '.', ...jars].join(';');
  const compileCommand = `javac -d classes -cp "${cp}" ${javaFiles.join(' ')}`;

  console.log('Compiling Java files...');
  execSync(compileCommand, { stdio: 'inherit' });
  console.log('Compilation successful!');

  console.log('Running the application...');
  execSync(`java -cp "${cp}" main.java.Main`, { stdio: 'inherit' });

} catch (error) {
  console.error('Error:', error.message);
  process.exit(1);
}