package com.cloudera.utils.hadoop.hms.mirror;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cloudera.utils.hadoop.hms.mirror.MirrorConf.*;

public class StatsCalculatorTest extends TestCase {
    private final List<String> table_01 = new ArrayList<String>();
    private EnvironmentTable environmentTable;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String[] strTable_01 = new String[]{
                "CREATE EXTERNAL TABLE `catalog_returns`(           "
                , "  `cr_returned_time_sk` bigint,                    "
                , "  `cr_item_sk` bigint,                             "
                , "  `cr_refunded_customer_sk` bigint,                "
                , "  `cr_refunded_cdemo_sk` bigint,                   "
                , "  `cr_refunded_hdemo_sk` bigint,                   "
                , "  `cr_refunded_addr_sk` bigint,                    "
                , "  `cr_returning_customer_sk` bigint,               "
                , "  `cr_returning_cdemo_sk` bigint,                  "
                , "  `cr_returning_hdemo_sk` bigint,                  "
                , "  `cr_returning_addr_sk` bigint,                   "
                , "  `cr_call_center_sk` bigint,                      "
                , "  `cr_catalog_page_sk` bigint,                     "
                , "  `cr_ship_mode_sk` bigint,                        "
                , "  `cr_warehouse_sk` bigint,                        "
                , "  `cr_reason_sk` bigint,                           "
                , "  `cr_order_number` bigint,                        "
                , "  `cr_return_quantity` int,                        "
                , "  `cr_return_amount` decimal(7,2),                 "
                , "  `cr_return_tax` decimal(7,2),                    "
                , "  `cr_return_amt_inc_tax` decimal(7,2),            "
                , "  `cr_fee` decimal(7,2),                           "
                , "  `cr_return_ship_cost` decimal(7,2),              "
                , "  `cr_refunded_cash` decimal(7,2),                 "
                , "  `cr_reversed_charge` decimal(7,2),               "
                , "  `cr_store_credit` decimal(7,2),                  "
                , "  `cr_net_loss` decimal(7,2))                      "
                , "PARTITIONED BY (                                   "
                , "  `cr_returned_date_sk` bigint)                    "
                , "ROW FORMAT SERDE                                   "
                , "  'org.apache.hadoop.hive.ql.io.orc.OrcSerde'      "
                , "STORED AS INPUTFORMAT                              "
                , "  'org.apache.hadoop.hive.ql.io.orc.OrcInputFormat'  "
                , "OUTPUTFORMAT                                       "
                , "  'org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat' "
                , "LOCATION                                           "
                , "  'hdfs://HOME90/warehouse/tablespace/external/hive/tpcds_bin_partitioned_external_orc_6.db/catalog_returns' "
                , "TBLPROPERTIES (                                    "
                , "  'TRANSLATED_TO_EXTERNAL'='TRUE',                 "
                , "  'bucketing_version'='2',                         "
                , "  'external.table.purge'='TRUE',                   "
                , "  'transient_lastDdlTime'='1685711419')            "
        };
        table_01.addAll(Arrays.asList(strTable_01));
        environmentTable = new EnvironmentTable();
        environmentTable.setDefinition(table_01);
        List<String> partitions = new ArrayList<String>();
        // 15000 partitions
        for (int i = 0; i < 11000; i++) {
            partitions.add(Integer.toString(i));
        }
        environmentTable.getStatistics().put(FILE_FORMAT, SerdeType.ORC);
        // 13Tb
        Long size = 1024l*1024*1024*1024*13;
        environmentTable.getStatistics().put(DATA_SIZE, size);
        // 5Mb
        Double avgSize = 1024*1024*5d;
        environmentTable.getStatistics().put(AVG_FILE_SIZE, avgSize);
        environmentTable.setPartitions(partitions);

    }

    public void testGetPartitionDistributionRatio() {
        Long ratio = StatsCalculator.getPartitionDistributionRatio(environmentTable);
        assertEquals((long)8, (long)ratio);
        System.out.println();
    }

    public void testGetAdditionalPartitionDistribution() {
        String test = StatsCalculator.getAdditionalPartitionDistribution(environmentTable);
        assertEquals("ROUND((rand() * 1000) % 8), `cr_returned_date_sk`", test);
        System.out.println(test);
    }

    public void testGetTezMaxGrouping_01() {
        Long test = StatsCalculator.getTezMaxGrouping(environmentTable);
        assertEquals(67108864L, (long)test);
        System.out.println(test);
    }

    public void testSetSessionOptions() {
        EnvironmentTable applyEnv = new EnvironmentTable();
        StatsCalculator.setSessionOptions(environmentTable, applyEnv);
        System.out.println(applyEnv.getSql().stream().map(s -> s.toString() + "\n").reduce("", String::concat));
    }
}