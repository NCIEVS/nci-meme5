#!/usr/bin/env bash
#
# Prepare meme-test and/or meme-release for a test insertion by snapshotting the
# source edit database, recreating the target RDS database(s), and backing up the
# source Lucene indexes to S3.

set -euo pipefail

# Common environment is inherited from /local/content/MEME/MEME5/ncim/setenv.sh.
: "${APP_DIR:?source the production setenv.sh before running this script}"
: "${INDEX_DIR:?source the production setenv.sh before running this script}"
: "${BASE_URL:?source the production setenv.sh before running this script}"

# Script configuration
MEME_BIN="${MEME_BIN:-$APP_DIR/bin}"
AWS_BIN="${AWS_BIN:-aws}"
AWS_PROFILE="${AWS_PROFILE:-meme}"
S3_BUCKET_NAME="${S3_BUCKET_NAME:-nci-evs-meme}"
S3_BUCKET="${S3_BUCKET:-s3://$S3_BUCKET_NAME}"
APP_SERVICE="${APP_SERVICE:-nci-meme5}"
RESTART_SERVER_FOR_INSERTION_SNAPSHOT="${RESTART_SERVER_FOR_INSERTION_SNAPSHOT:-true}"
MEME_TEST_DB="${MEME_TEST_DB:-meme-test}"
MEME_RELEASE_DB="${MEME_RELEASE_DB:-meme-release}"
MEME_SOURCE_DB="${MEME_SOURCE_DB:-meme-edit-new}"
RDS_MANUAL_SNAPSHOT_ID="${RDS_MANUAL_SNAPSHOT_ID:-meme-edit-manual-snapshot}"
RDS_PARAMETER_GROUP="${RDS_PARAMETER_GROUP:-meme-db-84}"
RDS_AVAILABILITY_ZONE="${RDS_AVAILABILITY_ZONE:-us-east-1d}"
RDS_SUBNET_GROUP="${RDS_SUBNET_GROUP:-default-vpc-dca724a4}"
RDS_SECURITY_GROUP_IDS="${RDS_SECURITY_GROUP_IDS:-sg-05993d12d18c40cae}"

AWS_CMD=("$AWS_BIN")
if [[ -n "$AWS_PROFILE" ]]; then
  AWS_CMD+=(--profile "$AWS_PROFILE")
fi

read -r -a RDS_SECURITY_GROUP_ARGS <<< "$RDS_SECURITY_GROUP_IDS"

usage="test_insertion.sh {$MEME_TEST_DB|$MEME_RELEASE_DB|both}"
both=0

echo "------------------------------------------------------"
echo "Starting ... $(/bin/date)"
echo "------------------------------------------------------"

if [[ "$#" -ne 1 ]]; then
  echo "ERROR: Wrong number of parameters"
  echo "usage: $usage"
  exit 1
fi

TARGET_DB="$1"
SNAPSHOT_DATE="$(/bin/date +"%Y%m%d")"
TARGET_DBS=("$TARGET_DB")

if [[ "$TARGET_DB" == "both" ]]; then
  TARGET_DBS=("$MEME_TEST_DB" "$MEME_RELEASE_DB")
  both=1
  echo "Both $MEME_TEST_DB and $MEME_RELEASE_DB will be prepared"
fi

for db in "${TARGET_DBS[@]}"; do
  if [[ "$db" != "$MEME_TEST_DB" && "$db" != "$MEME_RELEASE_DB" ]]; then
    echo "ERROR: target DB must be $MEME_TEST_DB, $MEME_RELEASE_DB, or both"
    exit 1
  fi
done

echo "TARGET_DBS: ${TARGET_DBS[*]}"
echo "SOURCE_DB: $MEME_SOURCE_DB"
echo "SNAPSHOT: $RDS_MANUAL_SNAPSHOT_ID"
echo "SNAPSHOT_DATE: $SNAPSHOT_DATE"
echo "MEME_BIN: $MEME_BIN"
echo "APP_SERVICE: $APP_SERVICE"

server_stopped=0
restart_server() {
  if [[ "$server_stopped" -eq 1 && "$RESTART_SERVER_FOR_INSERTION_SNAPSHOT" == "true" ]]; then
    sudo service "$APP_SERVICE" start
  fi
}
trap restart_server EXIT

cd "$MEME_BIN"

if [[ "$RESTART_SERVER_FOR_INSERTION_SNAPSHOT" == "true" ]]; then
  sudo service "$APP_SERVICE" stop
  server_stopped=1
else
  echo "Skipping service stop because RESTART_SERVER_FOR_INSERTION_SNAPSHOT=$RESTART_SERVER_FOR_INSERTION_SNAPSHOT"
fi

echo "Deleting old $RDS_MANUAL_SNAPSHOT_ID snapshot if it exists"
"${AWS_CMD[@]}" rds delete-db-snapshot \
  --db-snapshot-identifier "$RDS_MANUAL_SNAPSHOT_ID" || true

while "${AWS_CMD[@]}" rds describe-db-snapshots \
    --db-snapshot-identifier "$RDS_MANUAL_SNAPSHOT_ID" >/dev/null 2>&1; do
  echo "waiting for old snapshot deletion"
  sleep 10
done

echo "Creating $RDS_MANUAL_SNAPSHOT_ID from $MEME_SOURCE_DB"
"${AWS_CMD[@]}" rds create-db-snapshot \
  --db-instance-identifier "$MEME_SOURCE_DB" \
  --db-snapshot-identifier "$RDS_MANUAL_SNAPSHOT_ID"

"${AWS_CMD[@]}" rds wait db-snapshot-available \
  --db-snapshot-identifier "$RDS_MANUAL_SNAPSHOT_ID"

for db in "${TARGET_DBS[@]}"; do
  echo "Restoring $db from $RDS_MANUAL_SNAPSHOT_ID"
  "${AWS_CMD[@]}" rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier "$db" \
    --db-snapshot-identifier "$RDS_MANUAL_SNAPSHOT_ID" \
    --db-parameter-group-name "$RDS_PARAMETER_GROUP" \
    --availability-zone "$RDS_AVAILABILITY_ZONE" \
    --db-subnet-group-name "$RDS_SUBNET_GROUP" \
    --vpc-security-group-ids "${RDS_SECURITY_GROUP_ARGS[@]}"

  "${AWS_CMD[@]}" rds wait db-instance-available \
    --db-instance-identifier "$db"
done

echo "RDS recreation completed"
echo "Backing up indexes to S3"
"$MEME_BIN/backupIndexes.csh" manual

echo "------------------------------------------------------"
echo "Finished ... $(/bin/date)"
echo "------------------------------------------------------"

if [[ "$both" -eq 1 ]]; then
  echo "Prepared $MEME_TEST_DB and $MEME_RELEASE_DB"
else
  echo "Prepared ${TARGET_DBS[0]}"
fi

cat <<EOF

Next steps on the target host:
1. Pull the matching index archive from $S3_BUCKET/indexes.
2. Restore it under INDEX_DIR=$INDEX_DIR.
3. Pull insertion source data with pull_s3.csh.
4. Start the app and verify BASE_URL=$BASE_URL.
EOF
