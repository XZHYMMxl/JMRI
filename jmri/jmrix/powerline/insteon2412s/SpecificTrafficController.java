// SpecificTrafficController.java

package jmri.jmrix.powerline.insteon2412s;

import java.util.logging.Level;
import java.util.logging.Logger;
import jmri.jmrix.AbstractMRListener;
import jmri.jmrix.AbstractMRMessage;
import jmri.jmrix.AbstractMRReply;
import jmri.jmrix.powerline.SerialTrafficController;
import jmri.jmrix.powerline.X10Sequence;
import jmri.jmrix.powerline.InsteonSequence;
import jmri.jmrix.powerline.SerialListener;
import jmri.jmrix.powerline.SerialMessage;
import jmri.jmrix.powerline.insteon2412s.Constants;

import java.io.DataInputStream;

/**
 * Converts Stream-based I/O to/from messages.  The "SerialInterface"
 * side sends/receives message objects.
 * <P>
 * The connection to
 * a SerialPortController is via a pair of *Streams, which then carry sequences
 * of characters for transmission.     Note that this processing is
 * handled in an independent thread.
 * <P>
 * This maintains a list of nodes, but doesn't currently do anything
 * with it.
 *
 * @author			Bob Jacobsen  Copyright (C) 2001, 2003, 2005, 2006, 2008, 2009
 * @author			Ken Cameron Copyright (C) 2010
 * @version			$Revision: 1.6 $
 */
public class SpecificTrafficController extends SerialTrafficController {

	public SpecificTrafficController() {
        super();
        logDebug = log.isDebugEnabled();
        
        // not polled at all, so allow unexpected messages, and
        // use poll delay just to spread out startup
        setAllowUnexpectedReply(true);
        mWaitBeforePoll = 1000;  // can take a long time to send

    }

    /**
     * Send a sequence of X10 messages
     * <p>
     * Makes them into the local messages and then queues in order
     */
    synchronized public void sendX10Sequence(X10Sequence s, SerialListener l) {
        s.reset();
        X10Sequence.Command c;
        while ( (c = s.getCommand() ) !=null) {
            SpecificMessage m;
            if (c.isAddress()) 
                m = SpecificMessage.getX10Address(c.getHouseCode(), ((X10Sequence.Address)c).getAddress());
            else {
                X10Sequence.Function f = (X10Sequence.Function)c;
                if (f.getDimCount() > 0)
                    m = SpecificMessage.getX10FunctionDim(f.getHouseCode(), f.getFunction(), f.getDimCount());
                else
                    m = SpecificMessage.getX10Function(f.getHouseCode(), f.getFunction());
            }
            sendSerialMessage(m, l);
            // Someone help me improve this
            // Without this wait, the commands are too close together and will return
            // an 0x15 which means they failed.
            // But there must be a better way to delay the sending of the next command.
            try {
                wait(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpecificTrafficController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Send a sequence of Insteon messages
     * <p>
     * Makes them into the local messages and then queues in order
     */
    synchronized public void sendInsteonSequence(InsteonSequence s, SerialListener l) {
        s.reset();
        InsteonSequence.Command c;
        while ( (c = s.getCommand() ) !=null) {
            SpecificMessage m;
            if (c.isAddress()) {
                // We should not get here
                // Clean this up later
                m = SpecificMessage.getInsteonAddress(-1, -1, -1);
            } else {
                InsteonSequence.Function f = (InsteonSequence.Function)c;
                m = SpecificMessage.getInsteonFunction(f.getAddressHigh(), f.getAddressMiddle(), f.getAddressLow(), f.getFunction(), f.getFlag(), f.getCommand1(), f.getCommand2());
            }
            sendSerialMessage(m, l);
            // Someone help me improve this
            // Without this wait, the commands are too close together and will return
            // an 0x15 which means they failed.
            // But there must be a better way to delay the sending of the next command.
 /*
            try {
                wait(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpecificTrafficController.class.getName()).log(Level.SEVERE, null, ex);
            }
 */
        }
    }

    /**
     * Get a message of a specific length for filling in.
     */
    public SerialMessage getSerialMessage(int length) {
        return new SpecificMessage(length);
    }

    protected void forwardToPort(AbstractMRMessage m, AbstractMRListener reply) {
        if (logDebug) log.debug("forward "+m);
        sendInterlock = ((SerialMessage)m).getInterlocked();
        super.forwardToPort(m, reply);
    }
        
    protected AbstractMRReply newReply() { 
        SpecificReply reply = new SpecificReply();
        return reply;
    }

    boolean sendInterlock = false; // send the 00 interlock when CRC received
    boolean expectLength = false;  // next byte is length of read
    boolean countingBytes = false; // counting remainingBytes into reply buffer
    int remainingBytes = 0;        // count of bytes _left_
    
    protected boolean endOfMessage(AbstractMRReply msg) {
        // check if this byte is length
        if (expectLength) {
            expectLength = false;
            countingBytes = true;
            remainingBytes = msg.getElement(1)&0xF; // 0 was the read command; max 9, really
            if (logDebug) log.debug("Receive count set to "+remainingBytes);
            return false;
        }
        if (remainingBytes>0) {
            if (remainingBytes>8) {
                log.error("Invalid remainingBytes: "+remainingBytes);
                remainingBytes = 0;
                return true;
            }
            remainingBytes--;
            if (remainingBytes == 0) {
                countingBytes = false;
                return true;  // done
            }
            return false; // wait for one more
        }
        // check for data available
        if ((msg.getElement(0)&0xFF)==Constants.FUNCTION_REQ_STD) {
            // get message
            SerialMessage m = new SpecificMessage(1);
            m.setElement(0, Constants.POLL_REQ_STD);
            expectLength = true;  // next byte is length
            forwardToPort(m, null);
            return false;  // reply message will get data appended            
        }
        // if the interlock is present, send it
        if (sendInterlock) {
        	if (logDebug) log.debug("Send interlock");
            sendInterlock = false;
            SerialMessage m = new SpecificMessage(1);
            m.setElement(0,0); // not really needed, but this is a slow protocol anyway
            forwardToPort(m, null);
            return false; // just leave in buffer
        }
        if (logDebug) log.debug("end of message: "+msg);
        return true;
    }

    /**
     * read a stream and pick packets out of it.
     * knows the size of the packets from the contents.
     */
    protected void loadChars(AbstractMRReply msg, DataInputStream istream) throws java.io.IOException {
        byte char1 = readByteProtected(istream);
        if (logDebug) log.debug("loadChars: " + char1);
        if ((char1 & 0xFF) == Constants.HEAD_STX) {  // 0x02 means start of command.
            msg.setElement(0, char1);
            byte char2 = readByteProtected(istream);
            if ((char2 & 0xFF) == Constants.FUNCTION_REQ_STD) {  // 0x62 means normal send command reply.
                msg.setElement(1, char2);
                byte addr1 = readByteProtected(istream);
                msg.setElement(2, addr1);
                byte addr2 = readByteProtected(istream);
                msg.setElement(3, addr2);
                byte addr3 = readByteProtected(istream);
                msg.setElement(4, addr3);
                byte flag1 = readByteProtected(istream);
                msg.setElement(5, flag1);
            	int bufsize = 2 + 1;
                if ((flag1 & Constants.FLAG_BIT_STDEXT) != 0x00) {
                	bufsize = 14 + 1;
                }
            	for (int i=6; i < (5 + bufsize); i++) {
                    byte byt = readByteProtected(istream);
                    msg.setElement(i, byt);
            	}
            } else if ((char2 & 0xFF) == Constants.FUNCTION_REQ_X10) {  // 0x63 means normal send X10 command reply.
                msg.setElement(1, char2);
                byte addrx1 = readByteProtected(istream);
                msg.setElement(2, addrx1);
                byte cmd1 = readByteProtected(istream);
                msg.setElement(3, cmd1);
                byte ack1 = readByteProtected(istream);
                msg.setElement(4, ack1);
            } else if ((char2 & 0xFF) == Constants.POLL_REQ_STD) {  // 0x50 means normal command received.
                msg.setElement(1, char2);
                for (int i = 2; i < (2 + 9); i++){
                    byte byt = readByteProtected(istream);
                    msg.setElement(2, byt);
                }
            } else if ((char2 & 0xFF) == Constants.POLL_REQ_EXT) {  // 0x51 means extended command received.
                msg.setElement(1, char2);
                for (int i = 2; i < (2 + 23); i++){
                    byte byt = readByteProtected(istream);
                    msg.setElement(2, byt);
                }
	        } else if ((char2 & 0xFF) == Constants.POLL_REQ_X10) {  // 0x52 means standard X10 received command.
	            msg.setElement(1, char2);
	            byte rawX10data = readByteProtected(istream);
	            msg.setElement(2, rawX10data);
	            byte X10Flag = readByteProtected(istream);
	            msg.setElement(3, X10Flag);
	            if (X10Flag == Constants.FLAG_X10_RECV_CMD) {
	            	if (logDebug) log.debug("loadChars: X10 Command Poll Received " + X10Sequence.houseValueToText((rawX10data & 0xF0) >> 4) + " " + X10Sequence.functionName((rawX10data & 0x0F)) );
	            } else {
	            	if (logDebug) log.debug("loadChars: X10 Unit Poll Received " + X10Sequence.houseValueToText((rawX10data & 0xF0) >> 4) + " " + X10Sequence.formatCommandByte(rawX10data));
	            }
	        } else if ((char2 & 0xFF) == Constants.POLL_REQ_BUTTON) {  // 0x54 means interface button received command.
	            msg.setElement(1, char2);
	            byte dat = readByteProtected(istream);
	            msg.setElement(2, dat);
	        } else if ((char2 & 0xFF) == Constants.POLL_REQ_BUTTON_RESET) {  // 0x55 means interface button received command.
	            msg.setElement(1, char2);
	        } else {
	            msg.setElement(1, char2);
	            if (logDebug) log.debug("loadChars: Unknown cmd byte " + char2);
	        }
        }
    }
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpecificTrafficController.class.getName());
}


/* @(#)SpecificTrafficController.java */
