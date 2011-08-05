package icecube.daq.test;

import icecube.daq.payload.impl.PayloadFactory;
import icecube.daq.payload.ILoadablePayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.IWriteablePayload;
import icecube.daq.payload.PayloadException;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class BaseValidator
    implements PayloadValidator
{
    private static final Log LOG = LogFactory.getLog(BaseValidator.class);

    private boolean foundInvalid;
    private PayloadFactory factory;

    public boolean foundInvalid()
    {
        return foundInvalid;
    }

    static long getUTC(IUTCTime time)
    {
        if (time == null) {
            return -1L;
        }

        return time.longValue();
    }

    static void dumpPayloadBytes(IWriteablePayload payload)
    {
        ByteBuffer buf = ByteBuffer.allocate(payload.getPayloadLength());

        int len;
        try {
            len = payload.writePayload(false, 0, buf);
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            len = -1;
        } catch (PayloadException pe) {
            pe.printStackTrace();
            len = -1;
        }

        StringBuffer strbuf = new StringBuffer();
        for (int i = 0; i < buf.limit(); i++) {
            String str = Integer.toHexString(buf.get(i));
            if (str.length() < 2) {
                strbuf.append('0').append(str);
            } else if (str.length() > 2) {
                strbuf.append(str.substring(str.length() - 2));
            } else {
                strbuf.append(str);
            }
            strbuf.append(' ');
        }

        System.err.println("LEN "+len+" HEX "+strbuf.toString());
    }

    public boolean validate(ByteBuffer payBuf)
    {
        // assume stop messages are valid
        if (payBuf.limit() >= 4 && payBuf.getInt(0) == 4) {
            return true;
        }

        if (factory == null) {
            factory = new PayloadFactory(new MockBufferCache("Validator"));
        }

        IWriteablePayload payload;
        try {
            payload = factory.getPayload(payBuf, 0);
        } catch (Exception ex) {
            LOG.error("Couldn't create payload from byte buffer " + payBuf, ex);
            foundInvalid = true;
            return false;
        }

        try {
            ((ILoadablePayload) payload).loadPayload();
        } catch (Exception ex) {
            LOG.error("Couldn't load payload " + payload, ex);
            foundInvalid = true;
            return false;
        }

        boolean isValid = validate(payload);

        foundInvalid |= !isValid;

        payload.recycle();

        return isValid;
    }
}
