# Wave Analyzer

A Java-based application for advanced Wave Intensity Analysis (WIA) of physiological signals. This tool is designed for researchers and clinicians to import, process, and analyze hemodynamic data from various sources.

## 🚀 Features

* **Flexible Data Input:** Imports pressure and flow data from various file formats. It can process data from simultaneous recordings or from different sources that are synchronized using an ECG trace.
* **Wave Intensity Analysis (WIA):** Performs wave intensity calculations according to standard formulae and established methodologies (e.g., Broyd et al.).
* **Savitzky-Golay Filtering:** Includes options for Savitzky-Golay filtering to smooth data and reduce noise prior to analysis.
* **Wave Selection:** Provides the ability to select standard, known physiological waves or define custom wave segments for detailed examination.
* **In-Depth Analysis:** Once waves are identified, the tool performs a comprehensive analysis on them.
* **Text-Based Visualization:** Generates a simple text-based plot of the waveform for quick review in the console.

## 📋 Prerequisites

To build and run this project, you will need:

* **Java Development Kit (JDK)**: Version 17 or higher.
* **Apache Maven**: To manage dependencies and build the project.

## ⚙️ Building the Project

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/YOUR_USERNAME/Wave-Analyzer.git](https://github.com/YOUR_USERNAME/Wave-Analyzer.git)
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd Wave-Analyzer
    ```
3.  **Build the project using Maven:**
    Run the following command from the root directory. This will compile the code, run tests, and package the application into a `.jar` file in the `target/` directory.
    ```bash
    mvn clean install
    ```

## ▶️ How to Run

Once the project is built, you can run the application from the command line.

```bash
java -jar target/Wave-Analyzer-1.0-SNAPSHOT.jar your_data_file
```

*Note: The name of the `.jar` file may vary depending on the version and artifactId defined in your `pom.xml` file.*

**Example:**
If you have a data file named `dataset1.csv` in the root of your project, you would run:

```bash
java -jar target/Wave-Analyzer-1.0-SNAPSHOT.jar dataset1.csv
```

The program will then perform the analysis and print the results to the console.

## 🤝 Contributing

Contributions are welcome! If you have suggestions for improvements or want to add new features, please feel free to:

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📜 License

This project is licensed under the MIT License. See the `LICENSE.md` file for details.

