# VTR
Written by [Yuta Maezawa](mailto:maezawa@nii.ac.jp) and greatest contributors

## Get Started


### Configuration
Need to describe local configurations at `src/main/resources/config.properties`.
```
path_to_output_dir = output
maven_home = /usr/local/apache-maven-3.3.*
```

Additionally, need to configure environmental variables below.
- JAVA_HOME

### Compile
```
$ mvn compile test-compile dependency:copy-dependencies
```

### Run

#### Make Dictionary
```
$ sh/run dict <project ID> <path to project> <reference to compare branch>
```

### Measure Coverage
```
$ sh/run cov <project ID> <path to project>
```

### Detect Test-Case Modifications
```
$ sh/run detect <project ID> <path to project>
```

Enjoy!

## License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

## Contributors
* Shunsuke Komuta - Result data analysis
* Kazuyuki Honda - Result data analysis

----
(C) Yuta Maezawa 2016
