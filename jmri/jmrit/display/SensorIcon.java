package jmri.jmrit.display;

import javax.swing.*;
import java.awt.event.*;

import jmri.*;

/**
 * SensorIcon provides a small icon to display a status of a Sensor.</p>
 * @author Bob Jacobsen
 * @version $Revision: 1.3 $
 */

public class SensorIcon extends PositionableLabel implements java.beans.PropertyChangeListener {

    public SensorIcon() {
        // super ctor call to make sure this is an icon label
        super(new ImageIcon(ClassLoader.getSystemResource("resources/icons/smallschematics/tracksegments/circuit-error.gif")),
            "sensor icon name");
        displayState(sensorState());
    }

    // what to display - at least one should be true!
    boolean showText = false;
    boolean showIcon = true;

    // the associated Sensor object
    Sensor sensor = null;

    /**
     * Attached a named sensor to this display item
     * @param name Used as a user name to lookup the sensor object
     */
    public void setSensor(String name) {
        sensor = InstanceManager.sensorManagerInstance().
            newSensor(null,name);
        sensor.addPropertyChangeListener(this);
    }
    public Sensor getSensor() {
        return sensor;
    }

    // display icons
    String activeName = "resources/icons/smallschematics/tracksegments/circuit-occupied.gif";
    Icon active = new ImageIcon(ClassLoader.getSystemResource(activeName));

    String inactiveName = "resources/icons/smallschematics/tracksegments/circuit-empty.gif";
    Icon inactive = new ImageIcon(ClassLoader.getSystemResource(inactiveName));

    String inconsistentName = "resources/icons/smallschematics/tracksegments/circuit-error.gif";
    Icon inconsistent = new ImageIcon(ClassLoader.getSystemResource(inconsistentName));

    String unknownName = "resources/icons/smallschematics/tracksegments/circuit-error.gif";
    Icon unknown = new ImageIcon(ClassLoader.getSystemResource(unknownName));

    public Icon getActiveIcon() { return active; }
    public String getActiveIconName() { return activeName; }
    public void setActiveIcon(Icon i, String n) { active = i; activeName = n; displayState(sensorState()); }

    public Icon getInactiveIcon() { return inactive; }
    public String getInactiveIconName() { return inactiveName; }
    public void setInactiveIcon(Icon i, String n) { inactive = i; inactiveName = n; displayState(sensorState()); }

    public Icon getInconsistentIcon() { return inconsistent; }
    public String getInconsistentIconName() { return inconsistentName; }
    public void setInconsistentIcon(Icon i, String n) { inconsistent = i; inconsistentName = n; displayState(sensorState()); }

    public Icon getUnknownIcon() { return unknown; }
    public String getUnknownIconName() { return unknownName; }
    public void setUnknownIcon(Icon i, String n) { unknown = i; unknownName = n; displayState(sensorState()); }

    public int getHeight() {
        return Math.max(
            Math.max(active.getIconHeight(), inactive.getIconHeight()),
            Math.max(inconsistent.getIconHeight(), unknown.getIconHeight())
            );
    }

    public int getWidth() {
        return Math.max(
            Math.max(active.getIconWidth(), inactive.getIconWidth()),
            Math.max(inconsistent.getIconWidth(), unknown.getIconWidth())
            );
    }

    /**
     * Get current state of attached sensor
     * @return A state variable from a Sensor, e.g. Sensor.ACTIVE
     */
    int sensorState() {
        if (sensor != null) return sensor.getKnownState();
        else return Sensor.UNKNOWN;
    }

	// update icon as state of turnout changes
	public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (log.isDebugEnabled()) log.debug("property change: "+e);
		if (e.getPropertyName().equals("KnownState")) {
            int now = ((Integer) e.getNewValue()).intValue();
             displayState(now);
		}
	}

    JPopupMenu popup = null;
    /**
     * Pop-up just displays the sensor name
     */
    protected void showPopUp(MouseEvent e) {
        if (popup==null) {
            String name;
            name = sensor.getID();
            popup = new JPopupMenu();
            popup.add(new JMenuItem(name));
        }
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Drive the current state of the display from the state of the
     * turnout.
     */
    void displayState(int state) {
        switch (state) {
            case Sensor.UNKNOWN:
                if (showText) super.setText("<unknown>");
                if (showIcon) super.setIcon(unknown);
                return;
            case Sensor.ACTIVE:
            	if (showText) super.setText("Active");
                if (showIcon) super.setIcon(active);
                return;
            case Sensor.INACTIVE:
                if (showText) super.setText("Inactive");
                if (showIcon) super.setIcon(inactive);
                return;
            default:
                if (showText) super.setText("<inconsistent>");
                if (showIcon) super.setIcon(inconsistent);
                return;
			}
    }

    /**
     * (Temporarily) change occupancy on click
     * @param e
     */
    public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.isAltDown() || e.isMetaDown()) return;
        try {
            if (sensor.getKnownState()==jmri.Sensor.ACTIVE)
                sensor.setKnownState(jmri.Sensor.INACTIVE);
            else
                sensor.setKnownState(jmri.Sensor.ACTIVE);
        } catch (jmri.JmriException reason) {
            log.warn("Exception changing sensor: "+reason);
        }
    }


    static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(SensorIcon.class.getName());
}