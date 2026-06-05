#!/bin/tcsh -f
#
# Weekly MEME automation:
# 1. Rebuild deep_relationships tables.
# 2. Optionally restart the application service.

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
if (! $?MEME_BIN) set MEME_BIN = "$APP_DIR/bin"
if (! $?MYSQL_BIN) set MYSQL_BIN = "mysql"
if (! $?APP_SERVICE) set APP_SERVICE = "nci-meme5"
if (! $?RESTART_SERVER_AFTER_WEEKLY) set RESTART_SERVER_AFTER_WEEKLY = "true"
if (! $?REBUILD_DEEP_RELS_SQL) set REBUILD_DEEP_RELS_SQL = "$MEME_BIN/rebuildDeepRels.sql"

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
  if (! -e "$REBUILD_DEEP_RELS_SQL") then
    echo "ERROR: REBUILD_DEEP_RELS_SQL not found: $REBUILD_DEEP_RELS_SQL"
    exit 1
  endif

  echo "  Rebuild the deep_relationships tables ...  `/bin/date`"
  $mysql < "$REBUILD_DEEP_RELS_SQL"
  if ($status != 0) then
    echo "ERROR: deep_relationships rebuild failed"
    exit 1
  endif

  if ("$RESTART_SERVER_AFTER_WEEKLY" == "true") then
    sudo service "$APP_SERVICE" stop
    if ($status != 0) then
      echo "ERROR: could not stop $APP_SERVICE ...  `/bin/date`"
      exit 1
    endif

    sudo service "$APP_SERVICE" start
    if ($status != 0) then
      echo "ERROR: could not start $APP_SERVICE ...  `/bin/date`"
      exit 1
    endif
  else
    echo "  Skipping restart because RESTART_SERVER_AFTER_WEEKLY=$RESTART_SERVER_AFTER_WEEKLY"
  endif
else
  echo "  DISABLED"
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
