High-Availability Java Cluster (Fault-Tolerant):

A self-healing distributed cluster implemented in Java using the Bully Algorithm and State Machine Replication. This project demonstrates how distributed systems maintain a "Single Source of Truth" and ensure task continuity during node failures.

Overview of the stuff
In this cluster, nodes communicate via TCP Sockets to monitor health and coordinate work. Unlike basic heartbeat systems, this project implements State Sync, allowing a newly elected leader to pick up exactly where the failed leader left off.

Key Features
Leader Election:
Automated election using the Bully Algorithm (Highest Port Priority).

Failure Detection:
Real-time health monitoring via bidirectional heartbeats.

Stateful Failover: 
Standby nodes track the lastSeenJobId to prevent data gaps or duplicate processing during transitions.

CP Architecture:
Prioritizes Consistency and Partition Tolerance, following the design philosophy of databases like PostgreSQL.
