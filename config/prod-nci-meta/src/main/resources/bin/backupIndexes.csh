#!/bin/tcsh -f
#
# Archive Lucene indexes locally and upload the archive to S3.

# Common environment is inherited from /local/content/MEME/MEME5/ncim/setenv.sh.
if (! $?APP_DIR) then
  echo "ERROR: APP_DIR must be set; source the production setenv.sh first."
  exit 1
endif
if (! $?INDEX_DIR) then
  echo "ERROR: INDEX_DIR must be set; source the production setenv.sh first."
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

# Script configuration
if (! $?INDEX_ARCHIVE_DIR) set INDEX_ARCHIVE_DIR = "$APP_DIR/archive/indexes"
if (! $?MYSQL_BIN) set MYSQL_BIN = "mysql"
if (! $?AWS_BIN) set AWS_BIN = "aws"
if (! $?AWS_PROFILE) set AWS_PROFILE = "meme"
if (! $?S3_BUCKET_NAME) set S3_BUCKET_NAME = "nci-evs-meme"
if (! $?S3_BUCKET) set S3_BUCKET = "s3://$S3_BUCKET_NAME"

if ($?DB_PASSWORD) then
  if ("$DB_PASSWORD" != "") then
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "-p$DB_PASSWORD" "$DB_NAME" )
  else
    set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
  endif
else
  set mysql = ( "$MYSQL_BIN" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" )
endif
if ("$AWS_PROFILE" == "") then
  set aws = ( "$AWS_BIN" )
else
  set aws = ( "$AWS_BIN" --profile "$AWS_PROFILE" )
endif

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"
echo "APP_DIR = $APP_DIR"
echo "INDEX_DIR = $INDEX_DIR"
echo "INDEX_ARCHIVE_DIR = $INDEX_ARCHIVE_DIR"
echo "S3_BUCKET = $S3_BUCKET"

set enabled = `echo "select if(automationsEnabled,'true','false') from projects;" | $mysql | tail -1`
set projectId = `echo "select id from projects;" | $mysql | tail -1`

echo "project: $projectId"
echo "enabled: $enabled"
echo ""

if ("$enabled" == "true") then
  if (! -d "$INDEX_DIR") then
    echo "ERROR: INDEX_DIR must exist: $INDEX_DIR"
    exit 1
  endif

  mkdir -p "$INDEX_ARCHIVE_DIR"
  cd "$INDEX_ARCHIVE_DIR"

  if ($#argv == 1) then
    set dayofweek = 'manual'
    set todaysdate = `date +"manual_%Y%m%d"`
  else
    set dayofweek = `date +"%a"`
    set todaysdate = `date +"%a_%Y%m%d"`
  endif

  echo "todaysdate: $todaysdate"

  mkdir "$INDEX_ARCHIVE_DIR/$todaysdate"
  cp -R "$INDEX_DIR"/* "$INDEX_ARCHIVE_DIR/$todaysdate"

  cd "$INDEX_ARCHIVE_DIR/$todaysdate"
  tar -cvf "$todaysdate.tar" *

  set fileExists = `$aws s3api list-objects-v2 --bucket "$S3_BUCKET_NAME" --max-items 10 --prefix "indexes/$dayofweek" --output json | jq -r '.Contents | .[] |[.Key]' | grep "$dayofweek" | wc -l`
  if ($fileExists == 1) then
    echo "INFO: replacing $dayofweek indexes file"
    set lastWeekFile = `$aws s3api list-objects-v2 --bucket "$S3_BUCKET_NAME" --max-items 10 --prefix "indexes/$dayofweek" --output json | jq -r '.Contents | .[0] |[.Key][0]' | grep "$dayofweek" | perl -pe 's/"//g;'`
    echo "INFO: lastWeekFile to remove: $lastWeekFile"
    $aws s3 rm "$S3_BUCKET/$lastWeekFile"
  else
    echo "INFO: no previous $dayofweek indexes file"
  endif

  $aws s3 cp "$todaysdate.tar" "$S3_BUCKET/indexes/$todaysdate.tar"

  rm "$todaysdate.tar"
  rm -rf "$INDEX_ARCHIVE_DIR/$todaysdate"
else
  echo "  DISABLED"
endif

echo "--------------------------------------------------------"
echo "Finished ... `/bin/date`"
echo "--------------------------------------------------------"
