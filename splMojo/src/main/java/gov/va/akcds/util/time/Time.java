package gov.va.akcds.util.time;

import java.util.Calendar;

public class Time {

	public static String now() {
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		return hour + ":" + min + ":" + sec;
	}
	
}
