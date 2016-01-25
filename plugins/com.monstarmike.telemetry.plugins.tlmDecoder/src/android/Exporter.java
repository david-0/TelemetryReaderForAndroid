package com.monstarmike.telemetry.plugins.tlmDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.monstarmike.tlmreader.Flight;
import com.monstarmike.tlmreader.datablock.AirspeedBlock;
import com.monstarmike.tlmreader.datablock.AltitudeBlock;
import com.monstarmike.tlmreader.datablock.CurrentBlock;
import com.monstarmike.tlmreader.datablock.DataBlock;
import com.monstarmike.tlmreader.datablock.GForceBlock;
import com.monstarmike.tlmreader.datablock.HeaderBlock;
import com.monstarmike.tlmreader.datablock.HeaderNameBlock;
import com.monstarmike.tlmreader.datablock.PowerboxBlock;
import com.monstarmike.tlmreader.datablock.RXBlock;
import com.monstarmike.tlmreader.datablock.StandardBlock;
import com.monstarmike.tlmreader.datablock.VarioBlock;
import com.monstarmike.tlmreader.datablock.VoltageBlock;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class Exporter {
    Iterable<Flight> flights;
    Uri uri;
    Context context;

    private static final String TAG = "TLMDecoder";

    public Exporter(Uri uri, Iterable<Flight> flights, Context context) {
        this.flights = flights;
        this.uri = uri;
        this.context = context;
    }

    public JSONObject exportFlights() {
        JSONObject file = new JSONObject();

        JSONArray flightsArray = new JSONArray();
        try {
            file.put("uri", uri);
            file.put("flights", flightsArray);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int index = 0;
        for (Flight flight : this.flights) {
            JSONObject flightJO = this.buildFlight(false, flight);
            try {
                flightJO.put("_id", index++);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            flightsArray.put(flightJO);
        }

        return file;
    }

    public JSONObject exportFlightData(JSONObject flightJO) {
        JSONObject newFlightJO = null;
        int index = 0;
        for (Flight flight : this.flights) {
            int joId = 0;
            try {
                joId = flightJO.getInt("_id");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (index != joId) {
                Log.d(TAG, "Flight index: " + index + " != passed in value: " + joId);
                index++;
                continue;
            }

            Log.d(TAG, "Found flight. Going to do a full decode.");
            newFlightJO = this.buildFlight(true, flight);
            if (newFlightJO != null) {
                try {
                    newFlightJO.put("_id", joId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            break;
        }

        return newFlightJO;
    }

    private JSONObject buildFlight(boolean includeBlockData, Flight flight) {
        JSONObject flightJO = new JSONObject();
        try {
            flightJO.put("_id", flight.hashCode());

            Duration flightDuration = flight.get_duration();
            Period period = flightDuration.toPeriod();
            PeriodFormatter hms = new PeriodFormatterBuilder()
                    .appendHours()
                    .appendSeparator(":")
                    .printZeroAlways()
                    .appendMinutes()
                    .appendSeparator(":")
                    .appendSecondsWithMillis()
                    .toFormatter();

            flightJO.put("duration", hms.print(period));

            Iterator<HeaderBlock> iterator = flight.get_headerBlocks();
            while (iterator.hasNext()) {
                HeaderBlock headerBlock = iterator.next();
                if (headerBlock instanceof HeaderNameBlock) {
                    HeaderNameBlock nameBlock = (HeaderNameBlock) headerBlock;
                    flightJO.put("name", nameBlock.get_modelName());
                    flightJO.put("modelNumber", nameBlock.get_modelNumber());
                    flightJO.put("bindInfo", nameBlock.get_bindInfo());
                    flightJO.put("modelType", nameBlock.get_modelType());
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (includeBlockData) {
            JSONObject flightData = null;
            try {
                flightData = this.loadChartDataTemplate();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                flightJO.put("flightData", flightData);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Iterator<HeaderBlock> headerBlockIterator = flight.get_headerBlocks();
            while (headerBlockIterator.hasNext()) {
                HeaderBlock headerBlock = headerBlockIterator.next();
                try {
                    handleHeaderBlock(flightJO, headerBlock);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Iterator<DataBlock> dataBlockIterator = flight.get_dataBlocks();
            while (dataBlockIterator.hasNext()) {
                DataBlock dataBlock = dataBlockIterator.next();
                try {
                    handleDataBlock(flightData, dataBlock);
                } catch (JSONException e) {
                    Log.w(TAG, "JSON error when working with data block", e);
                }
            }
        }

        return flightJO;
    }

    private JSONObject loadChartDataTemplate() throws JSONException {
        BufferedReader reader = null;
        String jsonTemplate = null;
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("json/chartTemplate.json")));
            String line = reader.readLine();
            StringBuilder stringBuilder = new StringBuilder();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
            jsonTemplate = stringBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error while reading JSON template.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error while trying to close JSON template reader", e);
                }
            }
        }

        if (jsonTemplate == null || jsonTemplate.equalsIgnoreCase("")) {
            Log.e(TAG, "The template read from assets is null!");
        }

        return new JSONObject(jsonTemplate);
    }

    private JSONArray findDataPointsArray(JSONObject flightData, String sensorName,
                                          int seriesPosition, int dataSetPosition) throws JSONException {
        return flightData.getJSONObject(sensorName)
                .getJSONArray("chartSeriesTypes")
                .getJSONObject(seriesPosition)
                .getJSONArray("data")
                .getJSONObject(dataSetPosition)
                .getJSONArray("dataPoints");
    }

    void handleHeaderBlock(JSONObject jsonFlight, HeaderBlock headerBlock) throws JSONException {
        JSONObject jsonHeaderBlock = null;

        if (headerBlock instanceof HeaderNameBlock) {
            HeaderNameBlock nameBlock = (HeaderNameBlock) headerBlock;
            jsonHeaderBlock = new JSONObject();
            jsonHeaderBlock.put("modelNumber", nameBlock.get_modelNumber());
            jsonHeaderBlock.put("bindInfo", nameBlock.get_bindInfo());
            jsonHeaderBlock.put("modelName", nameBlock.get_modelName());
            jsonHeaderBlock.put("modelType", nameBlock.get_modelType());
        }

        if (jsonHeaderBlock == null) return;

        if (jsonFlight.has("headers")) {
            jsonFlight.getJSONArray("headers").put(jsonHeaderBlock);
        } else {
            JSONArray headersArray = new JSONArray();
            headersArray.put(jsonHeaderBlock);
            jsonFlight.put("headers", headersArray);
        }
    }

    void handleDataBlock(JSONObject flightData, DataBlock dataBlock) throws JSONException {
        if (dataBlock instanceof AirspeedBlock) {

        } else if (dataBlock instanceof AltitudeBlock) {
            JSONObject jsonBlock = new JSONObject();
            jsonBlock.put("x", dataBlock.get_timestamp());
            jsonBlock.put("y",
                    ((AltitudeBlock) dataBlock).get_altitudeInTenthsOfAMeter());

            this.findDataPointsArray(flightData, "altitude", 0, 0).put(jsonBlock);
        } else if (dataBlock instanceof CurrentBlock) {
            JSONObject jsonBlock = new JSONObject();
            jsonBlock.put("x", dataBlock.get_timestamp());

            CurrentBlock currentBlock = (CurrentBlock) dataBlock;
            jsonBlock.put("y", currentBlock.get_Current());

            this.findDataPointsArray(flightData, "current", 0, 0).put(jsonBlock);
        } else if (dataBlock instanceof GForceBlock) {
            JSONObject jsonBlock = new JSONObject();
            jsonBlock.put("x", dataBlock.get_timestamp());

            GForceBlock gfBlock = (GForceBlock) dataBlock;
            jsonBlock.put("maxX", gfBlock.get_maxX());
            jsonBlock.put("maxY", gfBlock.get_maxY());
            jsonBlock.put("maxZ", gfBlock.get_maxZ());
            jsonBlock.put("x", gfBlock.get_x());
            jsonBlock.put("y", gfBlock.get_y());
            jsonBlock.put("z", gfBlock.get_z());
            jsonBlock.put("minZ", gfBlock.get_minZ());

        } else if (dataBlock instanceof PowerboxBlock) {
            JSONObject v1Block = new JSONObject(),
                    v2Block = new JSONObject(),
                    cap1Block = new JSONObject(),
                    cap2Block = new JSONObject();

            v1Block.put("x", dataBlock.get_timestamp());
            v2Block.put("x", dataBlock.get_timestamp());
            cap1Block.put("x", dataBlock.get_timestamp());
            cap2Block.put("x", dataBlock.get_timestamp());

            PowerboxBlock pbBlock = (PowerboxBlock) dataBlock;
            cap1Block.put("y", pbBlock.get_capacityOne());
            cap2Block.put("y", pbBlock.get_capacityTwo());
            v1Block.put("y", pbBlock.get_voltageOne());
            v2Block.put("y", pbBlock.get_voltageTwo());

            this.findDataPointsArray(flightData, "powerbox", 0, 0).put(v1Block);
            this.findDataPointsArray(flightData, "powerbox", 0, 1).put(v2Block);
            this.findDataPointsArray(flightData, "powerbox", 1, 0).put(cap1Block);
            this.findDataPointsArray(flightData, "powerbox", 1, 1).put(cap2Block);
        } else if (dataBlock instanceof RXBlock) {
            RXBlock rxBlock = (RXBlock) dataBlock;

            JSONObject aBlock = new JSONObject(),
                    bBlock = new JSONObject(),
                    lBlock = new JSONObject(),
                    rBlock = new JSONObject(),
                    frameLossBlock = new JSONObject(),
                    holdsBlock = new JSONObject(),
                    voltsBlock = new JSONObject();

            aBlock.put("x", dataBlock.get_timestamp());
            bBlock.put("x", dataBlock.get_timestamp());
            lBlock.put("x", dataBlock.get_timestamp());
            rBlock.put("x", dataBlock.get_timestamp());
            frameLossBlock.put("x", dataBlock.get_timestamp());
            holdsBlock.put("x", dataBlock.get_timestamp());
            voltsBlock.put("x", dataBlock.get_timestamp());

            aBlock.put("y", rxBlock.get_a());
            bBlock.put("y", rxBlock.get_b());
            lBlock.put("y", rxBlock.get_l());
            rBlock.put("y", rxBlock.get_r());

            frameLossBlock.put("y", rxBlock.get_frameLoss());
            holdsBlock.put("y", rxBlock.get_holds());

            voltsBlock.put("y", rxBlock.get_volts());

            this.findDataPointsArray(flightData, "rx", 0, 0).put(aBlock);
            this.findDataPointsArray(flightData, "rx", 0, 1).put(bBlock);
            this.findDataPointsArray(flightData, "rx", 0, 2).put(lBlock);
            this.findDataPointsArray(flightData, "rx", 0, 3).put(rBlock);

            this.findDataPointsArray(flightData, "rx", 1, 0).put(frameLossBlock);
            this.findDataPointsArray(flightData, "rx", 1, 1).put(holdsBlock);

            this.findDataPointsArray(flightData, "rx", 2, 0).put(voltsBlock);
        } else if (dataBlock instanceof StandardBlock) {
            StandardBlock standard = (StandardBlock) dataBlock;

            JSONObject rpmBlock = new JSONObject(),
                    tempBlock = new JSONObject(),
                    voltBlock = new JSONObject();

            rpmBlock.put("x", dataBlock.get_timestamp());
            tempBlock.put("x", dataBlock.get_timestamp());
            voltBlock.put("x", dataBlock.get_timestamp());

            rpmBlock.put("y", standard.get_rpm());
            tempBlock.put("y", standard.get_temperature());
            voltBlock.put("y", standard.get_volt());

            this.findDataPointsArray(flightData, "standard", 0, 0).put(rpmBlock);
            this.findDataPointsArray(flightData, "standard", 1, 0).put(tempBlock);
            this.findDataPointsArray(flightData, "standard", 2, 0).put(voltBlock);

        } else if (dataBlock instanceof VarioBlock) {
//            VarioBlock varioBlock = (VarioBlock) dataBlock;
/*
            jsonBlock.put("delta1000ms", varioBlock.get_1000ms());
            jsonBlock.put("delta2000ms", varioBlock.get_2000ms());
            jsonBlock.put("delta250ms", varioBlock.get_250ms());
            jsonBlock.put("delta3000ms", varioBlock.get_3000ms());
            jsonBlock.put("delta500ms", varioBlock.get_500ms());
            jsonBlock.put("altitude", varioBlock.get_altitude());*/

        } else if (dataBlock instanceof VoltageBlock) {
//            VoltageBlock voltageBlock = (VoltageBlock) dataBlock;

//			jsonBlock.put("delta3000ms", voltageBlock.g);
//			jsonBlock.put("delta500ms", varioBlock.get_500ms());
//			jsonBlock.put("altitude", varioBlock.get_altitude());
        }
    }
}
