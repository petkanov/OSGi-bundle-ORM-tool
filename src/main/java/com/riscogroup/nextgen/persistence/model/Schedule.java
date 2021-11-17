package com.riscogroup.nextgen.home.api.rule.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.events.intrusion.IntrusionEvent;
import com.riscogroup.nextgen.home.api.events.intrusion.LocalTroubleEvent;
import com.riscogroup.nextgen.home.api.events.intrusion.TroubleEvent;
import com.riscogroup.nextgen.home.api.rule.schedule.DailyTask;
import com.riscogroup.nextgen.home.api.rule.schedule.MonthlyTask;
import com.riscogroup.nextgen.home.api.rule.schedule.ScheduledTask;
import com.riscogroup.nextgen.home.api.rule.schedule.SingleTask;
import com.riscogroup.nextgen.home.api.rule.schedule.TaskScheduler;
import com.riscogroup.nextgen.home.api.rule.schedule.WeeklyTask;
import com.riscogroup.nextgen.home.api.rule.schedule.YearlyTask;
import com.riscogroup.nextgen.home.api.services.DataService;

public class Schedule {
	public static final int SINGLE = 0; // using 'date'
	public static final int DAILY = 1; // using 'time'
	public static final int WEEKLY = 2; // using 'day_of_week' and 'time'
	public static final int MONTHLY = 3; // using 'day_of_month' and 'time'
	public static final int YEARLY = 4; // using 'time'
	
	public static Logger logger = LoggerFactory.getLogger(Schedule.class);

	private Integer uID;
	private ScheduleTime startTime;
	private ScheduleTime endTime;
	private Integer type;
	private List<Integer> dayOfWeek;
	private List<Integer> dayOfMonth;
	private List<Integer> monthsOfYear;
	private ScheduledTask scheduledTask;
	private Rule parent;
	private TaskScheduler taskScheduler;
	private DataService dataService;

	public Schedule(Integer uID) {
		this.uID = uID;
	}
	
	public Schedule() {
		this.dayOfWeek = new ArrayList<Integer>();
		this.dayOfMonth = new ArrayList<Integer>();
		this.monthsOfYear = new ArrayList<Integer>();
	}
	public Schedule(Rule parent) {
		this.parent = parent;
		this.dayOfWeek = new ArrayList<Integer>();
		this.dayOfMonth = new ArrayList<Integer>();
		this.monthsOfYear = new ArrayList<Integer>();
	}

	public Integer getUID() {
		return this.uID;
	}
	
	public void setUID(Integer uID) {
		this.uID = uID;
	}

	public void setParentRule(Rule parent) {
		this.parent = parent;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void startTimer() {
		// Prevent rule execution when system clock is not set
		if (Calendar.getInstance().get(Calendar.YEAR) < 2018) {
			logger.warn("Schedule with UID: {} will not run, because the system clock is not set", getUID());
			new LocalTroubleEvent(null, null, null, TroubleEvent.SYSTEM_TIME_STATUS, IntrusionEvent.EVENT_TRIGGERED).postEvent();
			logger.info("The system clock is not set. Sending Trouble event (only to cloud) with type SYSTEM_CLOCK_STATUS [{}]", IntrusionEvent.SYSTEM_DATE_STATUS);
			return;
		}

		if (getStartTime() == null) {
			logger.warn("Schedule with UID: {} will not run, because the star time is not not set", getUID());
			return;
		}

		final int hour = startTime.getHour();
		final int minutes = startTime.getMinute();
		final int day = !dayOfMonth.isEmpty() ? dayOfMonth.get(0) : 1;
		final String taskID = parent.getUID().toString() + "," + getUID().toString(); 		

		switch (type) {
			case Schedule.SINGLE:
				scheduledTask = new SingleTask(startTime, taskID, endTime);
				logger.info("Single timer will start on date {}", startTime.getDate());
				break;
			case Schedule.DAILY:
				scheduledTask = new DailyTask(hour, minutes, taskID, endTime);
				logger.info("Daily timer will start at time: hour {}, minutes {}", hour, minutes);
				break;
			case Schedule.WEEKLY:
				boolean[] activeDays = new boolean[7];
				logger.debug("dayOfWeek: {}", dayOfWeek);
				for (Iterator<Integer> i = dayOfWeek.iterator(); i.hasNext();) {
					activeDays[i.next()] = true;
				}
				scheduledTask = new WeeklyTask(hour, minutes, activeDays, taskID, endTime);
				logger.info("Weekly timer will start at time: hour {}, minutes {}, days: {}", hour, minutes, activeDays);
				break;
			case Schedule.MONTHLY:
				boolean[] activeMonths = new boolean [12];
				logger.debug("monthsOfYear: {}", monthsOfYear);
				for (Iterator<Integer> i = monthsOfYear.iterator(); i.hasNext();) {
					int index = i.next();
					// to avoid OutOfBounds exception
					if (index < activeMonths.length) {
						activeMonths[index] = true;
					}
					
				}
				scheduledTask = new MonthlyTask(day, hour, minutes, activeMonths, taskID, endTime);
				logger.debug("Monthly timer started with day: {}, hour: {}, minutes: {}, activeMonths: {}", day, hour, minutes, activeMonths);
				break;
			case Schedule.YEARLY:
				//take first day and month if the client did not provide them
				int month = !monthsOfYear.isEmpty() ? monthsOfYear.get(0):1;
				scheduledTask = new YearlyTask(month, day, hour, minutes, taskID, endTime);
				logger.debug("YearlyTask timer started with month: {}, day: {}, hour: {}, minutes: {}", month, day, hour, minutes);
				break;
		}

		logger.debug("Schedule with UID: {} and type: {} is added to task scheduler", getUID(), getType());
		if (scheduledTask != null) {
			scheduledTask.setExecutable(parent);
			scheduledTask.setDuration(parent.getDuration().longValue()); 
			taskScheduler.add(scheduledTask);
		}
	}

	public void stopTimer() {
		if (scheduledTask != null) {
			scheduledTask.destroy();
			taskScheduler.remove(scheduledTask);
		}
	}
	
	public void setType(Integer type) {
		this.type = type;
		registerForUpdate();
		logger.trace("({})", this.type);
	}

	public Integer getType() {
		logger.trace("returns {}", type);
		return type;
	}

	/**
	 * Set list of week days, 0-6 (0-Sunday)
	 * 
	 * @param dayOfWeek
	 */
	public void setDayOfWeek(List<Integer> dayOfWeek) {
		logger.trace("({})", dayOfWeek);
		if (dayOfWeek == null) {
			dayOfWeek = new ArrayList<Integer>();
		}
		this.dayOfWeek = dayOfWeek;
		registerForUpdate();
		logger.debug("size {}", dayOfWeek != null ? dayOfWeek.size() : "");
	}

	public List<Integer> getDayOfWeek() {
		logger.trace("returns {}", dayOfWeek);
		return dayOfWeek;
	}

	/**
	 * Set the day of month 1-31
	 * 
	 * @param dayOfMonth
	 */
	public void setDayOfMonth(List<Integer> dayOfMonth) {
		if (dayOfMonth == null)
			dayOfMonth = new ArrayList<Integer>();
		this.dayOfMonth = dayOfMonth;
		registerForUpdate();
		logger.trace("({})", dayOfMonth);
	}

	public List<Integer> getDayOfMonth() {
		logger.trace("returns {}", dayOfMonth);
		return dayOfMonth;
	}
	
	public ArrayList<Integer> getMonthsOfYear() {
		logger.trace("returns : {}", monthsOfYear);
		return new ArrayList<>(monthsOfYear);
	}

	public void setMonthsOfYear(List<Integer> monthsOfYear) {
		if (monthsOfYear == null) {
			monthsOfYear = new ArrayList<>();
		}
		
		this.monthsOfYear = monthsOfYear;
		registerForUpdate();
		logger.debug("size {}", monthsOfYear != null ? monthsOfYear.size() : "");
	}

	public ScheduleTime getStartTime() {
		logger.trace("returns {}", startTime);
		return startTime;
	}

	public void setEndTime(ScheduleTime endTime) {
		logger.trace("({})", endTime);
		if(endTime == null) {
			return;
		}

		this.endTime = endTime;
		registerForUpdate();
	}
	
	public ScheduleTime getEndTime() {
		logger.trace("returns {}", startTime);
		return endTime;
	}

	public void setStartTime(ScheduleTime startTime) {
		logger.trace("({})", startTime);
		if(startTime == null) {
			return;
		}

		this.startTime = startTime;
		registerForUpdate();
	}

	public Rule getParentRule() {
		logger.trace("returns {}", parent);
		return parent;
	}

	protected void registerForUpdate() {
		if(this.uID == null) {
			return;
		}
		if(dataService == null) {
			dataService = Activator.getService(DataService.class); 
		}
		dataService.registerForUpdate(this);
	}
	
	@Override
	public String toString() {
		return "Schedule: uid: " + getUID() + "type: " + getType() + ", daysOfWeek: " + getDayOfWeek() + ", ruleId: "
				+ getParentRule().getUID() + ", getStartTime(): " + getStartTime();
	}
}
