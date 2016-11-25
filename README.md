# VTR
Written by [Yuta Maezawa](mailto:maezawa@nii.ac.jp) and greatest contributors

## Get Started

### Configuration
Provide your local configurations at `src/main/resources/config.properties`.
```
path_to_output_dir = output
path_to_subjects_dir = subjects
maven_home = /usr/local/apache-maven-3.3.9
maven_output = false
```

### Clone
Clone your project under the `subjects` directory.
```
$ cd subjects
$ git clone https://github.com/mzw/vtr-example
```

### Compile
```
$ mvn compile test-compile dependency:copy-dependencies
```

### Run

#### Make Dictionary
```
$ sh/run dict vtr-example
```

### Measure Coverage of Modified Test Cases
```
$ sh/run cov vtr-example
```

### Detect Test-Case Modifications for Previously-Released Source Code
```
$ sh/run detect vtr-example
```

### Cluster Detected Test-Case Modifications
```
$ sh/run cluster lcs complete 0.5
```

### Review VTR In-Advance Results
```
$ sh/run visualize html
```

Enjoy!

## License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

----
(C) Yuta Maezawa 2016
