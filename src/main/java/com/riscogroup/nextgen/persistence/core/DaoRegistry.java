package com.riscogroup.nextgen.persistence.core;

import java.util.HashMap;
import java.util.Map;

import com.riscogroup.nextgen.home.api.device.type.ZWaveDevice;
import com.riscogroup.nextgen.home.api.device.type.ZwaveMultiChannelDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.CODevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.ControlPanel;
import com.riscogroup.nextgen.home.api.device.type.intrusion.ControlPanelBell;
import com.riscogroup.nextgen.home.api.device.type.intrusion.FloodDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.IntrusionDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.KeyPadDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.MWDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.MagneticDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.PIRCameraDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.PIRDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.SirenDevice;
import com.riscogroup.nextgen.home.api.device.type.intrusion.SmokeDevice;
import com.riscogroup.nextgen.home.api.function.type.ZWaveDeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProperty;
import com.riscogroup.nextgen.home.api.generic.DeviceTroublesReportRecord;
import com.riscogroup.nextgen.home.api.generic.Group;
import com.riscogroup.nextgen.home.api.generic.User;
import com.riscogroup.nextgen.home.api.rule.data.ActionAddress;
import com.riscogroup.nextgen.home.api.rule.data.LocalAction;
import com.riscogroup.nextgen.home.api.rule.data.Rule;
import com.riscogroup.nextgen.home.api.rule.data.RuleTrigger;
import com.riscogroup.nextgen.home.api.rule.data.Schedule;
import com.riscogroup.nextgen.persistence.repository.device.DeviceFunctionDAO;
import com.riscogroup.nextgen.persistence.repository.device.DevicePropertyDAO;
import com.riscogroup.nextgen.persistence.repository.device.IntrusionDeviceDAO;
import com.riscogroup.nextgen.persistence.repository.device.KeyPadDeviceDAO;
import com.riscogroup.nextgen.persistence.repository.device.ZWaveDeviceDAO;
import com.riscogroup.nextgen.persistence.repository.device.ZWaveDeviceFunctionDAO;
import com.riscogroup.nextgen.persistence.repository.device.ZwaveMultiChannelDeviceDAO;
import com.riscogroup.nextgen.persistence.repository.group.DeviceTroubleReportDAO;
import com.riscogroup.nextgen.persistence.repository.group.GroupDAO;
import com.riscogroup.nextgen.persistence.repository.rule.ActionAddressDAO;
import com.riscogroup.nextgen.persistence.repository.rule.LocalActionDAO;
import com.riscogroup.nextgen.persistence.repository.rule.RuleDAO;
import com.riscogroup.nextgen.persistence.repository.rule.RuleTriggerDAO;
import com.riscogroup.nextgen.persistence.repository.rule.ScheduleDAO;
import com.riscogroup.nextgen.persistence.repository.user.UserDAO;

public class DaoRegistry {
	
	private final Map<String, DataAccessObject<?>> registry = new HashMap<>();
	
	private DaoRegistry() {
		registry.put(User.class.getName(), new UserDAO());
		registry.put(Group.class.getName(), new GroupDAO());
		registry.put(DeviceTroublesReportRecord.class.getName(), new DeviceTroubleReportDAO());
		registry.put(DeviceProperty.class.getName(), new DevicePropertyDAO());
		registry.put(DeviceFunction.class.getName(), new DeviceFunctionDAO());
		registry.put(ZWaveDeviceFunction.class.getName(), new ZWaveDeviceFunctionDAO());
		registry.put(KeyPadDevice.class.getName(), new KeyPadDeviceDAO());
		registry.put(IntrusionDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(CODevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(ControlPanel.class.getName(), new IntrusionDeviceDAO());
		registry.put(ControlPanelBell.class.getName(), new IntrusionDeviceDAO());
		registry.put(FloodDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(MagneticDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(MWDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(PIRCameraDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(PIRDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(SirenDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(SmokeDevice.class.getName(), new IntrusionDeviceDAO());
		registry.put(ZWaveDevice.class.getName(), new ZWaveDeviceDAO());
		registry.put(ZwaveMultiChannelDevice.class.getName(), new ZwaveMultiChannelDeviceDAO());
		
		registry.put(Schedule.class.getName(), new ScheduleDAO());
		registry.put(RuleTrigger.class.getName(), new RuleTriggerDAO());
		registry.put(ActionAddress.class.getName(), new ActionAddressDAO());
		registry.put(LocalAction.class.getName(), new LocalActionDAO());
		registry.put(Rule.class.getName(), new RuleDAO());
	}
	
	private static class DaoRegistryMaker {
		private static final DaoRegistry INSTANCE = new DaoRegistry();
	}

	public static DaoRegistry getInstance() {
		return DaoRegistryMaker.INSTANCE;
	}
	
	@SuppressWarnings("unchecked")
	public <T> DataAccessObject<T> getDAO(String className){
		return (DataAccessObject<T>) registry.get(className);
	}
}