# College ERP — Fixed Build

This folder contains a **fixed, buildable** version of your Java Swing ERP with:
- Correct Swing look-and-feel call
- Missing `RowFilter` imports added
- Models made `Serializable` so file persistence works
- Maven-style source tree (`src/main/java`) and Node build helper

## Build & Run (Option A — plain JDK)

```bash
# from this folder
javac -d classes $(find src/main/java -name "*.java")
java -cp classes main.java.Main
```

On Windows (PowerShell):

```powershell
Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName | Set-Content sources.txt
javac -d classes @sources.txt
java -cp classes main.java.Main
```

## Build & Run (Option B — Node helper)

```bash
npm run build-java
npm run run-java
```

## Build & Run (Option C — Maven)

```bash
mvn -q -DskipTests exec:java
```

## Notes
- Data is stored under `data/` next to the runtime; ensure the process has write permission.
- Default login: **admin / admin123**
