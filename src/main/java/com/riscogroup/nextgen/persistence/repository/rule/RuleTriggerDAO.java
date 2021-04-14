package com.riscogroup.nextgen.persistence.repository.rule;

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

import com.riscogroup.nextgen.home.api.rule.data.ActionAddress;
import com.riscogroup.nextgen.home.api.rule.data.Rule;
import com.riscogroup.nextgen.home.api.rule.data.RuleTrigger;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class RuleTriggerDAO extends DataAccessObject<RuleTrigger> {
	private static final Logger logger = LoggerFactory.getLogger(RuleTriggerDAO.class);

	@Override
	public boolean persist(RuleTrigger object) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.rule_trigger ")
		  .append("(event_type, action_address_id, group_ids_list, event_status, parent_rule_id) ")
		  .append("VALUES (?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setInt(1, object.getEventType() != null ? object.getEventType() : -1);
		stmnt.setInt(2, object.getActionAddress() != null ? object.getActionAddress().getUID() : -1);
		stmnt.setString(3, object.getGroupUIDs() != null ? object.getGroupUIDs().toString(): "");
		stmnt.setInt(4, object.getEventStatus() != null ? object.getEventStatus() : -1);
		stmnt.setInt(5, object.getParentRule() != null ? object.getParentRule().getUID() : -1);
		
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
	public boolean update(RuleTrigger object) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.rule_trigger ")
		  .append("SET event_type=?, action_address_id=?, group_ids_list=?,")
		  .append("event_status=?, parent_rule_id=? ")
		  .append("WHERE id=?");
		
		final PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(sb.toString());
		stmnt.setInt(1, object.getEventType() != null ? object.getEventType() : -1);
		stmnt.setInt(2, object.getActionAddress() != null ? object.getActionAddress().getUID() : -1);
		stmnt.setString(3, object.getGroupUIDs() != null ? object.getGroupUIDs().toString(): "");
		stmnt.setInt(4, object.getEventStatus() != null ? object.getEventStatus() : -1);
		stmnt.setInt(5, object.getParentRule() != null ? object.getParentRule().getUID() : -1);
		
		stmnt.setInt(6, object.getUID());
		
		stmnt.executeUpdate();
		return true;
	}

	@Override
	public List<RuleTrigger> getAllForID(int parentId) throws SQLException {
		final List<RuleTrigger> ruleTriggers = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.rule_trigger WHERE parent_rule_id=?";
		
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if(getCache().contains(resultSet.getInt("id"))) {
						ruleTriggers.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final RuleTrigger ruleTrigger = new RuleTrigger(resultSet.getInt("id"));
					getCache().addIfAbsent(ruleTrigger.getUID(), ruleTrigger);
					ruleTriggers.add(ruleTrigger);
					
					fillWithData(ruleTrigger, resultSet);
				}
			}
		} catch (SQLException e) {
			for(RuleTrigger r : ruleTriggers) {
				getCache().remove(r.getUID());
			}
			throw new SQLException(e);
		}
		return ruleTriggers;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final String query = "DELETE FROM home_automation.rule_trigger WHERE id=?";
		try (PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		if(getCache().get(objectId) != null) {
			RuleTrigger ruleTrugger = getCache().get(objectId);
			DaoRegistry.getInstance().getDAO(ActionAddress.class.getName()).delete( ruleTrugger.getActionAddress());
		}
		if(getCache().get(objectId) == null) {
			return true;
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(RuleTrigger obj) throws SQLException {
		getCache().remove(obj.getUID());
		return delete(obj.getUID());
	}

	@Override
	public RuleTrigger get(int objectId) throws SQLException {
		RuleTrigger ruleTrigger = getCache().get(objectId);
		if(ruleTrigger != null) {
			return ruleTrigger;
		}

		final String query = "SELECT * FROM home_automation.rule_trigger WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					ruleTrigger = new RuleTrigger(objectId);
					getCache().addIfAbsent(objectId, ruleTrigger);
					
					fillWithData(ruleTrigger, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return ruleTrigger;
	}

	@Override
	public Map<Integer, RuleTrigger> getAll() throws SQLException {
		return null;
	}

	@Override
	public void restoreObjectState(RuleTrigger ruleTrigger) throws SQLException {
		if (ruleTrigger == null || ruleTrigger.getUID() == null) {
			return;
		}
		final String query = "SELECT * FROM home_automation.rule_trigger WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, ruleTrigger.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(ruleTrigger, rs);
					
				} else {
					getCache().remove(ruleTrigger.getUID());
				}
			}
		} catch (Exception e) {
			getCache().remove(ruleTrigger.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}
	
	private void fillWithData(RuleTrigger ruleTrigger, ResultSet resultSet) throws SQLException {
		ruleTrigger.setEventType( resultSet.getInt("event_type"));
		
		final List<Integer> listInts = deserializeListFromToString(resultSet.getString("group_ids_list")).stream()
				.map(v -> Integer.parseInt(v)).collect(Collectors.toList());
		ruleTrigger.setGroupUIDs(listInts);
		ruleTrigger.setEventStatus( resultSet.getInt("event_status"));
		
		final ActionAddress actionAddress = (ActionAddress) DaoRegistry.getInstance().getDAO(ActionAddress.class.getName()).get(resultSet.getInt("action_address_id"));
		ruleTrigger.setActionAddress(actionAddress);
		
		final Rule rule = (Rule) DaoRegistry.getInstance().getDAO(Rule.class.getName()).get(resultSet.getInt("parent_rule_id"));
		ruleTrigger.setParentRule(rule);
	}
}
