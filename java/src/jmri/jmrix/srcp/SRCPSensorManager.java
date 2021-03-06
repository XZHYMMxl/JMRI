package jmri.jmrix.srcp;

import jmri.Sensor;

/**
 * Implement Sensor manager for SRCP systems.
 * <p>
 * System names are "DSnnn", where D is the user configurable system prefix,
 * nnn is the sensor number without padding.
 *
 * @author	Bob Jacobsen Copyright (C) 2001, 2008
 */
public class SRCPSensorManager extends jmri.managers.AbstractSensorManager {

    SRCPBusConnectionMemo _memo = null;
    int _bus;

    public SRCPSensorManager(SRCPBusConnectionMemo memo, int bus) {
        _memo = memo;
        _bus = bus;
    }

    @Override
    public String getSystemPrefix() {
        return _memo.getSystemPrefix();
    }

    @Override
    public Sensor createNewSensor(String systemName, String userName) {
        Sensor t;
        int addr = Integer.parseInt(systemName.substring(getSystemPrefix().length() + 1));
        t = new SRCPSensor(addr, _memo);
        t.setUserName(userName);

        return t;
    }

    /**
     * @deprecated JMRI Since 4.4 instance() shouldn't be used, convert to JMRI multi-system support structure
     */
    @Deprecated
    static public SRCPSensorManager instance() {
        return null;
    }

}
