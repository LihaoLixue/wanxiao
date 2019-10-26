package com.dtc.java.analytic.demo;

import com.dtc.java.analytic.common.model.MetricEvent;
import com.dtc.java.analytic.common.utils.CountUtils;
import com.dtc.java.analytic.common.utils.ExecutionEnvUtil;
import com.dtc.java.analytic.common.utils.KafkaConfigUtil;
import com.dtc.java.analytic.common.utils.WaterTimeUtil;
import com.dtc.java.analytic.common.watermarks.DTCPeriodicWatermak;
import com.dtc.java.analytic.source.MySourceEvent;
import org.apache.commons.io.IOUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple7;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
        DataStreamSource dataStreamSource = env.addSource(new MySourceEvent());
        dataStreamSource.map(new DTCZabbixMapFunction()).print();
        env.execute("Start zabbix data.");
    }

    static class DTCZabbixMapFunction implements org.apache.flink.api.common.functions.MapFunction<String, Float> {
        private static final Logger LOGGER = LoggerFactory.getLogger(DTCPeriodicWatermak.class);
        @Override
        public Float map(String event) throws IOException {
            Graph graph = new Graph();
            byte[] graphBytes;
            Session session=null;
            FileInputStream fileInputStream=null;
            float message=0f;
            try {
                fileInputStream = new FileInputStream("/Users/lixuewei/workspace/DTC/workspace/test/dtc_bigdata_new/dtc-flink-zabbix/src/main/resources/model.pb");
                graphBytes = IOUtils.toByteArray(fileInputStream);
                graph.importGraphDef(graphBytes);
                session = new Session(graph);
                message = session.runner()
                        .feed("x", Tensor.create(Float.valueOf(event)))
                        .fetch("z").run().get(0).floatValue();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                session.close();
                fileInputStream.close();
                graph.close();
            }
            return message;
        }
    }
}


