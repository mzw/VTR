#!/bin/sh
BASE_DIR=`pwd`
SUBJECT_DIR="subjects"

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

if [ ! -d ${BASE_DIR}/${SUBJECT_DIR} ]; then
  echo "Not found: ${BASE_DIR}/${SUBJECT_DIR}"
  exit 1
fi
for subject in "${subjects[@]}"
do
  if [ ! -d ${BASE_DIR}/${SUBJECT_DIR}/${subject} ] ; then
    cd ${SUBJECT_DIR}
    echo "git clone ${subject}"
    git clone https://github.com/apache/${subject}
    cd ${BASE_DIR}
  fi
done
