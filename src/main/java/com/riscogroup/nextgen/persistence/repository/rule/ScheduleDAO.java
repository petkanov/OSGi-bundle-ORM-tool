package com.riscogroup.nextgen.persistence.repository.rule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.rule.data.ScheduleTime;
import com.riscogroup.nextgen.home.api.rule.data.Rule;
import com.riscogroup.nextgen.home.api.rule.data.Schedule;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class ScheduleDAO extends DataAccessObject<Schedule> {
	private static final Logger logger = LoggerFactory.getLogger(ScheduleDAO.class);

	@Override
	public boolean persist(Schedule object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.schedule ")
		  .append("(start_time, end_time, type, day_of_week_list, day_of_month_list, months_of_year_list, parent_rule_id) ")
		  .append("VALUES (?,?,?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getStartTime() != null ? object.getStartTime().serializeObject() : "");
		stmnt.setString(2, object.getEndTime() != null ? object.getEndTime().serializeObject() : "");
		stmnt.setInt(3, object.getType() != null ? object.getType() : -1);
		stmnt.setString(4, object.getDayOfWeek() != null ? object.getDayOfWeek().toString() : "");
		stmnt.setString(5, object.getDayOfMonth() != null ? object.getDayOfMonth().toString() : "");
		stmnt.setString(6, object.getMonthsOfYear() != null ? object.getMonthsOfYear().toString() : "");
		stmnt.setInt(7, object.getParentRule() != null ? object.getParentRule().getUID() : -1);
		
		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next(); 
		final int deviceFunctionId = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		object.setUID(deviceFunctionId);
		return true;
	}

	@Override
	public boolean update(Schedule object) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.schedule ")
		  .append("SET start_time=?, end_time=?, type=?, day_of_week_list=?, day_of_month_list=?, ")
		  .append("months_of_year_list=?, parent_rule_id=? ")
		  .append("WHERE id=?");
		
		final PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(sb.toString());
		stmnt.setString(1, object.getStartTime() != null ? object.getStartTime().serializeObject() : "");
		stmnt.setString(2, object.getEndTime() != null ? object.getEndTime().serializeObject() : "");
		stmnt.setInt(3, object.getType() != null ? object.getType() : -1);
		stmnt.setString(4, object.getDayOfWeek() != null ? object.getDayOfWeek().toString() : "");
		stmnt.setString(5, object.getDayOfMonth() != null ? object.getDayOfMonth().toString() : "");
		stmnt.setString(6, object.getMonthsOfYear() != null ? object.getMonthsOfYear().toString() : "");
		stmnt.setInt(7, object.getParentRule() != null ? object.getParentRule().getUID() : -1);
		
		stmnt.setInt(8, object.getUID());
		
		stmnt.executeUpdate();
		return true;
	}

	@Override
	public List<Schedule> getAllForID(int parentId) throws SQLException {
		final List<Schedule> schedules = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.schedule WHERE parent_rule_id=?";
		
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if(getCache().contains(resultSet.getInt("id"))) {
						schedules.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final Schedule schedule = new Schedule(resultSet.getInt("id"));
					getCache().addIfAbsent(schedule.getUID(), schedule);
					schedules.add(schedule);
					
					fillWithData(schedule, resultSet);
				}
			}
		} catch (SQLException e) {
			for(Schedule s : schedules) {
				getCache().remove(s.getUID());
			}
			throw new SQLException(e);
		}
		return schedules;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final String query = "DELETE FROM home_automation.schedule WHERE id=?";
		try (PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		if(getCache().get(objectId) == null) {
			return true;
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(Schedule obj) throws SQLException {
		getCache().remove(obj.getUID());
		return delete(obj.getUID());
	}

	@Override
	public Schedule get(int objectId) throws SQLException {
		Schedule schedule = getCache().get(objectId);
		if(schedule != null) {
			return schedule;
		}

		String query = "SELECT * FROM home_automation.schedule WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					schedule = new Schedule(objectId);
					getCache().addIfAbsent(objectId, schedule);
					
					fillWithData(schedule, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return schedule;
	}

	@Override
	public Map<Integer, Schedule> getAll() throws SQLException {
		return null;
	}

	@Override
	public void restoreObjectState(Schedule schedule) throws SQLException {
		if (schedule == null || schedule.getUID() == null) {
			return;
		}
		final String query = "SELECT * FROM home_automation.schedule WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, schedule.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(schedule, rs);
					
				} else {
					getCache().remove(schedule.getUID());
				}
			}
		} catch (Exception e) {
			getCache().remove(schedule.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}
	
	private void fillWithData(Schedule schedule, ResultSet resultSet) throws SQLException {
		schedule.setStartTime(new ScheduleTime().deserializeObject( resultSet.getString("start_time")));
		schedule.setEndTime(new ScheduleTime().deserializeObject( resultSet.getString("end_time")));
		schedule.setType(resultSet.getInt("type"));
		
		List<Integer> listInts = deserializeListFromToString(resultSet.getString("day_of_week_list")).stream()
				.map(v -> Integer.parseInt(v)).collect(Collectors.toList());
		schedule.setDayOfWeek(listInts);
		listInts = deserializeListFromToString(resultSet.getString("day_of_month_list")).stream()
				.map(v -> Integer.parseInt(v)).collect(Collectors.toList());
		schedule.setDayOfMonth(listInts);
		listInts = deserializeListFromToString(resultSet.getString("months_of_year_list")).stream()
				.map(v -> Integer.parseInt(v)).collect(Collectors.toList());
		schedule.setMonthsOfYear(listInts);
		
		final Rule rule = new Rule(99); //(_Rule) DaoRegistry.getInstance().getDAO(RuleDAO.class).get(resultSet.getInt("parent_rule_id"));
		schedule.setParentRule(rule);
	}
}
