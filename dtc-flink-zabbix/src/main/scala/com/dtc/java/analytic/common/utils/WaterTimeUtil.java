package com.dtc.java.analytic.common.utils;

import com.dtc.java.analytic.common.model.MetricEvent;
import com.dtc.java.analytic.common.watermarks.DTCPeriodicWatermak;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple7;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2019-10-17
 *
 * @author :hao.li
 */
public class WaterTimeUtil {
    public static WindowedStream<Tuple7<String, String, String, String, String, Long, Double>, Tuple, TimeWindow>
    buildWaterMarks(StreamExecutionEnvironment env, DataStreamSource<MetricEvent> streamSource) {
        ParameterTool parameter = (ParameterTool) env.getConfig().getGlobalJobParameters();
        long windowSizeMillis = Long.parseLong(parameter.get("windowSizeMillis", "50000"));
        SingleOutputStreamOperator<MetricEvent> streamOperator = streamSource
                .assignTimestampsAndWatermarks(new DTCPeriodicWatermak());
        SingleOutputStreamOperator mapTest = streamOperator.map(new DTCZabbixMapFunction());
        WindowedStream<Tuple7<String, String, String, String, String, Long, Double>, Tuple, TimeWindow>
                timeWindowedStream = mapTest
                .keyBy(0)
                .timeWindow(Time.of(windowSizeMillis, TimeUnit.MILLISECONDS));
        return timeWindowedStream;
    }

    static class DTCZabbixMapFunction implements MapFunction<MetricEvent, Tuple7<String, String, String, String, String, Long, Double>> {
        private static final Logger LOGGER = LoggerFactory.getLogger(DTCPeriodicWatermak.class);

        @Override
        public Tuple7<String, String, String, String, String, Long, Double> map(MetricEvent event) {
            CountUtils.incrementEventReceivedCount();
            Tuple7<String, String, String, String, String, Long, Double> message;
            String ip = event.getIp();
            String host = event.getHost();
            String oid = event.getOid();
            String name = event.getName();
            String english = event.getEnglish();
            long clock = event.getClock();

            double value = event.getValue();
            message = Tuple7.of(ip, host, oid, name, english, clock, value);
            return message;
        }
    }
}
