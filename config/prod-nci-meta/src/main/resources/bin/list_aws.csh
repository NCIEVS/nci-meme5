#!/bin/tcsh -f
#
# This script is used to list ec2 and rds instances
#

# Configuration
set AWS_BIN = "aws"
set AWS_PROFILE = "meme"
if ("$AWS_PROFILE" == "") then
  set aws = ( "$AWS_BIN" )
else
  set aws = ( "$AWS_BIN" --profile "$AWS_PROFILE" )
endif

set usage = 'list_aws.csh'

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"


$aws ec2 describe-instances \
--query "Reservations[*].Instances[*].{InstanceId:InstanceId,PublicIP:PublicIpAddress,Type:InstanceType,Name:Tags[?Key=='Name']|[0].Value,Status:State.Name}"  \
--filters "Name=instance-state-name,Values=running" "Name=tag:Name,Values='*'"  \
--output table

$aws rds describe-db-instances --query "DBInstances[*].{DBInstance:DBInstanceIdentifier,Type:DBInstanceClass,Status:DBInstanceStatus}" --output table

$aws rds describe-db-instances --query "DBInstances[*].{DBInstance:DBInstanceArn}" | jq -r '.[].DBInstance' > /tmp/rdsArns.txt


foreach i (`cat /tmp/rdsArns.txt`)
echo ""
echo $i
$aws rds describe-pending-maintenance-actions --resource-identifier $i --output table
end

$aws rds describe-db-snapshots --query "DBSnapshots[*].{DBInstance:DBInstanceIdentifier,DBSnapshot:DBSnapshotIdentifier,CreateTime:"InstanceCreateTime",Status:"Status"}" --output table

echo ""

echo "-----------------------------------------------------"
echo "Finished $0 ... `/bin/date`"
echo "-----------------------------------------------------"
