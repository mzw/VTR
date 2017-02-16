# VTR

Written by [Yuta Maezawa](mailto:maezawa@nii.ac.jp) and greatest contributors

## Configuration

Provide your local configurations at `src/main/resources/config.properties` .

```
path_to_output_dir = output
path_to_subjects_dir = subjects
maven_home = /usr/local/apache-maven-3.3.9
maven_output = false
```

## Clone
Make `output` and `subjects` directories and clone your project under the `subjects` directory.

```
$ mkdir output
$ mkdir subjects
$ cd subjects
$ git clone https://github.com/mzw/vtr-example
```

## Compile

```
$ mvn compile test-compile dependency:copy-dependencies
```

## Run

### In-Advance Phase

#### 1. Make Dictionary

```
$ sh/run dict vtr-example
```

#### 2.  Measure Coverage of Modified Test Cases

```
$ sh/run cov vtr-example
```

#### 3. Detect Test-Case Modifications for Previously-Released Source Code

```
$ sh/run detect vtr-example
```

### In-Review Phase

#### 4. Cluster Detected Test-Case Modifications

```
$ sh/run cluster lcs complete 0.5
```

#### 5. Review VTR In-Advance Results

```
$ sh/run visualize html
```

### In-Use Phase

#### 6. Validate Test Cases to Be Modified

```
$ sh/run validate vtr-example
```

#### 7. Generate Patches to Modify Test Cases Detected

```
$ sh/run gen vtr-example
```

#### 8. Evaluate Improvement of Modified Test Cases

```
$ sh/run repair vtr-example
```

Enjoy!

## License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

----
(C) Yuta Maezawa 2017
