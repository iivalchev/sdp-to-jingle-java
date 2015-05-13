package com.tuenti.protocol.sdp;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQProvider;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

/**
 * Created by Ivan Valchev (ivan.valchev@estafet.com) on 3/13/15.
 */
public class XmlElementParser {
    public XmlElementParser() {
        final ProviderManager providerManager = ProviderManager.getInstance();
        providerManager.addIQProvider(JingleIQ.ELEMENT_NAME, JingleIQ.NAMESPACE, new JingleIQProvider());
    }

    public Element parse(final String input) throws DocumentException {
        return new SAXReader().read(new StringReader(input)).getRootElement();
    }

    public JingleIQ parseJingleIQ(final String input) throws Exception {
        final MXParser parser = new MXParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(input));
        parser.next();
        return (JingleIQ) PacketParserUtils.parseIQ(parser, null);
    }
}
