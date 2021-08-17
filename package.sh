#!/bin/bash
set -e
echo 'Begin to package job'
BACKEND_MODULES=(job-config job-crontab job-execute job-gateway job-logsvr job-manage job-backup job-file-gateway job-ticket job-file-worker job-analysis)
FRONTEND_MODULES=(job-frontend)
ALL_MODULES=(job-config job-crontab job-execute job-gateway job-logsvr job-manage job-backup job-file-gateway job-ticket job-file-worker job-analysis job-frontend)
JOB_EDITION=ce

if [[ ! -d "release" ]]; then
  mkdir release
else
  rm -rf release/*
fi

usage () {
    cat <<EOF
用法: 
    $PROGRAM [ -h --help -?  查看帮助 ]
            [ -m, --module      [必选] "子模块(${PROJECTS[*]}), 逗号分隔。ALL表示全部都更新" ]
            [ -v, --version    [必选] "Job版本" ]
            [ -e, --edition    [非必选] "Job出包类型，ce表示社区版，ee表示企业版，默认ce" ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# 解析命令行参数，长短混合模式
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do 
    case "$1" in
        -m | --module )
            shift
            JOB_MODULES="$1"
            ;;
        -v | --version )
            shift
            JOB_VERSION="$1"
            ;;
        -e | --edition )
            shift
            JOB_EDITION="$1"
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "不可识别的参数: $1"
            ;;
        *) 
            break
            ;;
    esac
    shift 
done 

JOB_MODULES=${JOB_MODULES,,}          # to lower case
# 判断参数
if [[ -z $JOB_MODULES ]] || ! [[ $JOB_MODULES =~ ^[A-Za-z,-]+$ ]]; then
    warning "-m, --module必须指定要更新的模块名, 逗号分隔: 如job-config,job-logsvr"
fi

# 解析需要更新的模块，如果是all/ALL，则包含所有模块
if [[ $JOB_MODULES = all ]]; then
    PACKAGE_MODULES=("${ALL_MODULES[@]}")
else
    IFS=, read -ra PACKAGE_MODULES <<<"$JOB_MODULES"
fi

function packageJarAndScript()
{
  backend_module="$1"
  log "Packaging $backend_module ..."
  if [[ ! -d "release" ]]; then
    mkdir release
  fi
  
  mkdir -p release/job/backend/${backend_module}/bin

  cp src/backend/release/${backend_module}-${JOB_VERSION}.jar release/job/backend/${backend_module}/${backend_module}.jar
  cp scripts/${backend_module}/${backend_module}.sh release/job/backend/${backend_module}/bin/
  chmod 755 release/job/backend/${backend_module}/bin/${backend_module}.sh
  echo "Package ${backend_module} successfully"
}

# Package the back-end jar file and the corresponding execution script
for m in "${PACKAGE_MODULES[@]}"; do
  for BACKEND_MODULE in ${BACKEND_MODULES[@]}
  do
    [ "$BACKEND_MODULE" == "$m" ] && packageJarAndScript $m
  done
  # Package Upgrader
  if [[ ! -d "release/job/backend" ]]; then
    mkdir -p release/job/backend
  fi
  if [[ -f "src/backend/release/upgrader-${JOB_VERSION}.jar" ]]; then
    cp src/backend/release/upgrader-${JOB_VERSION}.jar release/job/backend/upgrader-${JOB_VERSION}.jar
  fi
done

# Package versionLogs
if [ ! -d "src/frontend/release/job/frontend/static" ] 
then
  mkdir -p src/frontend/release/job/frontend/static
fi
if [ ! -d "release/job/frontend/static" ] 
then
  mkdir -p release/job/frontend/static
fi
cp versionLogs/bundledVersionLog*.json release/job/frontend/static
if [ ! -d "src/frontend/dist/*" ]
then
  mkdir -p src/frontend/dist/_init_
fi

# Package front-end static files
for m in "${PACKAGE_MODULES[@]}"; do
  for FRONTEND_MODULE in ${FRONTEND_MODULES[@]}
  do
    if [[ "$FRONTEND_MODULE" == "$m" ]]; then
      log "Packaging $m ..."
      mkdir -p release/job/frontend
      cp -r src/frontend/dist/* release/job/frontend/
      echo "Package frontend successfully"
    fi
  done
done

# Package support-files
log "Packaging support-files ..."
if [[ ! -d "release/job/support-files" ]]; then
  mkdir -p release/job/support-files
fi
cp -r support-files/bkiam/ release/job/support-files/
cp -r support-files/dependJarInfo/ release/job/support-files/
# Package SQL by modules
if [[ ! -d "release/job/support-files/sql" ]]; then
  mkdir -p release/job/support-files/sql
fi
for m in "${PACKAGE_MODULES[@]}"; do
  if [[ -d "support-files/sql/${m}" ]]; then
    cp -r "support-files/sql/${m}/" release/job/support-files/sql/
  fi
done
# Package Templates by modules
if [[ -d "support-files/templates" ]]; then
  if [[ ! -d "release/job/support-files/templates" ]]; then
    mkdir -p release/job/support-files/templates
  fi
  for m in "${PACKAGE_MODULES[@]}"; do
    if [[ "job-frontend" == "${m}" ]]; then
      continue
    fi
    simpleName=${m:4}
    # Copy yml templates
    propertiesFilePath="support-files/templates/#etc#job#job-${simpleName}#job-${simpleName}.yml"
    if [[ -f "${propertiesFilePath}" ]]; then
      cp "${propertiesFilePath}" release/job/support-files/templates
    else
      if [[ "${simpleName}" != "config" && "${simpleName}" != "file-worker" ]];then
        echo "cannot find properties template of job-${simpleName}"
        exit 1
      fi
    fi
  done
  # Copy upgrader.properties
  upgraderPropertiesFile="support-files/templates/#etc#job#upgrader#upgrader.properties"
  if [[ -f "${upgraderPropertiesFile}" ]]; then
    cp "${upgraderPropertiesFile}" release/job/support-files/templates
  else
    echo "warn: cannot find ${upgraderPropertiesFile}, ignore"
  fi
  # Copy job.env
  jobEnvFile="support-files/templates/job.env"
  if [[ -f "${jobEnvFile}" ]]; then
    cp "${jobEnvFile}" release/job/support-files/templates
  else
    echo "warn: cannot find ${jobEnvFile}, ignore"
  fi
fi
# readme.md、requirements.txt
for fileName in "readme.md" "requirements.txt";
do
filePath="support-files/${fileName}"
if [[ -f "${filePath}" ]]; then
  cp "${filePath}" release/job/support-files/
fi
done
echo "Package support-files successfully"

# Package project documents
log "Packaging project doc ..."
cp README.md release/job/
cp projects.yaml release/job/
cp release.md release/job/
cp UPGRADE.md release/job/
cp VERSION release/job/
cp -r docs/ release/job/
echo "Package project doc successfully"

cd release

# 企业版、社区版包名称差异处理
if [[ "${JOB_EDITION}" == "ee" ]];then
  tar -czf "job_ee-${JOB_VERSION}.tgz" job
  log "Package job successfully! File: job_ee-${JOB_VERSION}.tgz"
else
  tar -czf "job_ce-${JOB_VERSION}.tgz" job
  log "Package job successfully! File: job_ce-${JOB_VERSION}.tgz"
fi

set +e
