# Code Analysis and Unit Test Reports Guide

This guide explains how to view code analysis and unit test execution reports after GitHub Actions workflows are executed.

## 📊 Available Reports

Your project generates several types of reports during CI/CD execution:

### 1. Test Coverage Reports (JaCoCo)
- **HTML Report**: Interactive coverage visualization
- **XML Report**: Machine-readable format for CI integration
- **Coverage Metrics**: Line, branch, and complexity coverage

### 2. Static Analysis Reports
- **PMD**: Code quality and style analysis
- **SpotBugs**: Bug detection and security analysis

## 🔍 How to View Reports

### Method 1: GitHub Actions Artifacts (Recommended)

1. **Navigate to your repository on GitHub**
2. **Click the "Actions" tab**
3. **Select the workflow run** (e.g., "Dev-Test")
4. **Scroll down to the "Artifacts" section**
5. **Download the artifacts**:
   - `test-coverage-reports` - JaCoCo coverage reports
   - `static-analysis-reports` - PMD and SpotBugs reports
   - `build-reports` - All build reports including summary

### Method 2: Pull Request Comments

For pull requests, the workflow automatically posts a summary comment with:
- ✅ Test coverage status
- 🔍 Static analysis results
- 📋 Links to download artifacts
- 🔗 Quick links to workflow runs

### Method 3: Workflow Logs

View detailed results in the workflow execution logs:
1. **Go to the workflow run**
2. **Click on the "build" job**
3. **Expand the "Display comprehensive results" step**
4. **View real-time coverage and analysis metrics**

## 📁 Report Locations

### Local Development
After running `./gradlew test jacocoTestReport`, reports are available at:
```
build/reports/
├── jacoco/
│   └── test/
│       ├── html/index.html          # Interactive coverage report
│       └── jacocoTestReport.xml     # XML coverage data
├── tests/
│   └── test/                        # Test execution reports
└── coverage-summary.md              # Generated summary
```

### CI/CD Artifacts
Reports are uploaded as artifacts with the following structure:
```
test-coverage-reports/
├── jacoco/
│   └── test/
│       ├── html/index.html
│       └── jacocoTestReport.xml
└── tests/
    └── test/

static-analysis-reports/
├── spotbugs.xml
└── pmd/
    ├── pmd-report.txt
    └── pmd-report.xml

build-reports/
├── coverage-summary.md
└── [all reports combined]
```

## 📈 Understanding Coverage Reports

### JaCoCo Coverage Metrics

1. **Line Coverage**: Percentage of lines executed
2. **Branch Coverage**: Percentage of branches executed
3. **Complexity Coverage**: Cyclomatic complexity coverage

### Coverage Thresholds
- **Line Coverage**: Minimum 80%
- **Branch Coverage**: Minimum 70%

### Viewing HTML Reports
1. **Download the `test-coverage-reports` artifact**
2. **Extract the archive**
3. **Open `jacoco/test/html/index.html` in your browser**
4. **Navigate through packages and classes to see detailed coverage**

## 🔍 Understanding Static Analysis Reports

### PMD Analysis
- **Code Quality**: Identifies code smells and style issues
- **Best Practices**: Enforces coding standards
- **Performance**: Detects inefficient patterns

### SpotBugs Analysis
- **Bug Detection**: Finds potential bugs and security issues
- **Security**: Identifies security vulnerabilities
- **Performance**: Detects performance problems

## 🚀 Integration with External Tools

### SonarQube Integration
The XML reports can be integrated with SonarQube:
```yaml
# In your SonarQube configuration
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
```

### GitHub Code Scanning
Reports can be used with GitHub Advanced Security:
- Upload SpotBugs results as SARIF
- Integrate PMD findings
- Track coverage trends

## 📊 Monitoring and Trends

### Coverage Trends
- Track coverage over time
- Set up alerts for coverage drops
- Monitor new code coverage

### Quality Gates
- Configure minimum coverage thresholds
- Set up quality gates for pull requests
- Monitor static analysis findings

## 🔧 Troubleshooting

### Common Issues

1. **Reports not generated**
   - Check if tests are running successfully
   - Verify JaCoCo plugin is configured
   - Ensure build completes without errors

2. **Artifacts not available**
   - Check workflow permissions
   - Verify artifact upload steps
   - Ensure workflow completes successfully

3. **Coverage metrics missing**
   - Check JaCoCo configuration
   - Verify test execution
   - Review build logs for errors

### Debugging Steps

1. **Check workflow logs** for error messages
2. **Verify artifact upload** steps completed
3. **Review build configuration** for report generation
4. **Test locally** with `./gradlew test jacocoTestReport`

## 📋 Best Practices

1. **Regular Review**: Check reports after each build
2. **Coverage Goals**: Maintain minimum coverage thresholds
3. **Quality Gates**: Use reports to gate deployments
4. **Documentation**: Keep this guide updated
5. **Automation**: Set up automated reporting

## 🔗 Quick Reference

### Local Commands
```bash
# Run tests with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html

# Run static analysis
./gradlew spotlessApply
```

### GitHub Actions
- **Workflow**: `.github/workflows/api-dev-workflow.yml`
- **Artifacts**: Available after successful runs
- **Comments**: Automatic PR summaries
- **Logs**: Detailed execution logs

### Report Locations
- **HTML Coverage**: `build/reports/jacoco/test/html/index.html`
- **XML Coverage**: `build/reports/jacoco/test/jacocoTestReport.xml`
- **PMD Reports**: `build/reports/pmd/`
- **SpotBugs Reports**: `build/reports/spotbugs.xml` 