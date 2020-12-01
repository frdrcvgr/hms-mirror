# HDFS Integration Between HDP 2.6 and CDP Base

## Goal

What does it take to support HDFS visibility between these two clusters?

Can that integration be used to support the Higher Clusters use of the Lower Clusters HDFS Layer for distcp AND Hive External Table support?

## Scenario #1

### HDP 2.6.5
Kerberized - sharing same KDC as CDP Base Cluster

#### Configuration Changes
_hdfs-site.xml_
```
hadoop.rpc.protection=true
```

_core-site.xml_
```
dfs.encrypt.data.transfer=true
dfs.encrypt.data.transfer.algorithm=3des
dfs.encrypt.data.transfer.cipher.key.bitlength=256
```

### CDP 7.1.4
Kerberized, TLS Enabled

#### Configuration Changes

Requirements that allow this (upper) cluster to negotiate and communicate with the lower environment.

_Cluster Wide hdfs-site.xml Safety Value_

```
ipc.client.fallback-to-simple-auth-allowed=true
```

_HDFS Service Advanced Config hdfs-site.xml_

```
# For this Clusters Name Service
dfs.internal.nameservices=HOME90

# For the target (lower) environment HA NN Services
dfs.ha.namenodes.HDP50=nn1,nn2
dfs.namenode.rpc-address.HDP50.nn1=k01.streever.local:8020
dfs.namenode.rpc-address.HDP50.nn2=k02.streever.local:8020
dfs.namenode.http-address.HDP50.nn1=k01.streever.local:50070
dfs.namenode.http-address.HDP50.nn2=k02.streever.local:50070
dfs.namenode.https
 address.HDP50.nn1=k01.streever.local:50471
dfs.namenode.https-address.HDP50.nn2=k02.streever.local:50470
dfs.client.failover.proxy.provider.HDP50=org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider

# For Available Name Services
dfs.nameservices=HOME90,HDP50  
```

### Running `distcp` from the Upper Cluster

Copy 'from' Lower Cluster

```
hadoop distcp hdfs://HDP50/user/dstreev/sstats/queues/2020-10.txt /user/dstreev/temp
```

Copy 'to' Lower Cluster

```
hadoop distcp /warehouse/tablespace/external/hive/cvs_hathi_workload.db/queue/2020-10.txt hdfs://HDP50/user/dstreev/temp
```

### Sourcing Data from Lower Cluster to Support Upper Cluster External Tables

#### Permissions
The lower cluster must allow the upper clusters _Hive Server 2_ host as a 'hive' proxy.  The setting in the lower clusters _custom_ `core-site.xml` may limit this to that clusters (lower) HS2 hosts.  Open it up to include the upper clusters HS2 host.

_Custom core-site.xml in Lower Cluster_
```
hadoop.proxyuser.hive.hosts=*
```

Credentials from the 'upper' cluster will be projected down to the 'lower' cluster.  This means the `hive` user in the upper cluster, when running with 'non-impersonation' will require access to the datasets in the lower cluster HDFS.

For table creation in the 'upper' clusters Metastore, a permissions check will be done on the lower environments directory for the submitting user.  So, both the service user AND `hive` will require access to the directory location specified in the lower cluster.

When the two clusters _share_ accounts and the same accounts are used between environment for users and service accounts, then access should be simple.

When a different set of accounts are used, the 'principal' from the upper clusters service account for 'hive' and the 'user' principal will be used in the lower cluster.  Which means additional HDFS policies in the lower cluster may be required to support this cross environment work.

#distcp #hdfs 