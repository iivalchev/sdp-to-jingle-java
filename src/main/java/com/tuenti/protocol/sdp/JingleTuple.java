package com.tuenti.protocol.sdp;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ;
import org.dom4j.Element;

import java.util.List;

/**
 * Created by Ivan Valchev (ivan.valchev@estafet.com) on 3/18/15.
 */
public class JingleTuple {
    private final JingleWithAdditionalInfo desc;
    private final List<JingleWithAdditionalInfo> transports;

    public JingleTuple(final JingleWithAdditionalInfo desc,
                       final List<JingleWithAdditionalInfo> transports) {
        this.desc = desc;
        this.transports = transports;
    }

    public JingleWithAdditionalInfo getDesc() {
        return desc;
    }

    public List<JingleWithAdditionalInfo> getTransports() {
        return transports;
    }

    @Override
    public String toString() {
        return "JingleTuple{" +
               "desc=" + desc +
               ", transports=" + transports +
               '}';
    }

    public static class JingleWithAdditionalInfo {
        private final JingleIQ jingleIQ;

        private final Element additionalInfo;

        public JingleWithAdditionalInfo(final JingleIQ jingleIQ) {
            this(jingleIQ, null);
        }

        public JingleWithAdditionalInfo(final JingleIQ jingleIQ, final Element additionalInfo) {
            this.jingleIQ = jingleIQ;
            this.additionalInfo = additionalInfo;
        }

        public JingleIQ getJingleIQ() {
            return jingleIQ;
        }

        public Element getAdditionalInfo() {
            return additionalInfo;
        }

        @Override
        public String toString() {
            return "JingleWithAdditionalInfo{" +
                   "jingleIQ=" + jingleIQ +
                   ", additionalInfo=" + additionalInfo +
                   '}';
        }
    }
}
