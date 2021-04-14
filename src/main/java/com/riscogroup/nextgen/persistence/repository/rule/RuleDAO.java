package com.riscogroup.nextgen.persistence.repository.rule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.rule.data.ActionAddress;
import com.riscogroup.nextgen.home.api.rule.data.LocalAction;
import com.riscogroup.nextgen.home.api.rule.data.Rule;
import com.riscogroup.nextgen.home.api.rule.data.RuleTrigger;
import com.riscogroup.nextgen.home.api.rule.data.Schedule;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class RuleDAO extends DataAccessObject<Rule> {
	private static final Logger logger = LoggerFactory.getLogger(RuleDAO.class);

	@Override
	public boolean persist(Rule object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.rule ")
		  .append("(name, duration, execution_interval, is_enabled, manually_executable, enabled_on_vacation ) ")
		  .append("VALUES (?,?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setLong(2, object.getDuration() != null ? object.getDuration() : -1L);
		stmnt.setLong(3, object.getExecutionInterval() != null ? object.getExecutionInterval() : -1L);
		stmnt.setBoolean(4, object.isEnabled());
		stmnt.setBoolean(5, object.isManually());
		stmnt.setBoolean(6, object.isEnabledOnVacation());
		
		stmnt.executeUpdate();
		
		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next(); 
		final int id = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		object.setUID(id);
		return true;
	}

	@Override
	public boolean update(Rule object) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.rule ")
		  .append("SET name=?, duration=?, execution_interval=?, is_enabled=?, manually_executable=?, enabled_on_vacation=? ")
		  .append("WHERE id=?");
		
		final PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(sb.toString());
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setLong(2, object.getDuration() != null ? object.getDuration() : -1L);
		stmnt.setLong(3, object.getExecutionInterval() != null ? object.getExecutionInterval() : -1L);
		stmnt.setBoolean(4, object.isEnabled());
		stmnt.setBoolean(5, object.isManually());
		stmnt.setBoolean(6, object.isEnabledOnVacation());
		
		stmnt.setInt(7, object.getUID());
		
		stmnt.executeUpdate();
		return true;
	}

	@Override
	public List<Rule> getAllForID(int parentId) throws SQLException {
		return null;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final String query = "DELETE FROM home_automation.rule WHERE id=?";
		try (PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		if(getCache().get(objectId) != null) {
			Rule rule = getCache().get(objectId);
			for (LocalAction l : rule.getLocalActions()) {
				DaoRegistry.getInstance().getDAO(LocalAction.class.getName()).delete(l);
			}
			for (ActionAddress a : rule.getActionAddresses()) {
				DaoRegistry.getInstance().getDAO(ActionAddress.class.getName()).delete(a);
			}
			for (RuleTrigger rt : rule.getRuleTriggers()) {
				DaoRegistry.getInstance().getDAO(RuleTrigger.class.getName()).delete(rt);
			}
			for (Schedule s : rule.getSchedules()) {
				DaoRegistry.getInstance().getDAO(Schedule.class.getName()).delete(s);
			}
		}
		if(getCache().get(objectId) == null) {
			return true;
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(Rule rule) throws SQLException {
		getCache().remove(rule.getUID());
		return delete(rule.getUID());
	}

	@Override
	public Rule get(int objectId) throws SQLException {
		Rule actionAddress = getCache().get(objectId);
		if(actionAddress != null) {
			return actionAddress;
		}

		String query = "SELECT * FROM home_automation.rule WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					actionAddress = new Rule(objectId);
					getCache().addIfAbsent(objectId, actionAddress);
					
					fillWithData(actionAddress, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return actionAddress;
	}

	@Override
	public Map<Integer, Rule> getAll() throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM home_automation.rule");
		if(!getCache().isEmpty()) {
			sb.append(" WHERE id NOT IN(").append(getCache().getExistingIndexesString())
			.append(") ");
		}
		final Connection connection = getTransactionManager().getConnection();

		try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
				ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				final Rule user = new Rule( resultSet.getInt("id"));
				getCache().addIfAbsent(user.getUID(), user);
				fillWithData(user, resultSet);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return getCache().getAll();
	}

	@Override
	public void restoreObjectState(Rule rule) throws SQLException {
		if (rule == null || rule.getUID() == null) {
			return;
		}
		final String query = "SELECT * FROM home_automation.rule WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, rule.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(rule, rs);
					
				} else {
					getCache().remove(rule.getUID());
				}
			}
		} catch (Exception e) {
			getCache().remove(rule.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}

	private void fillWithData(Rule rule, ResultSet resultSet) throws SQLException {
		rule.setName(resultSet.getString("name"));
		rule.setDuration(resultSet.getLong("duration"));
		rule.setExecutionInterval(resultSet.getLong("execution_interval"));
		rule.setEnabled(resultSet.getBoolean("is_enabled"));
		rule.setManually(resultSet.getBoolean("manually_executable"));
		rule.setEnabledOnVacation(resultSet.getBoolean("enabled_on_vacation"));
		

		Map<Integer, LocalAction> localActions = new HashMap<>();
		Map<Integer, ActionAddress> actionAddresses = new HashMap<>();
		Map<Integer, RuleTrigger> ruleTriggers = new HashMap<>();
		Map<Integer, Schedule> schedules = new HashMap<>();
		
		List<Object> objects = DaoRegistry.getInstance().getDAO(LocalAction.class.getName()).getAllForID(rule.getUID());
		if(objects != null) {
			objects.forEach(o->localActions.put(((LocalAction)o).getUID(), (LocalAction) o) );
		}
		objects = DaoRegistry.getInstance().getDAO(ActionAddress.class.getName()).getAllForID(rule.getUID());
		if(objects != null) {
			objects.forEach(o->actionAddresses.put(((ActionAddress)o).getUID(), (ActionAddress) o) );
		}
		objects = DaoRegistry.getInstance().getDAO(RuleTrigger.class.getName()).getAllForID(rule.getUID());
		if(objects != null) {
			objects.forEach(o->ruleTriggers.put(((RuleTrigger)o).getUID(), (RuleTrigger) o) );
		}
		objects = DaoRegistry.getInstance().getDAO(Schedule.class.getName()).getAllForID(rule.getUID());
		if(objects != null) {
			objects.forEach(o->schedules.put(((Schedule)o).getUID(), (Schedule) o) );
		}
		
		rule.setLocalActions(localActions);
		rule.setActionAdresses(actionAddresses);
		rule.setRuleTriggers(ruleTriggers);
		rule.setSchedules(schedules);
	}
}
