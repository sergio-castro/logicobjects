package org.logicobjects.adapter.objectadapters;

import java.util.Calendar;

import org.jpc.term.FloatTerm;
import org.jpc.term.Term;
import org.logicobjects.adapter.ObjectToTermAdapter;

public class CalendarToTermAdapter extends ObjectToTermAdapter<Calendar> {

	@Override
	public Term adapt(Calendar calendar) {
		long timeInMilliSeconds = calendar.getTimeInMillis();
		double timeInSeconds = ((double)timeInMilliSeconds)/1000;
		return new FloatTerm(timeInSeconds);
	}

}
