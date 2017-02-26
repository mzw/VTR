#!/bin/sh

#OUTPUT="/Users/yuta/Desktop/20170220-repair"
OUTPUT="/Users/yuta/docker-share/VTR/output-for-validator"
mode="improved-version2"

declare -a subjects=()
subjects=("${subjects[@]}" "commons-lang")
subjects=("${subjects[@]}" "commons-math")
subjects=("${subjects[@]}" "commons-bcel")
subjects=("${subjects[@]}" "commons-beanutils")
subjects=("${subjects[@]}" "commons-bsf")
subjects=("${subjects[@]}" "commons-chain")
subjects=("${subjects[@]}" "commons-cli")
subjects=("${subjects[@]}" "commons-codec")
subjects=("${subjects[@]}" "commons-collections")
subjects=("${subjects[@]}" "commons-compress")
subjects=("${subjects[@]}" "commons-configuration")
subjects=("${subjects[@]}" "commons-crypto")
subjects=("${subjects[@]}" "commons-csv")
subjects=("${subjects[@]}" "commons-dbcp")
subjects=("${subjects[@]}" "commons-dbutils")
subjects=("${subjects[@]}" "commons-digester")
subjects=("${subjects[@]}" "commons-discovery")
subjects=("${subjects[@]}" "commons-email")
subjects=("${subjects[@]}" "commons-exec")
subjects=("${subjects[@]}" "commons-fileupload")
subjects=("${subjects[@]}" "commons-functor")
subjects=("${subjects[@]}" "commons-imaging")
subjects=("${subjects[@]}" "commons-io")
subjects=("${subjects[@]}" "commons-jcs")
subjects=("${subjects[@]}" "commons-jexl")
subjects=("${subjects[@]}" "commons-jxpath")
subjects=("${subjects[@]}" "commons-logging")
subjects=("${subjects[@]}" "commons-net")
subjects=("${subjects[@]}" "commons-pool")
subjects=("${subjects[@]}" "commons-proxy")
subjects=("${subjects[@]}" "commons-rng")
subjects=("${subjects[@]}" "commons-scxml")
subjects=("${subjects[@]}" "commons-validator")

declare -a files=()
files=("${files[@]}" "mutation/results.csv")
#files=("${files[@]}" "performance/results.csv")
#files=("${files[@]}" "output/compile.csv")
#files=("${files[@]}" "output/runtime.csv")
#files=("${files[@]}" "output/javadoc.csv")
#files=("${files[@]}" "readability/results.csv")

for subject in "${subjects[@]}"
do
	for file in "${files[@]}"
	do
		#php sh/slack.php VTR "Start: eval ${mode} on ${subject} at ${machine}"
		echo "sh/run eval ${mode} ${OUTPUT}/${subject}/repair/${file}"
		sh/run eval ${mode} ${OUTPUT}/${subject}/repair/${file} ${subject}
		mv ~/Desktop/*.csv ~/Desktop/output/${subject}
		#php sh/slack.php VTR "Finish: eva ${mode} on ${subject} at ${machine}"
	done
done
