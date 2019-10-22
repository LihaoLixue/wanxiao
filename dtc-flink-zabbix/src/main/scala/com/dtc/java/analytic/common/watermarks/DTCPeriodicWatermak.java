package com.dtc.java.analytic.common.watermarks;


import com.dtc.java.analytic.common.model.MetricEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created on 2019-08-21
 *
 * @author :hao.li
 */
public class DTCPeriodicWatermak implements AssignerWithPeriodicWatermarks<MetricEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DTCPeriodicWatermak.class);
    Long currentMaxTimestamp = 0L;
    Long maxOutOfOrdernes = 3000L;

    @Nullable
    @Override
    public Watermark getCurrentWatermark() {
        Watermark watermark = new Watermark(currentMaxTimestamp - maxOutOfOrdernes);
        return watermark;
    }

    @Override
    public long extractTimestamp(MetricEvent event, long l) {
        long time = event.getClock();
        currentMaxTimestamp = Math.max(time, currentMaxTimestamp);
        return time;
    }

    private Long getTime(String str) {
        String s = str.replaceAll("[A-Z]", " ");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = simpleDateFormat.parse(s);
        } catch (ParseException e) {
            LOGGER.warn("Data parse is mistake,and reasson is {}.", e);
        }
        long time = date.getTime();
        return time;
    }
}
