# Wave Analyzer

A Java-based application for advanced Wave Intensity Analysis (WIA) of physiological signals. This tool is designed for researchers and clinicians to import, process, and analyze hemodynamic data from various sources.

## Features

* **Flexible Data Input:** Imports pressure and flow data from multiple file formats. Supports simultaneous recordings or synchronized data from different sources using an ECG trace.
* **Wave Intensity Analysis (WIA):** Performs calculations using standard formulae and established methodologies (e.g., Broyd et al.).
* **Savitzky-Golay Filtering:** Provides options for smoothing and noise reduction before analysis.
* **Wave Selection:** Allows selection of standard physiological waves or custom wave segments for detailed examination.
* **In-Depth Analysis:** Generates comprehensive results once waves are identified.
* **Text-Based Visualization:** Creates a simple text-based plot of the waveform for quick console review.


## Getting Started

Ensure you have **Java 8 or later** installed (available from [Java website](https://www.java.com/en/download/)). The user account on your computer must have read/write access privileges.  

You can either **download a pre-built release** of this program, or **build it yourself**. Make sure to place the program in its own folder, as it will create configuration files in the same directory.

Upon starting the program, you will be guided through prompts for WIA. There are hints (blue question marks) in the interface to provide additional guidance.

## Download

Download the latest release from the *Releases* section. The `.jar` file can be run in several ways.


### macOS

On the latest version of Java for macOS, double-click the `.jar` file to run the application. Because this is a self-built, open-source project that has not gone through Apple’s notarization process, macOS may display a warning. This is expected for software distributed outside the App Store. You may need to allow the program under **System Settings > Privacy & Security**.

If the program does not run with double-click, you will need to run it from the command line (**Terminal**) or create a launch script. 

**Command Line**
1.  Open **Terminal** (**Applications > Utilities > Terminal**)
2.  Open the `.jar` file in **Finder**
3.  In **Terminal**, type the `java -jar ` (including space at the end) and drag the `.jar` file in **Finder** into the **Terminal** window.
4.  Press *Enter* to run.

**Script**
1.  In the same folder as the `.jar` file, use the **TextEdit** application (or any other text editor) to create a new file. Paste the following code and save the file as `run.command`. This should be saved in plain text (in TextEdit, select Format > Make Plain Text).

	```bash
	#!/bin/bash
	DIR="$(cd "$(dirname "$0")" && pwd)"
	exec java -jar "$DIR/WaveAnalyzer.jar"
	```

2.  Make this script executable. Open **Terminal**. Type `cd ` (including the trailing space), and drag the folder containing `run.command` into the **Terminal** window. It will display the file path. Press *Enter*. Then, type `chmod +x run.command`. 

3.  The script should be executable if your user has privileges

4.  **Double-click** (or **right-click > Open**) the `run.command` file to run


### Windows

On windows, if Java is installed correctly the program can be run by opening the `.jar` file itself (i.e. double-click). If it doesn't open, you may try right-clicking, selecting "Open with," and then "Choose another app," and selecting Java. Using a tool like Jarfix can also help to repair file association (i.e. so that Windows knows how to open the JAR file). 

If these options do not work, you may run directly from the **Command Prompt** which can be opened by pressing `Win + R`, type `cmd`, and press Enter. Type `cd` and drag the folder containing the `jar` file into the Command Prompt window and press Enter to navigate to the folder. Then, type the following and press Enter to run.

    ```bash
    java -jar WaveAnalyzer.jar
    ```

## Building it yourself

The benefit to building the program yourself is that you may create a working program from the most recent repository commits, or clone this repository and make edits of your own. It may be built using [Maven](https://maven.apache.org/). 

1. **Clone the repository:**

    ```bash
    git clone https://github.com/JCarring/WaveAnalyzer.git
    ```
    
2. **Navigate to the project directory:**

    ```bash
    cd /path/to/WaveAnalyzer
    ```

3. **Build the project using Maven:** Run the following command from the root directory. This will compile the code, run tests, and package the application into a `.jar` file in the `target/` directory.

    ```bash
    mvn clean package
    ```

## Contributing

Contributions are welcome! If you have suggestions for improvements or want to add new features, please feel free to fork the project.


## License

This project is licensed under the MIT License. Developed by Justin Carrington, MD, and Claire Raphael, MBBS, Ph.D. See the `LICENSE.md` file for details.

