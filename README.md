# Marathon Runner Overview

This system runs your solutions against a set of tests and displays the results on a local web dashboard.

## Folder Structure

- **solutions/** — Place your compiled solution binaries here.
- **tests/** — Place your test files (with a `.txt` extension) here.
- **results/** — CSV files with test results are saved here automatically.

## How It Works

1. **Runner**
    - Executes a specified solution on all test files.
    - Reads the solution's stderr for lines like `Score = ...` and `Time = ... ms`.
    - Writes the results to a CSV file in the **results/** folder.
    - **Run Example:**  
      `java Runner solutionName.exe`

2. **Web Server**
    - Reads all CSV files from the **results/** folder.
    - Computes relative scores (supports both maximization and minimization modes via a dropdown).
    - Displays a dashboard at [http://localhost:8080](http://localhost:8080).

## Quick Usage

1. Add your test files to **tests/**.
2. Add your solution binaries to **solutions/**.
3. Run the Runner to generate results:  
   `java Runner solutionName.exe`
4. Start the Web Server:  
   `java WebServer`
5. Open [http://localhost:8080](http://localhost:8080) in your browser to view the dashboard.

## Running from command line

This project includes several shell scripts to facilitate building and running the application:

- **`build.sh`**: Compiles the Java source files and prepares the output directory.
- **`run.sh`**: Executes the specified solution against the test files.
- **`start_web_server.sh`**: Launches the web server to display the results dashboard.

You can run these scripts from the command line to streamline your workflow.
