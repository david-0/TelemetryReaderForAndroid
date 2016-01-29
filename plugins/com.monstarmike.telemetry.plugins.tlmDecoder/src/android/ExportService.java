package com.monstarmike.telemetry.plugins.tlmDecoder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.monstarmike.tlmreader.Flight;
import com.monstarmike.tlmreader.IFlight;
import com.monstarmike.tlmreader.TLMReader;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by mgardner on 6/5/15.
 */
public class ExportService extends IntentService {
	private static final String TAG = "ExportService";

	public ExportService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();

		Log.d(TAG, "Export Service action: " + action);
		if (action.equalsIgnoreCase(Constants.ExportServiceActions.READ_FLIGHT)) {
			Uri fileUri = ServiceDataTransfer.getInstance().get_fileUri();
			JSONObject flightJO = ServiceDataTransfer.getInstance().get_flight();

			if (fileUri == null) {
				Log.w(TAG, "The file Uri pulled from the ServiceDataTransfer singleton was null!");
			}
			if (flightJO == null) {
				Log.w(TAG, "The flight object pulled from the ServiceDataTransfer singleton was null!");
			}

			JSONObject decodedFlight = null;
			try {
				decodedFlight = this.decodeFlight(fileUri, flightJO);
			} catch (Exception e) {
				Log.e(TAG, "Error occurred decoding the flight!", e);
			}

			if (decodedFlight == null) {
				Log.w(TAG, "The decoded flight was null. This could be seriously not good!");
			}

			ServiceDataTransfer.getInstance().set_flight(decodedFlight);

			Intent localIntent = new Intent(Constants.READ_FLIGHT_BROADCAST_ACTION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		} else if (action.equalsIgnoreCase(Constants.ExportServiceActions.READ_FILE)) {
			Uri uri = intent.getData();

			Log.d(TAG, "Reading file: " + uri.toString());

			JSONObject file = null;

			try {
				file = this.decodeFlightDefinitions(uri);
			} catch (IOException e) {
				Log.e(TAG, "IO error when trying to get and parse the TLM data file.", e);
			}

			if (file == null) {
				Log.w(TAG, "This is interesting. The file JSONObject from the parse function is null!");
			}

			Log.d(TAG, "Done with the file. Broadcasting a local intent.");
			ServiceDataTransfer.getInstance().set_file(file);
			Intent localIntent = new Intent(Constants.READ_FILE_BROADCAST_ACTION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}

	private JSONObject decodeFlight(Uri uri, JSONObject flightJO) throws IOException {

		if (uri == null) {
			Log.w(TAG, "Returning the default flight object that was passed in.");
			return flightJO;
		} else {
			Log.d(TAG, "URI: " + uri.toString());
		}

		InputStream inputStream = null;
		try {
			inputStream = this.getContentResolver().openInputStream(uri);
		} catch (FileNotFoundException e) {
			Log.w(TAG, "Cannot find the file at " + uri.toString(), e);
		}
		if (inputStream == null) {
			Log.w(TAG, "Returning the default flight object that was passed in.");
			return flightJO;
		}

		int joId = 0;
		try {
			joId = flightJO.getInt("_id");
			Log.d(TAG, "FlightId: " + joId);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final TLMReader reader = new TLMReader();
		final Flight flight = reader.parseFlight(inputStream, joId);

		Exporter exporter = new Exporter(uri, this.getApplicationContext());
		Log.d(TAG, "Found flight. Going to do a full decode.");
		JSONObject exportFlightData = exporter.exportFlightData(flight);
		Log.d(TAG, "Full decode done");
		if (exportFlightData != null) {
			try {
				exportFlightData.put("_id", joId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return exportFlightData;

	}

	private JSONObject decodeFlightDefinitions(Uri uri) throws IOException {
		InputStream inputStream = this.getApplicationContext().getContentResolver().openInputStream(uri);
		Log.d(TAG, "Instantiate TLMReader.");
		List<IFlight> flights = new ArrayList<IFlight>();
		final TLMReader reader = new TLMReader();
		try {
			flights = reader.parseFlightDefinitions(inputStream);
		} catch (final IOException e) {
			Log.w(TAG, "Unable to read the input stream.", e);
		}
		Log.d(TAG, "Read bytes: " + reader.getNumberOfBytesRead());
		Exporter exporter = new Exporter(uri, this.getApplicationContext());
		JSONObject file = exporter.exportFlightDefintions(flights);
		return file;
	}
}
