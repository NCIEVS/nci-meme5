#!/usr/bin/env bash

set -euo pipefail

usage="getMaintHtml.sh {start|stop} {environment}"

if [[ "$#" -ne 2 ]]; then
  echo "ERROR: Wrong number of parameters"
  echo "usage: $usage"
  exit 1
fi

action="$1"
environment="$2"

maint_file="${MAINT_FILE:-maintain.html}"
staged_file="${MAINT_FILE_PREFIX:-maintain}.${environment}.html"
stage_root="${MAINT_STAGE_ROOT:-/home/ec2-tomcat}"
stage_dir="${MAINT_STAGE_DIR:-$stage_root/$environment/config}"
maint_dir="${MAINT_DIR:-/opt/maint}"

echo "Action = $action"
echo "Environment = $environment"
echo "Stage dir = $stage_dir"
echo "Maint dir = $maint_dir"

case "$action" in
  start)
    /bin/cp -f "$stage_dir/$maint_file" "$maint_dir/$staged_file"
    ;;
  stop)
    rm -f "$maint_dir/$staged_file"
    ;;
  *)
    echo "ERROR: action must be start or stop"
    echo "usage: $usage"
    exit 1
    ;;
esac
