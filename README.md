# VTR [![Build Status](https://travis-ci.org/mzw/VTR.svg?branch=master)](https://travis-ci.org/mzw/VTR)

Written by [Yuta Maezawa](mailto:maezawa@nii.ac.jp) and greatest contributors

## Configuration

Provide your local configurations at `src/main/resources/config.properties` .

```
path_to_output_dir = outputs
path_to_subjects_dir = subjects
maven_home = /usr/local/apache-maven-3.3.9
maven_output = false
```

## Clone
Make `outputs` and `subjects` directories and clone your project under the `subjects` directory.

```
$ mkdir outputs
$ mkdir subjects
$ cd subjects
$ git clone https://github.com/mzw/vtr-example
```

## Compile

```
$ mvn compile test-compile dependency:copy-dependencies
```

## Run

### 1. In-Advance Phase

#### 1-1. Make Dictionary

```
$ sh/run dict vtr-example
```

#### 1-2.  Measure Coverage of Modified Test Cases

```
$ sh/run cov vtr-example
```

#### 1-3. Detect Test-Case Modifications for Previously-Released Source Code

```
$ sh/run detect vtr-example
```

### 2. In-Review Phase

#### 2-1. Classify Detected Test-Case Modifications by GumTreeDiff

```
$ sh/run cluster gumtreediff
```

#### 2-2. Classify Detected Test-Case Modifications by Testedness

```
$ sh/run cluster testedness
```

##### (Optional) Adding Manual Defined Modification Patterns

```
$ sh/run cluster add-patterns-for-testedness
```

#### 2-3. Review VTR In-Advance Results

```
$ sh/run visualize html
```

### 3. In-Use Phase

#### 3-1. Validate Test Cases to Be Modified

```
$ sh/run validate vtr-example
```

#### 3-2. Generate Patches to Modify Test Cases Detected

```
$ sh/run gen vtr-example
```

#### 3-3. Evaluate Improvement of Modified Test Cases

```
$ sh/run repair vtr-example
```

Enjoy!

## License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

----
(C) Yuta Maezawa 2017
