package com.monstarmike.telemetry.plugins.tlmDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.monstarmike.tlmreader.Flight;
import com.monstarmike.tlmreader.IFlight;
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
	Uri uri;
	Context context;

	private static final String TAG = "TLMDecoder";

	public Exporter(Uri uri, Context context) {
		this.uri = uri;
		this.context = context;
	}

	public JSONObject exportFlightDefintions(List<IFlight> flightDefinitions) {
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
		for (IFlight flight : flightDefinitions) {
			JSONObject flightJO = this.buildFlightDefinition(flight);
			try {
				flightJO.put("_id", index++);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			flightsArray.put(flightJO);
		}

		return file;
	}

	public JSONObject exportFlightData(Flight flight) {
		JSONObject newFlightJO = this.buildFlightDefinition(flight);
		JSONObject flightData = null;
		try {
			flightData = this.loadChartDataTemplate();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			newFlightJO.put("flightData", flightData);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		for (HeaderBlock headerBlock : flight.getHeaderBlocks()) {
			try {
				this.handleHeaderBlock(newFlightJO, headerBlock);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		int i = 0;
		for (DataBlock dataBlock : flight.getDataBlocks()) {
			if (i % 100 == 0) {
				Log.d(TAG, "DataBlock: " + i);
			}
			i++;
			try {
				this.handleDataBlock(flightData, dataBlock, flight);
			} catch (JSONException e) {
				Log.w(TAG, "JSON error when working with data block", e);
			}
		}
		return newFlightJO;
	}

	private JSONObject buildFlightDefinition(IFlight flight) {
		JSONObject flightJO = new JSONObject();
		try {
			Duration flightDuration = flight.getDuration();
			Period period = flightDuration.toPeriod();
			PeriodFormatter hms = new PeriodFormatterBuilder().appendHours().appendSeparator(":").printZeroAlways()
					.appendMinutes().appendSeparator(":").appendSecondsWithMillis().toFormatter();

			flightJO.put("duration", hms.print(period));

			for (HeaderBlock headerBlock : flight.getHeaderBlocks()) {
				if (headerBlock instanceof HeaderNameBlock) {
					HeaderNameBlock nameBlock = (HeaderNameBlock) headerBlock;
					flightJO.put("name", nameBlock.getModelName());
					flightJO.put("modelNumber", nameBlock.getModelNumber());
					flightJO.put("bindInfo", nameBlock.getBindInfo());
					flightJO.put("modelType", nameBlock.getModelType());
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private JSONArray findDataPointsArray(JSONObject flightData, String sensorName, int seriesPosition,
			int dataSetPosition) throws JSONException {
		return flightData.getJSONObject(sensorName).getJSONArray("chartSeriesTypes").getJSONObject(seriesPosition)
				.getJSONArray("data").getJSONObject(dataSetPosition).getJSONArray("dataPoints");
	}

	void handleHeaderBlock(JSONObject jsonFlight, HeaderBlock headerBlock) throws JSONException {
		JSONObject jsonHeaderBlock = null;

		if (headerBlock instanceof HeaderNameBlock) {
			HeaderNameBlock nameBlock = (HeaderNameBlock) headerBlock;
			jsonHeaderBlock = new JSONObject();
			jsonHeaderBlock.put("modelNumber", nameBlock.getModelNumber());
			jsonHeaderBlock.put("bindInfo", nameBlock.getBindInfo());
			jsonHeaderBlock.put("modelName", nameBlock.getModelName());
			jsonHeaderBlock.put("modelType", nameBlock.getModelType());
		} else {
			return;
		}

		if (!jsonFlight.has("headers")) {
			jsonFlight.put("headers", new JSONArray());
		}
		jsonFlight.getJSONArray("headers").put(jsonHeaderBlock);
	}

	void handleDataBlock(JSONObject flightData, DataBlock dataBlock, Flight flight) throws JSONException {
		if (dataBlock instanceof AirspeedBlock) {
		} else if (dataBlock instanceof AltitudeBlock) {
			JSONObject jsonBlock = new JSONObject();
			jsonBlock.put("x", dataBlock.getTimestamp());
			jsonBlock.put("y", ((AltitudeBlock) dataBlock).getAltitudeInTenthOfMeter());

			this.findDataPointsArray(flightData, "altitude", 0, 0).put(jsonBlock);
		} else if (dataBlock instanceof CurrentBlock) {
			JSONObject jsonBlock = new JSONObject();
			jsonBlock.put("x", dataBlock.getTimestamp());

			CurrentBlock currentBlock = (CurrentBlock) dataBlock;
			jsonBlock.put("y", currentBlock.getCurrent());

			this.findDataPointsArray(flightData, "current", 0, 0).put(jsonBlock);
		} else if (dataBlock instanceof GForceBlock) {
			JSONObject jsonBlock = new JSONObject();
			jsonBlock.put("x", dataBlock.getTimestamp());

			GForceBlock gfBlock = (GForceBlock) dataBlock;
			jsonBlock.put("maxX", gfBlock.getMaxXInHunderthOfG());
			jsonBlock.put("maxY", gfBlock.getMaxYInHunderthOfG());
			jsonBlock.put("maxZ", gfBlock.getMaxZInHunderthOfG());
			jsonBlock.put("x", gfBlock.getXInHunderthOfG());
			jsonBlock.put("y", gfBlock.getYInHunderthOfG());
			jsonBlock.put("z", gfBlock.getZInHunderthOfG());
			jsonBlock.put("minZ", gfBlock.getMinZInHunderthOfG());

		} else if (dataBlock instanceof PowerboxBlock) {
			JSONObject v1Block = new JSONObject(), v2Block = new JSONObject(), cap1Block = new JSONObject(),
					cap2Block = new JSONObject();

			v1Block.put("x", dataBlock.getTimestamp());
			v2Block.put("x", dataBlock.getTimestamp());
			cap1Block.put("x", dataBlock.getTimestamp());
			cap2Block.put("x", dataBlock.getTimestamp());

			PowerboxBlock pbBlock = (PowerboxBlock) dataBlock;
			cap1Block.put("y", pbBlock.getCapacityOneInmAh());
			cap2Block.put("y", pbBlock.getCapacityTwoInmAh());
			v1Block.put("y", pbBlock.getVoltageOneInHunderthOfVolts());
			v2Block.put("y", pbBlock.getVoltageTwoInHunderthOfVolts());

			this.findDataPointsArray(flightData, "powerbox", 0, 0).put(v1Block);
			this.findDataPointsArray(flightData, "powerbox", 0, 1).put(v2Block);
			this.findDataPointsArray(flightData, "powerbox", 1, 0).put(cap1Block);
			this.findDataPointsArray(flightData, "powerbox", 1, 1).put(cap2Block);
		} else if (dataBlock instanceof RXBlock) {
			RXBlock rxBlock = (RXBlock) dataBlock;

			if (rxBlock.hasValidDataLostPacketsReceiverA()) {
				JSONObject aBlock = new JSONObject();
				aBlock.put("x", dataBlock.getTimestamp());
				aBlock.put("y", rxBlock.getLostPacketsReceiverA());
				this.findDataPointsArray(flightData, "rx", 0, 0).put(aBlock);
			}
			if (rxBlock.hasValidDataLostPacketsReceiverB()) {
				JSONObject bBlock = new JSONObject();
				bBlock.put("x", dataBlock.getTimestamp());
				bBlock.put("y", rxBlock.getLostPacketsReceiverB());
				this.findDataPointsArray(flightData, "rx", 0, 1).put(bBlock);
			}
			if (rxBlock.hasValidDataLostPacketsReceiverL()) {
				JSONObject lBlock = new JSONObject();
				lBlock.put("x", dataBlock.getTimestamp());
				lBlock.put("y", rxBlock.getLostPacketsReceiverL());
				this.findDataPointsArray(flightData, "rx", 0, 2).put(lBlock);
			}
			if (rxBlock.hasValidDataLostPacketsReceiverR()) {
				JSONObject rBlock = new JSONObject();
				rBlock.put("x", dataBlock.getTimestamp());
				rBlock.put("y", rxBlock.getLostPacketsReceiverR());
				this.findDataPointsArray(flightData, "rx", 0, 3).put(rBlock);
			}

			if (rxBlock.hasValidFrameLosssData()) {
				JSONObject frameLossBlock = new JSONObject();
				frameLossBlock.put("x", dataBlock.getTimestamp());
				frameLossBlock.put("y", rxBlock.getFrameLoss());
				this.findDataPointsArray(flightData, "rx", 1, 0).put(frameLossBlock);
			}
			if (rxBlock.hasValidHoldsData()) {
				JSONObject holdsBlock = new JSONObject();
				holdsBlock.put("x", dataBlock.getTimestamp());
				holdsBlock.put("y", rxBlock.getHolds());
				this.findDataPointsArray(flightData, "rx", 1, 1).put(holdsBlock);
			}

			JSONObject voltsBlock = new JSONObject();
			voltsBlock.put("x", dataBlock.getTimestamp());
			voltsBlock.put("y", rxBlock.getVoltageInHunderthOfVolts());
			this.findDataPointsArray(flightData, "rx", 2, 0).put(voltsBlock);
		} else if (dataBlock instanceof StandardBlock) {
			StandardBlock standard = (StandardBlock) dataBlock;

			if (flight.hasRpmHeader() && standard.hasValidRpmData()) {
				JSONObject rpmBlock = new JSONObject();
				rpmBlock.put("x", dataBlock.getTimestamp());
				rpmBlock.put("y", standard.getRpm());
				this.findDataPointsArray(flightData, "standard", 0, 0).put(rpmBlock);
			}
			if (standard.hasValidTemperatureData()) {
				JSONObject tempBlock = new JSONObject();
				tempBlock.put("x", dataBlock.getTimestamp());
				this.findDataPointsArray(flightData, "standard", 1, 0).put(tempBlock);
				tempBlock.put("y", standard.getTemperatureInGradFahrenheit());
			}
			if (standard.hasValidVoltageData()) {
				JSONObject voltBlock = new JSONObject();
				voltBlock.put("y", standard.getVoltageInHunderthOfVolts());
				voltBlock.put("x", dataBlock.getTimestamp());
				this.findDataPointsArray(flightData, "standard", 2, 0).put(voltBlock);
			}

		} else if (dataBlock instanceof VarioBlock) {
			// VarioBlock varioBlock = (VarioBlock) dataBlock;
			/*
			 * jsonBlock.put("delta1000ms", varioBlock.get_1000ms());
			 * jsonBlock.put("delta2000ms", varioBlock.get_2000ms());
			 * jsonBlock.put("delta250ms", varioBlock.get_250ms());
			 * jsonBlock.put("delta3000ms", varioBlock.get_3000ms());
			 * jsonBlock.put("delta500ms", varioBlock.get_500ms());
			 * jsonBlock.put("altitude", varioBlock.get_altitude());
			 */

		} else if (dataBlock instanceof VoltageBlock) {
			// VoltageBlock voltageBlock = (VoltageBlock) dataBlock;

			// jsonBlock.put("delta3000ms", voltageBlock.g);
			// jsonBlock.put("delta500ms", varioBlock.get_500ms());
			// jsonBlock.put("altitude", varioBlock.get_altitude());
		}
	}
}
