#!/bin/sh

COMMAND=${1}
UPLOAD=${2}
echo ${COMMAND}

if [ ! ${COMMAND} = "dict" ] && [ ! ${COMMAND} = "cov" ] && [ ! ${COMMAND} = "detect" ] && [ ! ${COMMAND} = "all" ]; then
  echo "Illegal command"
  exit 1
fi

DATE="20171201"
OUTPUT_DIR="output"

declare -a subjects=()
subjects=("${subjects[@]}" "commons-bcel")
subjects=("${subjects[@]}" "commons-lang")
subjects=("${subjects[@]}" "commons-math")
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

# all
if [ ! ${COMMAND} = "all" ]; then
  for subject in "${subjects[@]}"
  do
    echo "sh/run ${COMMAND} ${subject}"
    sh/run ${COMMAND} ${subject}
    echo "finished ${COMMAND} ${subject}"
  done
else
  for subject in "${subjects[@]}"
  do
    for com in "dict" "cov" "detect"
    do
      echo "sh/run ${com} ${subject}"
      sh/run ${com} ${subject}
      echo "finished ${com} ${subject}"
    done
    aws --profile mzw s3 cp --recursive --dryrun ${OUTPUT_DIR}/${subject} s3://vtr.mzw.jp/${DATE}/results/${subject}
  done
fi

# upload
if [ ${#} -eq 2 ] && [ ${UPLOAD} = "true" ]; then
  echo "upload the ${COMMAND} result ${subject}"
  aws --profile mzw s3 cp --recursive --dryrun ${OUTPUT_DIR}/${subject} s3://vtr.mzw.jp/${DATE}/results/${subject}
fi
