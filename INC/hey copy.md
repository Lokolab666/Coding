I'd focus on #2 first in the last image




Script from where i am checking this connectivity 


Your leader’s recommendation makes sense. Based on the corrected AI analysis in the screenshots, I would focus on option #2 first: the destination VPC/account that owns 10.145.18.0/24.

The important correction is this: the first AI conclusion blamed the Firewall Manager SG, then the Flow Log parsing was corrected. After that correction, there were 69 matching records and all were ACCEPT. That materially changes the diagnosis.

What we can currently say with reasonable confidence is:

GitLab runner pod
      ↓
Cilium egress
      ↓        ACCEPT
Runner ENI / source SG
      ↓        ACCEPT
Source VPC routing
      ↓
TGW / shared network
      ↓
?????
      ↓
SQL subnet 10.145.18.0/24
      ↓
SQL Server :41431

The source-side evidence does not prove that the packet reaches the SQL Server VPC. It proves that the source AWS networking is not rejecting it. The absence of any return traffic strongly suggests the drop is downstream: destination SG/NACL, intermediate firewall, destination routing, or TGW return routing.

The internal troubleshooting procedure supports exactly this approach: for a private resource in another AWS account, verify the security group actually attached to the destination resource, then check CIDR-based inbound access rather than assuming the reported SG is correct.  The GitLab runner runbook also says that once Cilium is permitting the destination but connectivity still fails, investigation/escalation moves to the network/firewall layer. 

So I would do these checks in corp-mesapacandgc-dev-mdt (495138734720).

First, identify what actually owns the four SQL IPs. Do not start by inspecting a random EC2 SG.

aws ec2 describe-network-interfaces \
  --filters \
    "Name=addresses.private-ip-address,Values=10.145.18.80,10.145.18.110,10.145.18.121,10.145.18.123" \
  --query 'NetworkInterfaces[].{
    IP:PrivateIpAddress,
    ENI:NetworkInterfaceId,
    VPC:VpcId,
    Subnet:SubnetId,
    Instance:Attachment.InstanceId,
    Description:Description,
    SGs:Groups[*].GroupId
  }' \
  --output table

This is one of the most important checks. It tells us:

10.145.18.x
   ↓
actual ENI
   ↓
actual subnet
   ↓
actual SG
   ↓
actual EC2/NLB/resource

If those are Windows EC2 SQL Server instances, you should see an instance ID. If the ENIs belong to some load balancer or another resource, the investigation changes.

Next, inspect every SG actually attached to those ENIs.

For example:

aws ec2 describe-security-groups \
  --group-ids sg-xxxxxxxx sg-yyyyyyyy

Or for cleaner rule-level output:

aws ec2 describe-security-group-rules \
  --filters "Name=group-id,Values=sg-xxxxxxxx"

You are specifically looking for an inbound rule equivalent to:

Protocol: TCP
Destination port: 41431
Source: <GitLab runner source CIDR>

But there is an important subtlety in the AI analysis.

The source Flow Logs reportedly showed both:

100.64.x.x

and NAT/private addresses such as:

10.210.54.86
10.210.54.208

Do not immediately add both:

100.64.0.0/16
10.210.54.0/23

to the SQL SG.

That would be premature and potentially too broad.

We first want to know which address the destination actually sees as srcaddr.

This leads to the best next test: destination-side VPC Flow Logs.

Trigger the failing GitLab job again and note the exact timestamp. Then, in the SQL Server VPC Flow Logs, search for traffic where:

destination IP:
10.145.18.80
10.145.18.110
10.145.18.121
10.145.18.123

destination port:
41431

protocol:
TCP / 6

Then interpret the result like this:

Destination Flow Logs	Meaning

REJECT	Very strong evidence of destination SG/NACL issue
ACCEPT	Packet reached destination AWS networking; investigate host/SQL/return path
No records at all	Packet is being lost before reaching destination VPC: TGW/firewall/routing
Records from 100.64.x.x	SQL-side controls must account for pod CIDR
Records from 10.210.54.x	SQL-side controls must account for NAT/node CIDR


This test is more valuable than guessing whether 100.64.0.0/16 or 10.210.54.0/23 should be whitelisted.

After SGs, check the subnet NACL.

Take the subnet IDs returned by the ENI query and run:

aws ec2 describe-network-acls \
  --filters "Name=association.subnet-id,Values=subnet-xxxxxxxx"

For the SQL subnet, verify at minimum:

Inbound:
source = actual GitLab source CIDR
TCP 41431
ALLOW

Outbound:
destination = actual GitLab source CIDR
TCP ephemeral ports
ALLOW

Remember the distinction:

Security Group = stateful
NACL           = stateless

For a Security Group, if inbound TCP/41431 is allowed, the response is automatically state-tracked.

For a NACL, you must permit both directions. A rule that allows inbound 41431 but blocks the return ephemeral port range can still produce exactly this timeout behavior.

One point in the AI screenshots deserves correction. It says:

> “Firewall-Manager-managed security group is NOT blocking this — packets are getting an ACCEPT verdict and leaving the account's network boundary successfully.”



The first half is reasonably supported for the specific source ENIs represented by those Flow Logs.

The second half is too strong.

An ACCEPT Flow Log means the traffic was accepted at the interface represented by that log record. It does not by itself prove successful traversal through:

source VPC
→ TGW
→ inspection/firewall
→ target VPC

That distinction matters here.

So I would currently describe the evidence as:

Confirmed:
✓ DNS works
✓ Cilium is very unlikely to be blocking
✓ Source ENI/security-group processing accepts TCP/41431
✓ Traffic is attempted repeatedly
✓ No SYN-ACK/response observed on source side
✓ SQL locally listens on 41431

Not yet confirmed:
? packet reaches destination VPC
? destination SG allows actual runner source
? destination NACL permits both directions
? intermediate firewall permits 41431
? TGW has valid return path

This is why your leader's “check #2 first” is a good sequence.

I would specifically perform these four commands/checks next:

# 1. Find actual destination ENIs/SGs/subnets
aws ec2 describe-network-interfaces \
  --filters \
  "Name=addresses.private-ip-address,Values=10.145.18.80,10.145.18.110,10.145.18.121,10.145.18.123"

# 2. Inspect SG rules
aws ec2 describe-security-group-rules \
  --filters "Name=group-id,Values=<ACTUAL-SG-ID>"

# 3. Find NACL associated with destination subnet
aws ec2 describe-network-acls \
  --filters "Name=association.subnet-id,Values=<SQL-SUBNET-ID>"

# 4. Check destination VPC Flow Logs during a fresh pipeline test
# Filter on destination 10.145.18.x and dstport 41431.

Then send me the output of the ENI query first. From that I can tell you exactly which SG and NACL you should inspect and what rule is missing, if any.

I would not touch the TGW route table yet. If the destination SG/NACL check is clean and the target-side Flow Logs show no packet arriving, then the TGW/intermediate firewall becomes the next owner. That is the appropriate point to pursue option #1 from the screenshot.
