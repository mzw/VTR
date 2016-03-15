# VTR
Written by [Yuta Maezawa](mailto:maezawa@nii.ac.jp) and greatest contributors

## Get Started

### Configuration
You can configure VTR for your environment.
```
$ make config
```
Then, write your configuration at ```src/main/resources/vtr.properties``
that indicates file has several properties, as follows.
```
# Command
path_to_git = /usr/local/bin/git
path_to_mvn = /usr/local/bin/mvn

# Example
path_to_project = subjects/vtr-example
ref_to_compare	= refs/heads/master
github_username = mzw
github_projname = vtr-example

# Log
path_to_log_dir = log
```

### Compile
```
$ make compile
```

### Run
VTR has two phases: in-advance and in-use.
For validating your test cases, you can ignore the in-advance phase and just run ```$ make validate```.

#### Sophisticate "test-case modifications towards software release" patterns
For this purpose, you can run VTR for collecting and clustering test-case modifications for previously released source programs.
```
$ make detect
$ make cluster
```

Enjoy!

## License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)

## Contributors
* Shunsuke Komuta - Result data analysis
* Kazuyuki Honda - Result data analysis

----
(C) Yuta Maezawa 2016
