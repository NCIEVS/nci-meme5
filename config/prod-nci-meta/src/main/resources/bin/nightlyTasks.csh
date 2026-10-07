#!/bin/tcsh -f
#
# Nightly MEME automation:
# 1. Always run the Daily Editing Report.
# 2. If automations are enabled, regenerate MUTUALLY_EXCLUSIVE workflow bins.
# 3. If automations are enabled, optionally restart the application service.

# Common environment is inherited from /local/content/MEME/MEME5/ncim/setenv.sh.
# If the script is run directly, bootstrap through bash because setenv.sh uses
# bash syntax that tcsh cannot source directly.
if (! $?APP_DIR) then
  set production_setenv = "/local/content/MEME/MEME5/ncim/setenv.sh"
  if ($?SETENV) then
    if ("$SETENV" != "") set production_setenv = "$SETENV"
  endif

  if (! $?MEME_SETENV_BOOTSTRAPPED) then
    if (-r "$production_setenv") then
      setenv MEME_SETENV_BOOTSTRAPPED 1
      exec /bin/bash -c 'set -a && source "$1" && set +a && exec /bin/tcsh -f "$2"' bash "$production_setenv" "$0"
    endif
  endif

  echo "ERROR: APP_DIR must be set; source the production setenv.sh first."
  exit 1
endif
if (! $?DB_HOST) then
  echo "ERROR: DB_HOST must be set; source the production setenv.sh first."
  exit 1
endif
if (! $?DB_PORT) then
  echo "ERROR: DB_PORT must be set; source the production setenv.sh first."
  exit 1
endif
if (! $?DB_NAME) then
  echo "ERROR: DB_NAME must be set; source the production setenv.sh first."
  exit 1
endif
if (! $?DB_USER) then
  echo "ERROR: DB_USER must be set; source the production setenv.sh first."
  exit 1
endif
if (! $?BASE_URL) then
  echo "ERROR: BASE_URL must be set; source the production setenv.sh first."
  exit 1
endif

# Script configuration
if (! $?MYSQL_BIN) set MYSQL_BIN = "mysql"
if (! $?CURL_BIN) set CURL_BIN = "curl"
if (! $?ADMIN_USER) set ADMIN_USER = "admin"
if (! $?ADMIN_PASSWORD) set ADMIN_PASSWORD = "admin"
if (! $?APP_SERVICE) set APP_SERVICE = "nci-meme5"
if (! $?RESTART_SERVER_AFTER_NIGHTLY) set RESTART_SERVER_AFTER_NIGHTLY = "true"
if (! $?NIGHTLY_REPORT_PROCESS_NAME) set NIGHTLY_REPORT_PROCESS_NAME = "Daily Editing Report"
if (! $?NIGHTLY_WORKFLOW_PROGRESS_BINS) set NIGHTLY_WORKFLOW_PROGRESS_BINS = '["demotions","norelease","reviewed","ncithesaurus","icd10","icdo","meddra","medrt","radlex","snomedct_us","leftovers"]'
if (! $?NIGHTLY_REPORT_POLL_SECONDS) set NIGHTLY_REPORT_POLL_SECONDS = 10
if (! $?NIGHTLY_REPORT_MAX_POLLS) set NIGHTLY_REPORT_MAX_POLLS = 720

if ($?DB_PASSWORD) then
  if ("$DB_PASSWORD" != "") then
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASSWORD" "$DB_NAME" )
  else
    set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
  endif
else
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
endif
set curl = ( "$CURL_BIN" -sS )

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"
echo "APP_DIR = $APP_DIR"
echo "DB_NAME = $DB_NAME"
echo "BASE_URL = $BASE_URL"

set automationsEnabled = `echo "select if(automationsEnabled,'true','false') from projects;" | $mysql | tail -1`
set projectId = `echo "select id from projects;" | $mysql | tail -1`

echo "project: $projectId"
echo "automations enabled: $automationsEnabled"
echo ""

echo "  Login ... `/bin/date`"
set authToken = `$curl -H "Content-type: text/plain" -X POST -d "$ADMIN_PASSWORD" "$BASE_URL/security/authenticate/$ADMIN_USER" | perl -pe 's/.*"authToken":"([^"]*).*/$1/;'`
if ("$authToken" == "") then
  echo "ERROR: authentication failed"
  exit 1
endif

echo "  Run $NIGHTLY_REPORT_PROCESS_NAME... `/bin/date`"
set processId = `echo "select id from process_configs where name='$NIGHTLY_REPORT_PROCESS_NAME';" | $mysql | tail -1`
echo "    processId = $processId"
if ("$processId" == "") then
  echo "ERROR: process config not found: $NIGHTLY_REPORT_PROCESS_NAME"
  exit 1
endif
set executionId = `$curl -H "Content-type: application/json" -H "Authorization: $authToken" -X GET "$BASE_URL/process/config/$processId/prepare?projectId=$projectId"`
if ("$executionId" == "") then
  echo "ERROR: could not prepare $NIGHTLY_REPORT_PROCESS_NAME"
  exit 1
endif
echo "    executionId = $executionId"
sleep 2
set startedExecutionId = `$curl -H "Content-type: application/json" -H "Authorization: $authToken" -X GET "$BASE_URL/process/execution/$executionId/execute?projectId=$projectId&background=true"`
if ("$startedExecutionId" == "") then
  echo "ERROR: could not start $NIGHTLY_REPORT_PROCESS_NAME execution $executionId"
  exit 1
endif

echo "  Wait for $NIGHTLY_REPORT_PROCESS_NAME execution $executionId... `/bin/date`"
set reportState = "RUNNING"
set reportPolls = 0
while ("$reportState" == "RUNNING")
  sleep "$NIGHTLY_REPORT_POLL_SECONDS"
  set reportState = `echo "select case when finishDate is not null and failDate is null then 'COMPLETE' when failDate is not null then 'FAILED' when stopDate is not null then 'STOPPED' else 'RUNNING' end from process_executions where id=$executionId;" | $mysql | tail -1`
  @ reportPolls = $reportPolls + 1
  echo "    report state: $reportState"
  if ($reportPolls >= $NIGHTLY_REPORT_MAX_POLLS) then
    echo "ERROR: timed out waiting for $NIGHTLY_REPORT_PROCESS_NAME execution $executionId"
    exit 1
  endif
end

if ("$reportState" != "COMPLETE") then
  echo "ERROR: $NIGHTLY_REPORT_PROCESS_NAME execution $executionId finished with state $reportState"
  exit 1
endif

if ("$automationsEnabled" == "true") then
  echo "  Regenerate MUTUALLY_EXCLUSIVE ... `/bin/date`"
  $curl -H "Content-type: application/json" -H "Authorization: $authToken" -d "" "$BASE_URL/workflow/bin/regenerate/all?projectId=$projectId&type=MUTUALLY_EXCLUSIVE"

  set binsLeft = 10
  while ($binsLeft != 0)
    set binsLeft = `$curl -H "Content-type: application/json" -H "Authorization: $authToken" -d "$NIGHTLY_WORKFLOW_PROGRESS_BINS" "$BASE_URL/workflow/lookup/progress/bulk?projectId=$projectId" | jq -r '.totalCount'`
    echo "bins left: $binsLeft"
    sleep 10
  end

  if ("$RESTART_SERVER_AFTER_NIGHTLY" == "true") then
    sudo systemctl stop "$APP_SERVICE"
    if ($status != 0) then
      echo "ERROR: could not stop $APP_SERVICE"
      exit 1
    endif

    sudo systemctl start "$APP_SERVICE"
    if ($status != 0) then
      echo "ERROR: could not start $APP_SERVICE"
      exit 1
    endif
  else
    echo "  Skipping restart because RESTART_SERVER_AFTER_NIGHTLY=$RESTART_SERVER_AFTER_NIGHTLY"
  endif
else
  echo "  Skipping workflow-bin regeneration and restart because automations are disabled."
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
