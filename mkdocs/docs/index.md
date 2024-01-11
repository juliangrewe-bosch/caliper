# Caliper Report

## Test Environment

| Size           | vCPU | Memory (GiB) | Temp storage SSD (GiB) | Max temp storage throughput: IOPS/ Read MBps/ Write MBps | Max data disks | Throughput IOPS | Max NICs | Expected network bandwidth (Mbps) |
| -------------- | ---- | ------------ | ---------------------- | -------------------------------------------------------- | -------------- | --------------- | -------- | --------------------------------- |
| Standard_D3_v2 | 4    | 14           | 200                    | 12000/187/93                                             | 16             | 16x500          | 4        | 3000                              |

_DSv2-series_ sizes run on the
`3rd Generation Intel® Xeon® Platinum 8370C (Ice Lake)`,
`Intel® Xeon® Platinum 8272CL (Cascade Lake)`,
`Intel® Xeon® 8171M 2.1GHz (Skylake)`, or the
`Intel® Xeon® E5-2673 v4 2.3 GHz (Broadwell)`, or the
`Intel® Xeon® E5-2673 v3 2.4 GHz (Haswell)` processors with Intel Turbo Boost
Technology 2.0 and use premium storage.

### Resource reservations

Allocatable resources are less than total resources since AKS uses node
resources to maintain node performance and functionality
[resource reservations](https://learn.microsoft.com/en-us/azure/aks/concepts-clusters-workloads).

## Amphora

| Name                | Objective             | Users           | Description                                                                                      | Test data                        |
| ------------------- | --------------------- | --------------- | ------------------------------------------------------------------------------------------------ | -------------------------------- |
| create_x            | secret size           | 1 Virtual User  | Upload secret, repeat 10 times                                                                   | variable secret size, fixed tags |
| create_x_loaded     | System is under load  | 1 Virtual User  | Upload secret, repeat 10 times                                                                   | variable secret size, fixed tags |
| create_get_parallel | Concurrent operations | 10 Virtual User | Secrets are uploaded by 5 users concurrently, and parallel 5 users download secrets concurrently | fixed secret size, fixed tags    |

## Castor

_Castor_ no _Client Interface_, therefore cAdvisor metrics for _Castor_ are
collected during the execution of _Amphora_ and _Ephemeral_ scenario(s) and
exported under the specific group.

## Ephemeral

| Name                           | Objective                              | Users           | Description                                                   | Test data                        |
| ------------------------------ | -------------------------------------- | --------------- | ------------------------------------------------------------- | -------------------------------- |
| scalarValueOptProgram          | Multiplication operation on input data | 1 Virtual User  | Upload secret, repeat 10 times                                | variable secret size, fixed tags |
| scalarValueOptProgram_loaded   | System is under load                   | 1 Virtual User  | Execute program while system is loaded with 5.000.000 secrets | fixed secrets, fixed tags        |
| scalarValueOptProgram_parallel | Concurrent execution                   | 10 Virtual User | Execute program with 10 users concurrently                    | fixed secrets, fixed tags        |
