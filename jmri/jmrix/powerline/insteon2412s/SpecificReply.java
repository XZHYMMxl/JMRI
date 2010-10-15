// SpecificReply.java

package jmri.jmrix.powerline.insteon2412s;

import jmri.jmrix.powerline.SerialReply;
import jmri.jmrix.powerline.X10Sequence;

/**
 * Contains the data payload of a serial reply
 * packet.  Note that its _only_ the payload.
 *
 * @author	Bob Jacobsen  Copyright (C) 2002, 2006, 2007, 2008, 2009
 * @version     $Revision: 1.6 $
 */
public class SpecificReply extends jmri.jmrix.powerline.SerialReply {

    // create a new one
    public  SpecificReply() {
        super();
        setBinary(true);
    }
    public SpecificReply(String s) {
        super(s);
        setBinary(true);
    }
    public SpecificReply(SerialReply l) {
        super(l);
        setBinary(true);
    }

	public String toMonitorString() {
        // check for valid length
        int len = getNumDataElements();
        StringBuilder text = new StringBuilder();
        if ((getElement(0)& 0xFF) != Constants.HEAD_STX) {
        	text.append("INVALID HEADER: " + String.format("0x%1X", getElement(0)& 0xFF));
        	text.append(" len: " + len);
        } else {
	        switch (getElement(1)& 0xFF) {
	            case Constants.FUNCTION_REQ_STD :
	            	text.append("Send Cmd ");
	            	if (len == 8 || len == 22) {
		            	if ((getElement(5) & Constants.FLAG_BIT_STDEXT) == Constants.FLAG_STD) {
		            		text.append(" Std");
		            	} else if (len == 22) {
		            		text.append(" Ext");
		            	}
		            	text.append(" addr " + String.format("%1$X.%2$X.%3$X", (getElement(2) & 0xFF), (getElement(3) & 0xFF), (getElement(4) & 0xFF)));
		            	switch (getElement(6) & 0xFF) {
		            	case Constants.CMD_LIGHT_ON:
		            		text.append(" ON ");
		            		text.append((getElement(7) & 0xFF) / 256.0);
		            		break;
		            	case Constants.CMD_LIGHT_OFF:
		            		text.append(" ON ");
		            		text.append((getElement(7) & 0xFF) / 256.0);
		            		break;
		            	case Constants.CMD_LIGHT_CHG:
		            		text.append(" CHG ");
		            		text.append((getElement(7) & 0xFF) / 256.0);
		            		break;
	            		default:
	            			text.append(" Unknown cmd: " + String.format("0x%1$2.2X", getElement(6) & 0xFF));
	            			break;
		            	}
	            	} else {
	            		text.append(" !! Length wrong: " + len);
	            	}
	            	break;
	            case Constants.POLL_REQ_BUTTON :
	            	text.append("Poll Button ");
	            	int button = ((getElement(2) & Constants.BUTTON_BITS_ID) >> 4) + 1;
	            	text.append(button);
	            	int op = getElement(2) & Constants.BUTTON_BITS_OP;
	            	if (op == Constants.BUTTON_HELD) {
		            	text.append(" HELD");
	            	} else if (op == Constants.BUTTON_REL) {
		            	text.append(" RELEASED");
	            	} else if (op == Constants.BUTTON_TAP) {
		            	text.append(" TAP");
	            	}
	            	break;
	            case Constants.POLL_REQ_BUTTON_RESET :
	            	text.append("Reset by Button at Power Cycle");
	            	break;
	            case Constants.FUNCTION_REQ_X10 :
	            	text.append("Send Cmd X10 ");
                    if ((getElement(3)& Constants.FLAG_BIT_X10_CMDUNIT) == Constants.FLAG_X10_RECV_CMD) {
                    	text.append(X10Sequence.formatCommandByte(getElement(2) & 0xFF));
                    } else {
                    	text.append(X10Sequence.formatAddressByte(getElement(2)& 0xFF));
                    }
	            	break;
	            case Constants.POLL_REQ_X10 :
	            	text.append("Poll Cmd X10 ");
                    if ((getElement(3)& Constants.FLAG_BIT_X10_CMDUNIT) == Constants.FLAG_X10_RECV_CMD) {
                    	text.append(X10Sequence.formatCommandByte(getElement(2) & 0xFF));
                    } else {
                    	text.append(X10Sequence.formatAddressByte(getElement(2)& 0xFF));
                    }
	            	break;
	            default: {
	            	text.append(" Unknown command: " + String.format("0x%1$2.2X", getElement(1)& 0xFF));
	            	text.append(" len: " + len);
	            }
	        }
        }
        return text+"\n";
    }
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpecificReply.class.getName());

}

/* @(#)SpecificReply.java */
