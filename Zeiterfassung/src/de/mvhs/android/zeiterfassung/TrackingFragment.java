package de.mvhs.android.zeiterfassung;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import de.mvhs.android.zeiterfassung.db.ZeitContracts;

public class TrackingFragment extends Fragment {

	private boolean _IsStarted = false;
	private Button _StartCommand = null;
	private Button _StopCommand = null;
	private EditText _StartTime = null;
	private EditText _EndTime = null;
	private long _CurrentId = -1;

	private final static String[] _SEARCH_PROJECTION = {
			ZeitContracts.Zeit.Columns._ID, ZeitContracts.Zeit.Columns.START };
	private final static String _SEARCH_SELECTION = "IFNULL("
			+ ZeitContracts.Zeit.Columns.END + ",'')=''";

	private final static DateFormat _UI_DATE_FORMATTER = DateFormat
			.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_tracking, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		// UI Elemente initialisieren
		_StartCommand = (Button) getActivity().findViewById(R.id.StartCommand);
		_StopCommand = (Button) getActivity().findViewById(R.id.EndCommand);
		_StartTime = (EditText) getActivity().findViewById(R.id.StartTime);
		_EndTime = (EditText) getActivity().findViewById(R.id.EndTime);

		// Click Event registrieren
		_StartCommand.setOnClickListener(new OnStartButtonClicked());
		_StopCommand.setOnClickListener(new OnEndButtonClicked());

		// Bearbeitung in den Textfeldern verbieten
		_StartTime.setKeyListener(null);
		_EndTime.setKeyListener(null);

		// Prüfen, ob ein angefangener Eintrag in der Datenbank vorliegt
		checkTrackState();
	}

	@Override
	public void onStop() {
		// Click Event deregistrieren
		_StartCommand.setOnClickListener(null);
		_StopCommand.setOnClickListener(null);

		super.onStop();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main_menu, menu);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Menüpunkt herausfinden
		switch (item.getItemId()) {
		case R.id.mnu_list:
			// Aktion für unser List-Menü-Eintrag
			Intent listIntent = new Intent(getActivity(),
					AuflistungActivity.class);
			startActivity(listIntent);
			break;

		case R.id.mnu_add:
			Intent editIntent = new Intent(getActivity(), EditActivity.class);
			startActivity(editIntent);
			break;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setButtonState() {

		_StartCommand.setEnabled(_IsStarted == false);
		_StopCommand.setEnabled(_IsStarted);
	}

	private void checkTrackState() {
		Cursor data = getActivity().getContentResolver().query(
				ZeitContracts.Zeit.CONTENT_URI, _SEARCH_PROJECTION,
				_SEARCH_SELECTION, null, null);

		if (data != null && data.moveToFirst()) {
			// Ein Eintrag gefunden
			_CurrentId = data.getLong(0);

			String startDate = data.getString(1);

			try {
				// Konvertierung des Datums aus der Datenbank
				Date startTime = ZeitContracts.Converters.DB_FORMATTER
						.parse(startDate);

				// Ausgabe an der UI
				_StartTime.setText(_UI_DATE_FORMATTER.format(startTime));

			} catch (ParseException e) {
				e.printStackTrace();
			}

			_EndTime.setText("");

			_IsStarted = true;
		} else {
			// Keine Einträge gefunden
			_StartTime.setText("");
			_EndTime.setText("");

			_IsStarted = false;
		}

		setButtonState();
	}

	private final class OnEndButtonClicked implements OnClickListener {
		@Override
		public void onClick(View v) {
			// Verhalten beim Click auf den Ende-Button
			Date currentTime = new Date();
			_EndTime.setText(_UI_DATE_FORMATTER.format(currentTime));

			ContentValues values = new ContentValues();
			values.put(ZeitContracts.Zeit.Columns.END,
					ZeitContracts.Converters.DB_FORMATTER.format(currentTime));

			Uri updateUri = ContentUris.withAppendedId(
					ZeitContracts.Zeit.CONTENT_URI, _CurrentId);

			getActivity().getContentResolver().update(updateUri, values, null,
					null);

			_CurrentId = -1;

			_IsStarted = false;
			setButtonState();
		}
	}

	private final class OnStartButtonClicked implements OnClickListener {
		@Override
		public void onClick(View v) {
			// Verhalten beim Klick auf den Strat Button
			Date currentTime = new Date();
			_StartTime.setText(_UI_DATE_FORMATTER.format(currentTime));

			ContentValues values = new ContentValues();
			values.put(ZeitContracts.Zeit.Columns.START,
					ZeitContracts.Converters.DB_FORMATTER.format(currentTime));

			Uri insertUri = getActivity().getContentResolver().insert(
					ZeitContracts.Zeit.CONTENT_URI, values);

			_CurrentId = ContentUris.parseId(insertUri);

			_IsStarted = true;
			setButtonState();
		}

	}
}