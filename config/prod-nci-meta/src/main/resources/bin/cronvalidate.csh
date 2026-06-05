#!/bin/tcsh -f
#
# Run the MID Validation Report process from cron.

# Common environment is inherited from /local/content/MEME/MEME5/ncim/setenv.sh.
if (! $?APP_DIR) then
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
if (! $?ADMIN_USER) set ADMIN_USER = "admin"
if (! $?ADMIN_PASSWORD) set ADMIN_PASSWORD = "admin"
if (! $?CRONVALIDATE_PROCESS_NAME) set CRONVALIDATE_PROCESS_NAME = "MID Validation Report"

if ($?DB_PASSWORD) then
  if ("$DB_PASSWORD" != "") then
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASSWORD" "$DB_NAME" )
  else
    set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
  endif
else
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
endif

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"
echo "APP_DIR = $APP_DIR"
echo "DB_NAME = $DB_NAME"
echo "BASE_URL = $BASE_URL"

set enabled = `echo "select if(automationsEnabled,'true','false') from projects;" | $mysql | tail -1`
set projectId = `echo "select id from projects;" | $mysql | tail -1`

echo "project: $projectId"
echo "enabled: $enabled"
echo ""

if ("$enabled" == "true") then
  echo "  Login ... `/bin/date`"
  set authToken = `curl -H "Content-type: text/plain" -X POST -d "$ADMIN_PASSWORD" "$BASE_URL/security/authenticate/$ADMIN_USER" | perl -pe 's/.*"authToken":"([^"]*).*/$1/;'`

  echo "  Run $CRONVALIDATE_PROCESS_NAME... `/bin/date`"
  set processId = `echo "select id from process_configs where name='$CRONVALIDATE_PROCESS_NAME';" | $mysql | tail -1`
  set executionId = `curl -H "Content-type: application/json" -H "Authorization: $authToken" -X GET "$BASE_URL/process/config/$processId/prepare?projectId=$projectId"`
  sleep 2
  curl -H "Content-type: application/json" -H "Authorization: $authToken" -X GET "$BASE_URL/process/execution/$executionId/execute?projectId=$projectId&background=true"
else
  echo "  DISABLED"
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
