package local.ateng.java.TableAPI.query;

import org.apache.flink.connector.datagen.table.DataGenConnectorOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.and;

public class TableAPIQueryComplex {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 3
        env.setParallelism(3);

        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 使用 TableDescriptor 定义 Kafka 数据源
        TableDescriptor sourceDescriptor = TableDescriptor.forConnector("kafka")
                .schema(Schema.newBuilder()
                        .column("id", DataTypes.BIGINT())
                        .column("name", DataTypes.STRING())
                        .column("age", DataTypes.INT())
                        .column("score", DataTypes.DOUBLE())
                        .column("birthday", DataTypes.TIMESTAMP())
                        .column("province", DataTypes.STRING())
                        .column("city", DataTypes.STRING())
                        .column("createTime", DataTypes.TIMESTAMP())
                        // 映射 Kafka 中的 createTime 到逻辑字段 create_time
                        .columnByExpression("create_time", "createTime")
                        .columnByMetadata("timestamp",  DataTypes.TIMESTAMP())
                        .columnByMetadata("partition",  DataTypes.INT())
                        .columnByMetadata("offset",  DataTypes.BIGINT())
                        .build())
                .option("topic", "ateng_flink_json")  // Kafka topic 名称
                .option("properties.group.id", "ateng_flink_table_api")  // 消费者组 名称
                .option("properties.bootstrap.servers", "192.168.1.10:9094")  // Kafka 地址
                .option("format", "json")  // 数据格式，假设 Kafka 中的数据是 JSON 格式
                // 'earliest-offset', 'latest-offset', 'group-offsets', 'timestamp' and 'specific-offsets'
                .option("scan.startup.mode", "latest-offset")  // 从最早的偏移量开始消费
                .build();

        // 创建一个临时表 'my_user'，这个表通过 data generator 连接器读取数据
        tableEnv.createTemporaryTable("my_user", sourceDescriptor);

        // 使用 Table API 读取表
        Table table = tableEnv.from("my_user");

        // 使用 GroupBy 聚合查询
        Table result = table.filter(
                        and(
                                $("id").isNotNull(),
                                $("name").isNotNull(),
                                $("age").isNotNull()
                        )
                )
                .groupBy($("province"))
                .select($("province"), $("id").count().as("cnt"));

        // 执行操作
        TableResult tableResult = result.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("复杂查询");
    }
}
