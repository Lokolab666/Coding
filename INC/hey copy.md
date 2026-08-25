The reply of the customer
DNS resolution from the runner works and resolves to 10.145.18.80, 10.145.18.121, 10.145.18.123 and 10.145.18.110.

From the AWS shared runner, TCP connectivity to all SQL Server IPs on port 41431 times out.

On the SQL Server side, DB01 is running and listening on 0.0.0.0:41431. Windows Firewall is disabled, and local connectivity to port 41431 succeeds.