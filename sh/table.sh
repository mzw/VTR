#!/bin/sh

OUTPUT_BASE="/Users/yuta/Desktop/output/"

declare -a subjects=()
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
subjects=("${subjects[@]}" "commons-lang")
subjects=("${subjects[@]}" "commons-logging")
subjects=("${subjects[@]}" "commons-math")
subjects=("${subjects[@]}" "commons-net")
subjects=("${subjects[@]}" "commons-pool")
subjects=("${subjects[@]}" "commons-proxy")
subjects=("${subjects[@]}" "commons-rng")
subjects=("${subjects[@]}" "commons-scxml")
subjects=("${subjects[@]}" "commons-validator")

declare -a files=()
files=("${files[@]}" "mutation_improve.csv")
files=("${files[@]}" "performance_improve.csv")
files=("${files[@]}" "compile_improve.csv")
files=("${files[@]}" "runtime_improve.csv")
files=("${files[@]}" "javadoc_improve.csv")
files=("${files[@]}" "readability_improve.csv")

for subject in "${subjects[@]}"
do
	for file in "${files[@]}"
	do
		find ${OUTPUT_BASE}/${subject} -name *${file} | xargs awk 'NR%2==0' | sed -e "s/^,//" | awk -F"," '{print $1, $2}' | sed -e "s/ / \& /g" | perl -pe "s/\n/ \& /"  >> table.txt
	done
	echo "\n" >> table.txt
done
