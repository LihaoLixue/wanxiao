package com.dtc.java.analytic.zabbix;


import com.dtc.java.analytic.common.model.MetricEvent;
import com.dtc.java.analytic.common.utils.*;
import com.dtc.java.analytic.common.watermarks.DTCPeriodicWatermak;
import com.dtc.java.analytic.sink.opentsdb.PSinkToOpentsdb;
import com.dtc.java.analytic.common.utils.DateUntil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple7;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created on 2019-08-15
 *
 * @author :hao.li
 */
public class PrometheusToFlink {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusToFlink.class);

    public static void main(String[] args) throws Exception {
        final ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args);
        StreamExecutionEnvironment env = ExecutionEnvUtil.prepare(parameterTool);
        DataStreamSource<MetricEvent> streamSource = KafkaConfigUtil.buildSource(env);
        WindowedStream<Tuple7<String, String, String, String, String, Long, Double>, Tuple, TimeWindow>
                timeWindowedStream = WaterTimeUtil.buildWaterMarks(env, streamSource);
        SingleOutputStreamOperator<Tuple7<String, String, String, String, String, Long, Double>>
                process = timeWindowedStream.process(new DtcProcessWindowFunction());
        String opentsdb_url = parameterTool.get("dtc.opentsdb.url", "http://10.3.7.234:4399");
        process.addSink(new PSinkToOpentsdb(opentsdb_url)).name("Opentsdb-Sink");
        //mysql sink
//      process.addSink(new MysqlSink(properties));
        env.execute("Start zabbix data.");
    }
}

class DtcMapFunction implements MapFunction<String, Tuple5<String, String, String, Long, Double>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DTCPeriodicWatermak.class);

    @Override
    public Tuple5<String, String, String, Long, Double> map(String s) {
        CountUtils.incrementEventReceivedCount();
        Tuple5<String, String, String, Long, Double> message = null;
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = null;
        try {
            json = objectMapper.readTree(s);
        } catch (IOException e) {
            LOGGER.error("Make mistake when parse the data,and the reason is {}.", e);
            CountUtils.incrementEventReceivedCount_Failuer();
            return Tuple5.of("null", "null", "null", null, null);
        }
        String name1 = json.get("name").textValue();
        if (!(name1.contains("_"))) {
            name1 = name1 + "_" + 0000;
        }
        String[] name = name1.split("_", 2);
        String system_name = name[0].trim();
        String ZB_name = name[1].trim();
//-----------------------------------------------------------------------------
//        Long time_all = DateUntil.getTime(json.get("timestamp").textValue());
//        Double value_all = json.get("value").asDouble();
//        JsonNode jsonNode_all = null;
//        try {
//            jsonNode_all = objectMapper.readTree(json.get("labels").toString());
//        } catch (IOException e) {
//            CountUtils.incrementEventReceivedCount_Failuer();
//            LOGGER.error("Make mistake when parse the lebles of data,and the reason is {}.", e);
//            return Tuple5.of("null", "null", "null", null, null);
//        }
//        String Host_all = jsonNode_all.get("instance").textValue();
//        message = Tuple5.of(system_name, Host_all, ZB_name, time_all, value_all);
//-----------------------------------------------------------------------------
////        //Linux数据处理
        if (system_name.contains("node")) {
            Long time = DateUntil.getTime(json.get("timestamp").textValue());
            Double value = json.get("value").asDouble();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(json.get("labels").toString());
            } catch (IOException e) {
                CountUtils.incrementEventErrorCount("linux");
                LOGGER.error("Make mistake when parse the lebles of data,and the reason is {}.", e);
                return Tuple5.of("null", "null", "null", null, null);

            }
            String Host = jsonNode.get("instance").textValue();
            message = Tuple5.of(system_name, Host, ZB_name, time, value);
            CountUtils.incrementEventRightCount("linux");
        }
        //mysql数据处理
        else if (system_name.contains("mysql")) {
            Long time = DateUntil.getTime(json.get("timestamp").textValue());
            Double value = json.get("value").asDouble();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(json.get("labels").toString());
            } catch (IOException e) {
                LOGGER.error("Make mistake when parse the lebles of data,and the reason is {}.", e);
                CountUtils.incrementEventErrorCount("mysql");
                return null;
            }
            String Host = jsonNode.get("instance").textValue();
            message = Tuple5.of(system_name, Host, ZB_name, time, value);
            CountUtils.incrementEventRightCount("mysql");
        }
        //oracle数据处理
        else if (system_name.contains("oracledb")) {
            Long time = DateUntil.getTime(json.get("timestamp").textValue());
            Double value = json.get("value").asDouble();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(json.get("labels").toString());
            } catch (IOException e) {
                LOGGER.error("Make mistake when parse the lebles of data,and the reason is {}.", e);
                CountUtils.incrementEventErrorCount("oracle");
                return Tuple5.of("null", "null", "null", null, null);
            }
            String Host = jsonNode.get("instance").textValue();
            message = Tuple5.of(system_name, Host, ZB_name, time, value);
            CountUtils.incrementEventRightCount("oracle");
        }
        //windows数据处理
        else if (system_name.contains("windows")) {
            Long time = DateUntil.getTime(json.get("timestamp").textValue());
            Double value = json.get("value").asDouble();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(json.get("labels").toString());
            } catch (IOException e) {
                LOGGER.error("Make mistake when parse the lebles of data,and the reason is {}.", e);
                CountUtils.incrementEventErrorCount("windows");
                return Tuple5.of("null", "null", "null", null, null);
            }
            String Host = jsonNode.get("instance").textValue();
            message = Tuple5.of(system_name, Host, ZB_name, time, value);
            CountUtils.incrementEventRightCount("windows");
        } else {
//            Long time_all = DateUntil.getTime(json.get("timestamp").textValue());
//            Double value_all = json.get("value").asDouble();
//            JsonNode jsonNode_all = null;
//            try {
//                jsonNode_all = objectMapper.readTree(json.get("labels").toString());
//            } catch (IOException e) {
//                CountUtils.incrementEventReceivedCount_Failuer();
//                LOGGER.error("Make mistake when parse the lebles of data,and the reason is {}.", e);
//                return Tuple5.of("null", "null", "null", null, null);
//            }
//            String Host_all = jsonNode_all.get("instance").textValue();
//            message = Tuple5.of(system_name, Host_all, ZB_name, time_all, value_all);
            return Tuple5.of("null", "null", "null", null, null);
        }
        return message;
    }
}


class DtcProcessWindowFunction extends ProcessWindowFunction<Tuple7<String, String, String, String, String, Long, Double>, Tuple7<String, String, String, String, String, Long, Double>, Tuple, TimeWindow> {

    @Override
    public void process(Tuple tuple, Context context, Iterable<Tuple7<String, String, String, String, String, Long, Double>> elements, Collector<Tuple7<String, String, String, String, String, Long, Double>> collector)
            throws Exception {
        for (Tuple7<String, String, String, String, String, Long, Double> in : elements) {
            collector.collect(Tuple7.of(in.f0, in.f1, in.f2, in.f3, in.f4, in.f5, in.f6));
        }
    }
}
