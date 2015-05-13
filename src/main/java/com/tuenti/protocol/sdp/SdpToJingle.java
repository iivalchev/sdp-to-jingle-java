package com.tuenti.protocol.sdp;

import com.tuenti.protocol.sdp.JingleTuple.JingleWithAdditionalInfo;
import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.GTalkCandidatePacketExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.GTalkTransportPacketExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.sourceforge.jsdp.*;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Converts a PeerConnection SDP Message to Jingle and vice-versa.
 * <p/>
 * Copyright (c) Tuenti Technologies. All rights reserved.
 *
 * @author Wijnand Warren <wwarren@tuenti.com>
 * @author Manuel Peinado Gallego <mpeinado@tuenti.com>
 */
public class SdpToJingle {

    public static final String LOCALHOST = "127.0.0.1";

    public static final int CANDIDATE_FOUNDATION = 0;
    public static final int CANDIDATE_COMPONENT = 1;
    public static final int CANDIDATE_PROTOCOL = 2;
    public static final int CANDIDATE_PRIORITY = 3;
    public static final int CANDIDATE_ADDRESS = 4;
    public static final int CANDIDATE_PORT = 5;
    public static final int CANDIDATE_TYPE = 7;
    public static final int CANDIDATE_GENERATION_WHEN_HOST = 9;
    public static final int CANDIDATE_RELAYED_ADDRESS = 9;
    public static final int CANDIDATE_RELAYED_PORT = 11;
    public static final int CANDIDATE_GENERATION_WHEN_REMOTE = 13;

    /**
     * Creates a brand new {@link SessionDescription} object.
     *
     * @param sid The session ID to use.
     * @return A new {@link SessionDescription} object.
     * @throws SDPException                  When a new {@link Origin} can't be created.
     * @throws java.net.UnknownHostException When local host can't be resolved to an address.
     */
    private static SessionDescription getNewSessionDescription(final String sid)
            throws SDPException, UnknownHostException {
        Version version = Version.parse("v=0");
        long ntpTime = Time.getNTP(new Date());
        /**
         * Passing in a fake name, all other params are the same as the {@link Origin#Origin}. Name is not that
         * important as it doesn't exist in the Jingle IQ.
         */
        Origin origin = new Origin("-", ntpTime, ntpTime, LOCALHOST);

        origin.setSessionID(Long.parseLong(sid));
        SessionName sessionName = new SessionName();
        TimeDescription timeDescription = new TimeDescription();

        return new SessionDescription(version, origin, sessionName, timeDescription);
    }

    /**
     * Creates a new {@link net.java.sip.communicator.impl.protocol.jabber.extensions.jingle
     * .IceUdpTransportPacketExtension} based on the passed in {@link Attribute}[].
     *
     * @param candidateAttrs {@link Attribute}[] - List of candidates.
     * @param userFragment   {@link Attribute} SDP attribute holding the ICE user fragment.
     * @param password       {@link Attribute} SDP attribute holding the ICE password.
     * @return A new {@link net.java.sip.communicator.impl.protocol.jabber.extensions.jingle
     * .IceUdpTransportPacketExtension}.
     */
    private static IceUdpTransportPacketExtension getIceUdpTransportPacketExtension(final Attribute[] candidateAttrs,
                                                                                    final Attribute userFragment,
                                                                                    final Attribute password) {

        CandidatePacketExtension candidateExtension = null;
        IceUdpTransportPacketExtension iceUdpExtension = new IceUdpTransportPacketExtension();
        if (userFragment != null) {
            iceUdpExtension.setUfrag(userFragment.getValue());
        }
        if (password != null) {
            iceUdpExtension.setPassword(password.getValue());
        }

        for (Attribute attr : candidateAttrs) {
            String[] params = attr.getValue().split("[ ]");
            if (params[2].equalsIgnoreCase("udp")) {
                candidateExtension = new CandidatePacketExtension();
                candidateExtension.setFoundation(params[0]);
                candidateExtension.setComponent(Integer.parseInt(params[1]));
                candidateExtension.setProtocol(params[2].toLowerCase());
                candidateExtension.setPriority(Long.parseLong(params[3]));
                candidateExtension.setIP(params[4]);
                candidateExtension.setPort(Integer.parseInt(params[5]));

                CandidateType type = CandidateType.valueOf(params[7]);
                candidateExtension.setType(type);
                if (type == CandidateType.host) {
                    if (params.length < 10) {
                        candidateExtension.setGeneration(0);
                    } else {
                        candidateExtension.setGeneration(Integer.parseInt(params[9]));
                    }
                } else {
                    candidateExtension.setRelAddr(params[9]);
                    candidateExtension.setRelPort(Integer.parseInt(params[11]));
                    if (params.length < 14) {
                        candidateExtension.setGeneration(0);
                    } else {
                        candidateExtension.setGeneration(Integer.parseInt(params[13]));
                    }
                }

                iceUdpExtension.addCandidate(candidateExtension);
            }
        }

        return iceUdpExtension;
    }

    public static SessionDescription sdpFromGTalkTransportInfo(final JingleIQ jingleIQ) {
        SessionDescription sdp;
        try {
            sdp = getNewSessionDescription(jingleIQ.getSID());
            for (ContentPacketExtension cpe : jingleIQ.getContentList()) {
                final String sdpMid = cpe.getName();
                final Media media =
                        SDPFactory.createMedia(sdpMid, 1 /* ignored */, "RTP/SAVPF" /* ignored */, "1" /* ignored */);
                final MediaDescription mediaDescription = new MediaDescription(media);
                mediaDescription.setConnection(new Connection(LOCALHOST));

                for (Attribute attr : getIceAttributes(cpe)) {
                    mediaDescription.addAttribute(attr);
                }

                for (GTalkTransportPacketExtension transport : cpe
                        .getChildExtensionsOfType(GTalkTransportPacketExtension.class)) {
                    for (GTalkCandidatePacketExtension candidate : transport
                            .getChildExtensionsOfType(GTalkCandidatePacketExtension.class)) {
                        mediaDescription.addAttribute(
                                new Attribute("candidate", gTtalkIceCandidateLineFromJingle0(candidate, true)));
                    }
                }
                sdp.addMediaDescription(mediaDescription);
            }
        } catch (SDPException | UnknownHostException e) {
            e.printStackTrace();
            sdp = null;
        }
        return sdp;
    }

    private static List<Attribute> getIceAttributes(final ContentPacketExtension cpe) throws SDPException {
        List<Attribute> iceAttr = new ArrayList<>();
        for (GTalkTransportPacketExtension transport : cpe
                .getChildExtensionsOfType(GTalkTransportPacketExtension.class)) {
            for (GTalkCandidatePacketExtension candidate : transport
                    .getChildExtensionsOfType(GTalkCandidatePacketExtension.class)) {
                if (candidate.getPassword() != null && !candidate.getPassword().isEmpty()
                    && candidate.getUsername() != null && !candidate.getUsername().isEmpty()) {
                    iceAttr.add(new Attribute("ice-ufrag", candidate.getUsername()));
                    iceAttr.add(new Attribute("ice-pwd", candidate.getPassword()));
                    break;
                }
            }
        }
        return iceAttr;
    }

    public static GTalkTransportPacketExtension getGTalkTransportPacketExtension(final Media media,
                                                                                 final Attribute[] candidateAttrs,
                                                                                 final Attribute userFragment,
                                                                                 final Attribute password) {
        final boolean isAudio = "audio".equals(media.getMediaType());
        final boolean isVideo = "video".equals(media.getMediaType());

        final GTalkTransportPacketExtension transport = new GTalkTransportPacketExtension();
        for (final Attribute candidateAttr : candidateAttrs) {
            String[] params = candidateAttr.getValue().split("[ ]");
            final String protocol = params[CANDIDATE_PROTOCOL];
            if ("udp".equalsIgnoreCase(protocol)) {
                final GTalkCandidatePacketExtension candidate = new GTalkCandidatePacketExtension();
                candidate.setNetwork(0);
                candidate.setUsername(userFragment.getValue());
                candidate.setPassword(password.getValue());
                candidate.setName(getGTalkCandidateNameAttr(isAudio, isVideo, params[CANDIDATE_COMPONENT]));
                candidate.setProtocol(protocol.toLowerCase());
                CandidateType type = CandidateType.valueOf(params[CANDIDATE_TYPE]);
                if (type == CandidateType.srflx) {
                    type = CandidateType.stun;
                } else if (type == CandidateType.host) {
                    type = CandidateType.local;
                }
                candidate.setType(type);
                if (type == CandidateType.local) {
                    candidate.setPreference(0.99D);
                    candidate.setAddress(params[CANDIDATE_ADDRESS]);
                    candidate.setPort(Integer.parseInt(params[CANDIDATE_PORT]));
                    if (params.length < 10) {
                        candidate.setGeneration(0);
                    } else {
                        candidate.setGeneration(Integer.parseInt(params[CANDIDATE_GENERATION_WHEN_HOST]));
                    }
                } else if (type == CandidateType.stun) {
                    candidate.setPreference(0.86D);
                    candidate.setAddress(params[CANDIDATE_ADDRESS]);
                    candidate.setPort(Integer.parseInt(params[CANDIDATE_PORT]));
                    if (params.length < 14) {
                        candidate.setGeneration(0);
                    } else {
                        candidate.setGeneration(Integer.parseInt(params[CANDIDATE_GENERATION_WHEN_REMOTE]));
                    }
                } else {
                    candidate.setPreference(0.5D);
                    candidate.setAddress(params[CANDIDATE_ADDRESS]);
                    candidate.setPort(Integer.parseInt(params[CANDIDATE_PORT]));
                    if (params.length < 14) {
                        candidate.setGeneration(0);
                    } else {
                        candidate.setGeneration(Integer.parseInt(params[CANDIDATE_GENERATION_WHEN_REMOTE]));
                    }
                }
                transport.addCandidate(candidate);
            }
        }
        return transport;
    }

    private static String getGTalkCandidateNameAttr(final boolean isAudio, final boolean isVideo,
                                                    final String component) {
        String name = "";
        if (isAudio) {
            if ("1".equals(component)) {
                name = GTalkCandidatePacketExtension.AUDIO_RTP_NAME;
            } else if ("2".equals(component)) {
                name = GTalkCandidatePacketExtension.AUDIO_RTCP_NAME;
            }
        } else if (isVideo) {
            if ("1".equals(component)) {
                name = GTalkCandidatePacketExtension.VIDEO_RTP_NAME;
            } else if ("2".equals(component)) {
                name = GTalkCandidatePacketExtension.VIDEO_RTCP_NAME;
            }
        }
        return name;
    }

    /**
     * Creates a Jingle "transport-info" IQ based on the passed in {@link SessionDescription}.
     *
     * @param sessionDescription {@link SessionDescription} - The {@link SessionDescription} to convert.
     * @param mediaName          String - The "name" to use in the "content" tag.
     * @return {@link net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ} - A new {@link
     * net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ} containing all the transport
     * candidates.
     */
    private static JingleIQ createJingleTransportInfo(final SessionDescription sessionDescription,
                                                      final String mediaName) {

        JingleIQ result = new JingleIQ();
        result.setType(IQ.Type.SET);
        Origin origin = sessionDescription.getOrigin();
        result.setSID(Long.toString(origin.getSessionID()));

        ContentPacketExtension content = new ContentPacketExtension();
        content.setName(mediaName);

        try {
            result.setAction(JingleAction.parseString(JingleAction.TRANSPORT_INFO.toString()));
            Attribute[] candidateAttributes = sessionDescription.getAttributes("candidate");
            Attribute userFragment = sessionDescription.getAttribute("ice-ufrag");
            Attribute password = sessionDescription.getAttribute("ice-pwd");
            // TODO: What about TCP?
            IceUdpTransportPacketExtension iceUdpExtension = getIceUdpTransportPacketExtension(candidateAttributes,
                    userFragment, password);

            content.addChildExtension(iceUdpExtension);
            result.addContent(content);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            result = null;
        }

        return result;
    }

    public SessionDescription sdpFromJingle(JingleIQ jingle) {
        return sdpFromJingle0(jingle);
    }

    /**
     * Creates an SDP object from a Jingle Stanza.
     *
     * @param jingle JingleIQ - The Jingle stanza to convert.
     * @return SessionDescription - Converted SDP object.
     */
    // Things that remain to be done:
    //  * Generate the "crypto" line from the <encription><crypto> element.
    //  * Generate the "ssrc" lines from the <streams> element.
    static SessionDescription sdpFromJingle0(JingleIQ jingle) {
        final boolean isGTalk = isGTalk(jingle);
        SessionDescription sdp;
        if (isGTalk) {
            sdp = sdpFromGTalkTransportInfo(jingle);
        } else {
            sdp = sdpFromStandardJingle(jingle);
        }
        return sdp;
    }

    private static boolean isGTalk(final JingleIQ jingle) {
        boolean isGTalk = false;
        if (JingleAction.TRANSPORT_INFO.equals(jingle.getAction())) {
            for (ContentPacketExtension cpe : jingle.getContentList()) {
                final List<GTalkTransportPacketExtension> gTalkTransportPacketExtensions =
                        cpe.getChildExtensionsOfType(GTalkTransportPacketExtension.class);
                if (gTalkTransportPacketExtensions != null && gTalkTransportPacketExtensions.size() > 0) {
                    isGTalk = true;
                }
            }
        }
        return isGTalk;
    }

    private static SessionDescription sdpFromStandardJingle(final JingleIQ jingle) {
        try {
            SessionDescription result = getNewSessionDescription(jingle.getSID());
            List<ContentPacketExtension> contents = jingle.getContentList();

            // "a=group:BUNDLE audio video"
            StringBuilder valueBuilder = new StringBuilder("BUNDLE");
            for (ContentPacketExtension content : contents) {
                valueBuilder.append(" ").append(content.getName());
            }
            Attribute attr = new Attribute("group", valueBuilder.toString());
//			result.addAttribute(attr);

            for (ContentPacketExtension content : contents) {

                // "m=audio 36798 RTP/AVPF 103 104 110 107 9 102 108 0 8 106 105 13 127 126\r\n"
                String contentType = content.getName();
                Media media;
                List<RtpDescriptionPacketExtension> descriptionExts =
                        content.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);
                RtpDescriptionPacketExtension descriptionExt = descriptionExts.get(0);
                List<PayloadTypePacketExtension> payloadExts =
                        descriptionExt.getChildExtensionsOfType(PayloadTypePacketExtension.class);
                if (payloadExts.size() > 0) {
                    final String profile =
                            descriptionExt.getProfile() == null || descriptionExt.getProfile().isEmpty() ? "RTP/SAVPF" :
                                    descriptionExt.getProfile();
                    final int id = payloadExts.get(0).getID();
                    media = new Media(contentType, 123456789, profile,
                            Integer.toString(id == 103 ? 97 : id));
                    for (int i = 1, n = payloadExts.size(); i < n; ++i) {
                        media.addMediaFormat(Integer.toString(payloadExts.get(i).getID()));
                    }
                } else {
                    throw new RuntimeException("No media format");
                }
                MediaDescription mediaDescription = new MediaDescription(media);

//				List<RawUdpTransportPacketExtension> rawUdpExts = content.getChildExtensionsOfType
// (RawUdpTransportPacketExtension.class);
//				RawUdpTransportPacketExtension firstRawUdpExt = rawUdpExts.get(0);
//				CandidatePacketExtension candidateExt = firstRawUdpExt.getChildExtensionsOfType
// (CandidatePacketExtension.class).get(0);

                // "c=IN IP4 172.22.76.221"
                Connection connection = new Connection(LOCALHOST);
                mediaDescription.setConnection(connection);

                // "a=rtcp:36798 IN IP4 172.22.76.221\n"
                Attribute rawUdpAttr = new Attribute("rtcp", 36798 + " IN IP4 " + LOCALHOST);
                mediaDescription.addAttribute(rawUdpAttr);

                List<IceUdpTransportPacketExtension> iceUdpExts =
                        content.getChildExtensionsOfType(IceUdpTransportPacketExtension.class);
                iceUdpExts = Utils.filterByClass(iceUdpExts, IceUdpTransportPacketExtension.class);
                for (IceUdpTransportPacketExtension iceUdpExt : iceUdpExts) {
                    // ICE user fragment
                    Attribute userFragment = new Attribute("ice-ufrag", iceUdpExt.getUfrag());
                    mediaDescription.addAttribute(userFragment);

                    // ICE password
                    Attribute password = new Attribute("ice-pwd", iceUdpExt.getPassword());
                    mediaDescription.addAttribute(password);
                    // There can (or better should) be multiple candidate tags inside one transport tag.
                    for (CandidatePacketExtension candidateExtension : iceUdpExt
                            .getChildExtensionsOfType(CandidatePacketExtension.class)) {
                        // "a=candidate:1 2 udp 1 172.22.76.221 47216 typ host generation 0"
                        String value = iceCandidateLineFromJingle0(candidateExtension, true);
                        Attribute iceUdpAttr = new Attribute("candidate", value);
                        mediaDescription.addAttribute(iceUdpAttr);
                    }
                }

                // "a=sendrecv"
                Attribute sendrecvAttr = new Attribute("sendrecv");
                mediaDescription.addAttribute(sendrecvAttr);

                // "a=mid:audio"
                Attribute midAttr = new Attribute("mid", contentType);
                mediaDescription.addAttribute(midAttr);

                // "a=rtpmap:106 CN/32000"
                payloadExts = descriptionExt.getChildExtensionsOfType(PayloadTypePacketExtension.class);
                for (PayloadTypePacketExtension payloadExt : payloadExts) {
                    String value = (payloadExt.getID() == 103 ? 97 : payloadExt.getID())
                                   + " " + payloadExt.getName().toUpperCase()
                                   + "/" + getClockrate(payloadExt);
                    Attribute rtpmapAttr = new Attribute("rtpmap", value);
                    mediaDescription.addAttribute(rtpmapAttr);
                }

                // "a=crypto:0 AES_CM_128_HMAC_SHA1_32 inline:keNcG3HezSNID7LmfDa9J4lfdUL8W1F7TNJKcbuy"
                List<EncryptionPacketExtension> encryptionExts =
                        descriptionExt.getChildExtensionsOfType(EncryptionPacketExtension.class);
                if (encryptionExts.size() > 0) {
                    EncryptionPacketExtension encryptionExt = encryptionExts.get(0);
                    List<CryptoPacketExtension> cryptoExts = encryptionExt.getCryptoList();
                    for (CryptoPacketExtension cryptoExt : cryptoExts) {
                        String value = cryptoExt.getTag()
                                       + " " + cryptoExt.getCryptoSuite()
                                       + " " + cryptoExt.getKeyParams();
                        Attribute cryptoAttr = new Attribute("crypto", value);
                        mediaDescription.addAttribute(cryptoAttr);
                    }
                }

                // "a=rtcp-mux"
                List<RtcpMuxExtension> rtcpMuxExts = descriptionExt.getChildExtensionsOfType(RtcpMuxExtension.class);
                if (!rtcpMuxExts.isEmpty()) {
                    Attribute muxAttr = new Attribute("rtcp-mux");
                    mediaDescription.addAttribute(muxAttr);
                }

                // "a=ssrc:2570980487 cname:hsWuSQJxx7przmb8"
                // "a=ssrc:2570980487 mslabel:stream_label"
                // "a=ssrc:2570980487 label:audio_label"
                List<StreamsPacketExtension> streamsExts =
                        descriptionExt.getChildExtensionsOfType(StreamsPacketExtension.class);
                if (!streamsExts.isEmpty()) {
                    List<StreamPacketExtension> streamExts =
                            streamsExts.get(0).getChildExtensionsOfType(StreamPacketExtension.class);
                    if (!streamExts.isEmpty()) {
                        StreamPacketExtension streamExt = streamExts.get(0);
                        List<String> attrNames = streamExt.getAttributeNames();
                        SsrcPacketExtension ssrcExt = streamExt.getSsrc();
                        for (String attrName : attrNames) {
                            String value =
                                    ssrcExt.getText() + " " + attrName + ":" + streamExt.getAttributeAsString(attrName);
                            attr = new Attribute("ssrc", value);
                            mediaDescription.addAttribute(attr);
                        }
                    }
                }

                result.addMediaDescription(mediaDescription);
            }
            return result;
        } catch (SDPException e) {
            e.printStackTrace();
            return null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getClockrate(final PayloadTypePacketExtension payloadExt) {
        return payloadExt.getClockrate() == null || payloadExt.getClockrate().isEmpty() ? "90000" :
                payloadExt.getClockrate();
    }

    public JingleTuple jingleFromSdp(final SessionDescription sdp, final boolean useGTalkTransport)
            throws Exception {
        return jingleFromSdp0(sdp, useGTalkTransport);
    }

    static JingleTuple jingleFromSdp0(final SessionDescription sdp) throws Exception {
        return jingleFromSdp0(sdp, false);
    }

    /**
     * Creates a Jingle stanza from SDP.
     *
     * @param sdp SessionDescription - The SDP object to convert to Jingle.
     * @return JingleIQ - Converted Jingle stanza.
     */
    // Things that remain to be done:
    //  * Generate the <encription><crypto> element from the "crypto" line of SDP.
    //  * Generate the <streams><stream> elements from the "ssrc" lines of SDP.
    static JingleTuple jingleFromSdp0(final SessionDescription sdp, final boolean useGTalkTransport)
            throws Exception {
        JingleIQ jingleIQ = new JingleIQ();
        jingleIQ.setType(IQ.Type.SET);

        Origin origin = sdp.getOrigin();
        jingleIQ.setSID(Long.toString(origin.getSessionID()));

        MediaDescription[] mediaDescriptions = sdp.getMediaDescriptions();
        for (MediaDescription mediaDescription : mediaDescriptions) {
            ContentPacketExtension content = new ContentPacketExtension();

            Connection connection = mediaDescription.getConnection();
            String netType = connection.getNetType();
            if (netType.equals("IN")) {
                content.setCreator(ContentPacketExtension.CreatorEnum.initiator);
            } else {
                throw new RuntimeException("Unsupported mediaDescription connection type '" + netType + "'");
            }

            Media media = mediaDescription.getMedia();
            content.setName(media.getMediaType());

            RtpDescriptionPacketExtension rtpExt = new RtpDescriptionPacketExtension();
            rtpExt.setMedia(media.getMediaType());
            rtpExt.setProfile(media.getProtocol());

            content.addChildExtension(rtpExt);

            Attribute[] rtpmapAttrs = mediaDescription.getAttributes("rtpmap");
            for (Attribute attr : rtpmapAttrs) {
                PayloadTypePacketExtension payloadExt = new PayloadTypePacketExtension();
                String[] params = attr.getValue().split("[ /]");
                payloadExt.setId(Integer.parseInt(params[0]));
                payloadExt.setName(params[1].toUpperCase());
                StringBuilder clockRate = new StringBuilder(params[2]);
                if (params.length > 3) {
                    clockRate.append("/").append(params[3]);
                }
                payloadExt.setClockrate(clockRate.toString());
                rtpExt.addChildExtension(payloadExt);
            }

            // <encryption><crypto /><crypto /></encryption>
            Attribute[] cryptoAttrs = mediaDescription.getAttributes("crypto");
            EncryptionPacketExtension encryptionExt = null;
            if (cryptoAttrs != null && cryptoAttrs.length > 0) {
                encryptionExt = new EncryptionPacketExtension();
//                encryptionExt.setRequired(true);
                rtpExt.addChildExtension(encryptionExt);
            }
            for (Attribute attr : cryptoAttrs) {
                String[] params = attr.getValue().split(" ");
                CryptoPacketExtension cryptoExt = new CryptoPacketExtension();
                cryptoExt.setTag(params[0]);
                cryptoExt.setCryptoSuite(params[1]);
                cryptoExt.setKeyParams(params[2]);
                encryptionExt.addChildExtension(cryptoExt);
            }

            // <rtcp-mux />
            Attribute[] rtcpMuxAttrs = mediaDescription.getAttributes("rtcp-mux");
            if (rtcpMuxAttrs.length > 0) {
                RtcpMuxExtension rtpcMuxExt = new RtcpMuxExtension();
                rtpExt.addChildExtension(rtpcMuxExt);
            }

            // <streams><stream><ssrc>
            Attribute[] ssrcAttrs = mediaDescription.getAttributes("ssrc");
            StreamsPacketExtension streamsExt = null;
            if (cryptoAttrs != null && cryptoAttrs.length > 0) {
                streamsExt = new StreamsPacketExtension();
                rtpExt.addChildExtension(streamsExt);
            }
            Map<String, Map<String, String>> streams = new HashMap<String, Map<String, String>>();
            for (Attribute attr : ssrcAttrs) {
                String[] params = attr.getValue().split(" ");
                Map<String, String> map = streams.get(params[0]);
                if (map == null) {
                    map = new HashMap<String, String>();
                    streams.put(params[0], map);
                }
                String[] kv = params[1].split(":");
                map.put(kv[0], kv[1]);
            }
            for (Map.Entry<String, Map<String, String>> stream : streams.entrySet()) {
                StreamPacketExtension streamExt = new StreamPacketExtension();
                Map<String, String> streamAttrs = stream.getValue();
                for (Map.Entry<String, String> streamAttr : streamAttrs.entrySet()) {
                    streamExt.setAttribute(streamAttr.getKey(), streamAttr.getValue());
                }
                SsrcPacketExtension ssrcExt = new SsrcPacketExtension();
                ssrcExt.setText(stream.getKey());
                streamExt.addChildExtension(ssrcExt);
                streamsExt.addChildExtension(streamExt);
            }

            if (!useGTalkTransport) {
                RawUdpTransportPacketExtension rawUdpExt = new RawUdpTransportPacketExtension();
                CandidatePacketExtension candidateExt = new CandidatePacketExtension();
                candidateExt.setIP(connection.getAddress());
                candidateExt.setPort(media.getPort());
                candidateExt.setGeneration(0);
                rawUdpExt.addChildExtension(candidateExt);
                content.addChildExtension(rawUdpExt);
            }

            // TODO: What about TCP?
            Attribute[] candidateAttributes = mediaDescription.getAttributes("candidate");
            Attribute userFragment = mediaDescription.getAttribute("ice-ufrag");
            Attribute password = mediaDescription.getAttribute("ice-pwd");
            // TODO: DTLS-SRTP (fingerprint).
            IceUdpTransportPacketExtension iceUdpExt = useGTalkTransport ?
                    getGTalkTransportPacketExtension(media, candidateAttributes, userFragment, password) :
                    getIceUdpTransportPacketExtension(candidateAttributes, userFragment, password);
            content.addChildExtension(iceUdpExt);

            jingleIQ.addContent(content);
        }

        // if useGTalkTransport split the Jingle message in one with description and an other with transport candidates

        //check if there are any candidates
        return split(useGTalkTransport, jingleIQ);
    }

    private static JingleTuple split(final boolean useGTalkTransport, final JingleIQ jingleIQ) throws Exception {
        JingleIQ descIQ = clone(jingleIQ);
        Element descAdditionalInfo = null;

        List<JingleWithAdditionalInfo> transports = new ArrayList<>();


        if (shouldSplitMessage(useGTalkTransport, jingleIQ)) {
            JingleIQ transportIQ = clone(jingleIQ);
            for (ContentPacketExtension cpe : descIQ.getContentList()) {
                for (PacketExtension packetExtension : cpe.getChildExtensions()) {
                    if (packetExtension instanceof GTalkTransportPacketExtension) {
                        removeAll(((GTalkTransportPacketExtension) packetExtension).getChildExtensions().iterator());
                    }
                }
            }
            for (ContentPacketExtension cpe : transportIQ.getContentList()) {
                final Iterator<? extends PacketExtension> it = cpe.getChildExtensions().iterator();
                while (it.hasNext()) {
                    if (it.next() instanceof RtpDescriptionPacketExtension) {
                        it.remove();
                    }
                }
            }
            transportIQ.setAction(JingleAction.TRANSPORT_INFO);
            transports = splitTranpsort(transportIQ);
        }
        if (useGTalkTransport) {
            descAdditionalInfo = DocumentFactory.getInstance().createElement("additional-info", "techwin:jingle");
            descAdditionalInfo.addAttribute("nat-type", "full_cone");
            descAdditionalInfo.addAttribute("profile-id", "6");
            descAdditionalInfo.addElement("rtsp", "").addAttribute("uri", "");
        }
        return new JingleTuple(new JingleWithAdditionalInfo(descIQ, descAdditionalInfo), transports);
    }

    private static List<JingleWithAdditionalInfo> splitTranpsort(JingleIQ baseTransportIQ) throws Exception {
        List<JingleWithAdditionalInfo> transports = new ArrayList<>();
        for (ContentPacketExtension cpe : baseTransportIQ.getContentList()) {
            JingleIQ transportIQ = clone(baseTransportIQ);
            transportIQ.getContentList().clear();
            transportIQ.addContent(cpe);
            transports.add(new JingleWithAdditionalInfo(transportIQ, null));
        }
        return transports;
    }

    private static void removeAll(final Iterator<? extends PacketExtension> it) {
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private static JingleIQ clone(JingleIQ jingleIQ) throws Exception {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        new ObjectOutputStream(byteArrayOutputStream).writeObject(jingleIQ);
        final JingleIQ cloned =
                (JingleIQ) new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                        .readObject();
        cloned.setType(jingleIQ.getType());
        cloned.setSID(jingleIQ.getSID());
        return cloned;
    }

    private static boolean shouldSplitMessage(final boolean useGTalkTransport, final JingleIQ result) {
        boolean shouldSplit = false;
        if (useGTalkTransport) {
            for (ContentPacketExtension cpe : result.getContentList()) {
                for (GTalkTransportPacketExtension transport : cpe
                        .getChildExtensionsOfType(GTalkTransportPacketExtension.class)) {
                    final List<GTalkCandidatePacketExtension> candidates =
                            transport.getChildExtensionsOfType(GTalkCandidatePacketExtension.class);
                    if (!candidates.isEmpty()) {
                        shouldSplit = true;
                    }
                }
            }
        }
        return shouldSplit;
    }

    public JingleIQ transportInfoFromSdpStub(final List<String> candidateList, final String sid,
                                             final String mediaName) {
        return transportInfoFromSdpStub0(candidateList, sid, mediaName);
    }

    /**
     * Creates a Jingle "transport-info" IQ based on a passed in SDP stub of ICE candidates.
     *
     * @param candidateList List<String> - List of SDP ICE candidates.
     * @param sid           String - The Jingle session ID.
     * @param mediaName     String - The "name" to use in the "content" tag.
     * @return {@link net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ} - A new {@link net
     * .java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ} containing all the transport candidates.
     */
    static JingleIQ transportInfoFromSdpStub0(final List<String> candidateList, final String sid,
                                              final String mediaName) {

        SessionDescription sessionDescription = null;
        JingleIQ result = null;
        String candidatePrefix = "candidate:";

        // First, create a session description object.
        try {
            sessionDescription = getNewSessionDescription(sid);

            // Add candidates.
            for (String candidate : candidateList) {
                String[] keyValue = candidate.split(candidatePrefix);
                Attribute field = new Attribute("candidate", keyValue[1].trim());
                sessionDescription.addAttribute(field);
            }
        } catch (SDPException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // Convert SDP to Jingle.
        if (sessionDescription != null) {
            result = createJingleTransportInfo(sessionDescription, mediaName);
        }

        return result;
    }

    public String iceCandidateLineFromJingle(final CandidatePacketExtension iceCandidate) {
        return iceCandidateLineFromJingle0(iceCandidate);
    }

    static String iceCandidateLineFromJingle0(final CandidatePacketExtension iceCandidate) {
        return iceCandidateLineFromJingle0(iceCandidate, false);
    }

    public String iceCandidateLineFromJingle(final CandidatePacketExtension iceCandidate, final boolean valueOnly) {
        return iceCandidateLineFromJingle0(iceCandidate, valueOnly);
    }

    /**
     * Constructs an SDP ICE candidate line based on a Jingle {@link net.java.sip.communicator.impl.protocol.jabber
     * .extensions.jingle.CandidatePacketExtension} object.
     *
     * @param iceCandidate {@link net.java.sip.communicator.impl.protocol.jabber.extensions.jingle
     *                     .CandidatePacketExtension} - The Jingle ICE candidate object to use.
     * @param valueOnly    boolean - Whether or not only the value part of the SDP will need to be generated.
     *                     If so, the prefix "a=candidate:" and the suffix "\r\n" will be omitted.
     * @return String - ICE candidate SDP line.
     * @see "http://tools.ietf.org/html/rfc5245#page-73"}
     */
    static String iceCandidateLineFromJingle0(final CandidatePacketExtension iceCandidate, final boolean valueOnly) {
        // "(a=candidate:)1 2 udp 2 172.22.76.221 36798 typ srflx raddr 10.0.34.44 rport 48296 generation 0(\r\n)"
        StringBuilder builder = new StringBuilder();
        if (!valueOnly) {
            builder.append("a=candidate:");
        }
        builder.append(iceCandidate.getFoundation() + " ");
        builder.append(iceCandidate.getComponent() + " ");
        builder.append(iceCandidate.getProtocol() + " ");
        builder.append(iceCandidate.getPriority() + " ");
        builder.append(iceCandidate.getIP() + " ");
        builder.append(iceCandidate.getPort() + " typ ");
        builder.append(iceCandidate.getType() + " ");
        if (iceCandidate.getType() != CandidateType.host) {
            builder.append("raddr " + iceCandidate.getRelAddr() + " ");
            builder.append("rport " + iceCandidate.getRelPort() + " ");
        }
        builder.append("generation " + iceCandidate.getGeneration());
        if (!valueOnly) {
            builder.append("\r\n");
        }

        return builder.toString();
    }

    static String gTtalkIceCandidateLineFromJingle0(final GTalkCandidatePacketExtension iceCandidate,
                                                    final boolean valueOnly) {
        final CandidateType type;

        if (CandidateType.local.equals(iceCandidate.getType())) {
            type = CandidateType.host;
        } else if (CandidateType.stun.equals(iceCandidate.getType())) {
            type = CandidateType.srflx;
        } else {
            type = iceCandidate.getType();
        }

        int foundation;
        if (type == CandidateType.host) {
            foundation = 1;
        } else {
            foundation = 2;
        }

        // "(a=candidate:)1 2 udp 2 172.22.76.221 36798 typ srflx raddr 10.0.34.44 rport 48296 generation 0(\r\n)"
        StringBuilder builder = new StringBuilder();
        if (!valueOnly) {
            builder.append("a=candidate:");
        }
        builder.append(foundation + " ");
        builder.append((iceCandidate.getName().contains("rtp") ? "1" : "2") + " ");
        builder.append(iceCandidate.getProtocol() + " ");
        //builder.append(preferenceToPriority(iceCandidate.getPreference()) + " ");
        if (type == CandidateType.host) {
            builder.append(1000 + " ");
        } else {
            builder.append(800 + " ");
        }
        builder.append(iceCandidate.getAddress() + " ");
        builder.append(iceCandidate.getPort() + " typ ");


        builder.append(type + " ");
        if (type != CandidateType.host) {
            builder.append("raddr " + iceCandidate.getAddress() + " ");
            builder.append("rport " + iceCandidate.getPort() + " ");
        }
        builder.append("generation " + iceCandidate.getGeneration());
        if (!valueOnly) {
            builder.append("\r\n");
        }

        return builder.toString();
    }

    private static int preferenceToPriority(final double preference) {
//        return Double.valueOf(preference * 1000000000).intValue();
        return 1000;
    }

    private static double priorityToPreference(final String priority) {
//        return Math.pow(10, priority.length() - 1) / Double.parseDouble(priority);
        return 0.99D;
    }
}
