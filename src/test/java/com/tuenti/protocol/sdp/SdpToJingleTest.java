package com.tuenti.protocol.sdp;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.GTalkTransportPacketExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.sourceforge.jsdp.*;
import org.jivesoftware.smack.packet.IQ;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xmlmatchers.XmlMatchers;
import org.xmlmatchers.transform.XmlConverters;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for the SdpToJingle class.
 * <p/>
 * Copyright (c) Tuenti Technologies. All rights reserved.
 *
 * @author Wijnand Warren <wwarren@tuenti.com>
 * @author Manuel Peinado Gallego <mpeinado@tuenti.com>
 */
@RunWith(JUnit4.class)
@Ignore
public class SdpToJingleTest {
    private static final String SAMPLE_SDP_MESSAGE = "v=0\r\n"
                                                     + "o=- 123 1 IN IP4 127.0.0.1\r\n"
                                                     + "s=session\r\n"
                                                     + "t=0 0\r\n"
                                                     + "a=group:BUNDLE audio video\r\n"
                                                     + "m=audio 36798 RTP/AVPF 103 104 110 107 9 102 108 0 8 106 105 "
                                                     + "13 127 126\r\n"
                                                     + "c=IN IP4 172.22.76.221\r\n"
                                                     + "a=rtcp:36798 IN IP4 172.22.76.221\r\n"
                                                     + "a=ice-ufrag:YuWMyUbmK/CX6awo\r\n"
                                                     + "a=ice-pwd:DpueNNn6/r6TTRFMqNWw0v/c\r\n"
                                                     + "a=candidate:1 2 udp 1 172.22.76.221 47216 typ host generation"
                                                     + " 0\r\n"
                                                     + "a=candidate:1 1 udp 1 172.22.76.221 48235 typ host generation"
                                                     + " 0\r\n"
                                                     + "a=candidate:1 2 udp 2 172.22.76.221 36798 typ srflx raddr 10"
                                                     + ".0.34.44 rport 48296 generation 0\r\n"
                                                     + "a=candidate:1 1 udp 2 172.22.76.221 50102 typ relay raddr 213"
                                                     + ".99.45.11 rport 4313 generation 0\r\n"
                                                     + "a=sendrecv\r\n"
                                                     + "a=mid:audio\r\n"
                                                     + "{{RTCP-MUX}}" // placeholder for a=rtcp-mux\r\n
                                                     + "a=crypto:0 AES_CM_128_HMAC_SHA1_32 "
                                                     + "inline:keNcG3HezSNID7LmfDa9J4lfdUL8W1F7TNJKcbuy \r\n"
                                                     + "a=rtpmap:111 opus/48000/2\r\n"
                                                     + "a=rtpmap:103 ISAC/16000\r\n"
                                                     + "a=rtpmap:104 ISAC/32000\r\n"
                                                     + "a=rtpmap:110 CELT/32000\r\n"
                                                     + "a=rtpmap:107 speex/16000\r\n"
                                                     + "a=rtpmap:9 G722/16000\r\n"
                                                     + "a=rtpmap:102 ILBC/8000\r\n"
                                                     + "a=rtpmap:108 speex/8000\r\n"
                                                     + "a=rtpmap:0 PCMU/8000\r\n"
                                                     + "a=rtpmap:8 PCMA/8000\r\n"
                                                     + "a=rtpmap:106 CN/32000\r\n"
                                                     + "a=rtpmap:105 CN/16000\r\n"
                                                     + "a=rtpmap:13 CN/8000\r\n"
                                                     + "a=rtpmap:127 red/8000\r\n"
                                                     + "a=rtpmap:126 telephone-event/8000\r\n"
                                                     + "a=ssrc:2570980487 cname:hsWuSQJxx7przmb8\r\n"
                                                     + "a=ssrc:2570980487 mslabel:stream_label\r\n"
                                                     + "a=ssrc:2570980487 label:audio_label\r\n"
                                                     + "m=video 39456 RTP/AVPF 100 101 102\r\n"
                                                     + "c=IN IP4 172.22.76.221\r\n"
                                                     + "a=rtcp:39456 IN IP4 172.22.76.221\r\n"
                                                     + "a=candidate:1 2 udp 1 172.22.76.221 40550 typ host generation"
                                                     + " 0\r\n"
                                                     + "a=candidate:1 1 udp 1 172.22.76.221 53441 typ host generation"
                                                     + " 0\r\n"
                                                     + "a=candidate:1 2 udp 2 172.22.76.221 46128 typ srflx raddr 10"
                                                     + ".0.34.43 rport 48295 generation 0\r\n"
                                                     + "a=candidate:1 1 udp 2 172.22.76.221 39456 typ relay raddr 213"
                                                     + ".99.45.10 rport 4312 generation 0\r\n"
                                                     + "a=sendrecv\r\n"
                                                     + "a=mid:video\r\n"
                                                     + "{{RTCP-MUX}}" // placeholder for a=rtcp-mux\r\n
                                                     + "a=crypto:0 AES_CM_128_HMAC_SHA1_80 "
                                                     + "inline:5ydJsA+FZVpAyqJMT/nW/UW+tcOmDvXJh/pPhNRe \r\n"
                                                     + "a=rtpmap:100 VP8/90000\r\n"
                                                     + "a=rtpmap:101 red/90000\r\n"
                                                     + "a=rtpmap:102 ulpfec/90000\r\n"
                                                     + "a=ssrc:43633328 cname:hsWuSQJxx7przmb8\r\n"
                                                     + "a=ssrc:43633328 mslabel:stream_label\r\n"
                                                     + "a=ssrc:43633328 label:video_label\r\n";

    private static final String SAMPLE_ICE_CANDIDATES_SDP_STUB =
            "a=candidate:1 2 udp 1 172.22.76.221 47216 typ host generation 0\r\n"
            + "a=candidate:1 1 udp 1 172.22.76.221 48235 typ host generation 0\r\n"
            + "a=candidate:1 2 udp 2 172.22.76.221 36798 typ srflx raddr 10.0.34.44 rport 48296 generation 0\r\n"
            + "a=candidate:1 1 udp 2 172.22.76.221 50102 typ relay raddr 213.99.45.11 rport 4313 generation 0\r\n"
            + "a=candidate:1 2 udp 1 172.22.76.221 40550 typ host generation 0\r\n"
            + "a=candidate:1 1 udp 1 172.22.76.221 53441 typ host generation 0\r\n"
            + "a=candidate:1 2 udp 2 172.22.76.221 46128 typ srflx raddr 10.0.34.43 rport 48295 generation 0\r\n"
            + "a=candidate:1 1 udp 2 172.22.76.221 39456 typ relay raddr 213.99.45.10 rport 4312 generation 0\r\n";
    private static final String MEDIA_NAME = "audio";
    private static final String SAMPLE_SID = "123456";

    private SessionDescription sdp;

    /**
     * Cleans up after the test has been run.
     */
    @After
    public void tearDown() {
        sdp = null;
    }

    public void prepare() {
        prepare(true);
    }

    /**
     * Prepares the {@link SessionDescription} object to be used in the tests.
     *
     * @param includeRtcpMuxAttr boolean - Whether or not to include the RTCP MUX attribute.
     */
    public void prepare(boolean includeRtcpMuxAttr) {
        try {
            String sdpMessage = SAMPLE_SDP_MESSAGE.replace("{{RTCP-MUX}}", includeRtcpMuxAttr ? "a=rtcp-mux\r\n" : "");
            sdp = SDPFactory.parseSessionDescription(sdpMessage);
        } catch (SDPParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies a Jingle IQ.
     *
     * @param jingle {@link net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.JingleIQ} - The Jingle
     *               IQ to check.
     */
    private void verifyJingleIq(JingleIQ jingle, boolean checkCandidatePort) {
        Assert.assertNotNull(jingle);

        Assert.assertTrue(jingle.getSID().equals("123"));
        Assert.assertEquals(IQ.Type.SET, jingle.getType());

        List<ContentPacketExtension> contentList = jingle.getContentList();
        Assert.assertTrue(contentList.size() == 2);
        for (ContentPacketExtension content : contentList) {
            Assert.assertTrue(
                    contentList.get(0).getChildExtensionsOfType(RtpDescriptionPacketExtension.class).size() == 1);
            Assert.assertTrue(content.getCreator() == ContentPacketExtension.CreatorEnum.initiator);
        }

        ContentPacketExtension audio = contentList.get(0);
        Assert.assertTrue(audio.getName().equals("audio"));
        List<RtpDescriptionPacketExtension> packetExts =
                audio.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);
        Assert.assertTrue(packetExts.size() == 1);
        RtpDescriptionPacketExtension packetExt = packetExts.get(0);
        Assert.assertTrue(packetExt.getMedia().equals("audio"));

        List<PayloadTypePacketExtension> payloadExts =
                packetExt.getChildExtensionsOfType(PayloadTypePacketExtension.class);
        Assert.assertTrue(payloadExts.size() == 15);
        Assert.assertTrue(payloadExts.get(0).getID() == 111);
        Assert.assertTrue(payloadExts.get(0).getName().equals("OPUS"));
        Assert.assertTrue(payloadExts.get(0).getClockrate().equals("48000/2"));
        Assert.assertTrue(payloadExts.get(8).getID() == 0);
        Assert.assertTrue(payloadExts.get(8).getName().equals("PCMU"));
        Assert.assertTrue(payloadExts.get(8).getClockrate().equals("8000"));

//		List<RawUdpTransportPacketExtension> rawUdpExts = audio.getChildExtensionsOfType
// (RawUdpTransportPacketExtension.class);
//		Assert.assertTrue(rawUdpExts.size() == 1);
//		Assert.assertTrue(rawUdpExts.get(0).getCandidateList().size() == 1);
//		Assert.assertTrue(rawUdpExts.get(0).getCandidateList().get(0).getIP().equals("0.0.0.0"));
//		if (checkCandidatePort) {
//			Assert.assertTrue(rawUdpExts.get(0).getCandidateList().get(0).getPort() == 36798);
//		}
//		Assert.assertTrue(rawUdpExts.get(0).getCandidateList().get(0).getGeneration() == 0);

        // ICE
        List<IceUdpTransportPacketExtension> iceUdpExts =
                audio.getChildExtensionsOfType(IceUdpTransportPacketExtension.class);
        iceUdpExts = Utils.filterByClass(iceUdpExts, IceUdpTransportPacketExtension.class);
        Assert.assertEquals(1, iceUdpExts.size());
        IceUdpTransportPacketExtension icePacket;
        for (int i = 0; i < iceUdpExts.size(); ++i) {
            icePacket = iceUdpExts.get(i);
            Assert.assertEquals(4, icePacket.getCandidateList().size());
            Assert.assertEquals("YuWMyUbmK/CX6awo", icePacket.getUfrag());
            Assert.assertEquals("DpueNNn6/r6TTRFMqNWw0v/c", icePacket.getPassword());
        }
        verifyCandidateExtension(iceUdpExts.get(0).getCandidateList().get(1), 1, "1", "udp", 1, 0,
                CandidateType.host, "172.22.76.221", 48235);
        verifyCandidateExtension(iceUdpExts.get(0).getCandidateList().get(2), 2, "1", "udp", 2, 0,
                CandidateType.srflx, "172.22.76.221", 36798);

        Assert.assertTrue(contentList.get(1).getName().equals("video"));
        ContentPacketExtension video = contentList.get(1);
        Assert.assertTrue(video.getName().equals("video"));
        packetExts = video.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);
        Assert.assertTrue(packetExts.size() == 1);
        packetExt = packetExts.get(0);
        Assert.assertTrue(packetExt.getMedia().equals("video"));

        payloadExts = packetExt.getChildExtensionsOfType(PayloadTypePacketExtension.class);
        Assert.assertTrue(payloadExts.size() == 3);
        Assert.assertTrue(payloadExts.get(2).getID() == 102);
        Assert.assertTrue(payloadExts.get(2).getName().equals("ULPFEC"));
        Assert.assertTrue(payloadExts.get(2).getClockrate().equals("90000"));

//		rawUdpExts = video.getChildExtensionsOfType(RawUdpTransportPacketExtension.class);
//		Assert.assertTrue(rawUdpExts.size() == 1);
//		Assert.assertTrue(rawUdpExts.get(0).getCandidateList().size() == 1);
//		Assert.assertTrue(rawUdpExts.get(0).getCandidateList().get(0).getIP().equals("172.22.76.221"));
//		if (checkCandidatePort) {
//			Assert.assertTrue(rawUdpExts.get(0).getCandidateList().get(0).getPort() == 39456);
//		}
//		Assert.assertTrue(rawUdpExts.get(0).getCandidateList().get(0).getGeneration() == 0);

        iceUdpExts = video.getChildExtensionsOfType(IceUdpTransportPacketExtension.class);
        iceUdpExts = Utils.filterByClass(iceUdpExts, IceUdpTransportPacketExtension.class);
        Assert.assertEquals(1, iceUdpExts.size());
        for (int i = 0; i < iceUdpExts.size(); ++i) {
            Assert.assertEquals(4, iceUdpExts.get(i).getCandidateList().size());
        }
        verifyCandidateExtension(iceUdpExts.get(0).getCandidateList().get(1), 1, "1", "udp", 1, 0,
                CandidateType.host, "172.22.76.221", 53441);
        verifyCandidateExtension(iceUdpExts.get(0).getCandidateList().get(2), 2, "1", "udp", 2, 0,
                CandidateType.srflx, "172.22.76.221", 46128);
    }

    private static void verifyCandidateExtension(CandidatePacketExtension candidateExt, int component,
                                                 String foundation,
                                                 String protocol, int priority, int generation, CandidateType type,
                                                 String ip, int port) {

        Assert.assertEquals(component, candidateExt.getComponent());
        Assert.assertEquals(foundation, candidateExt.getFoundation());
        Assert.assertEquals(protocol, candidateExt.getProtocol());
        Assert.assertEquals(priority, candidateExt.getPriority());
        Assert.assertEquals(generation, candidateExt.getGeneration());
        Assert.assertEquals(type, candidateExt.getType());
        Assert.assertEquals(ip, candidateExt.getIP());
        Assert.assertEquals(port, candidateExt.getPort());
    }

    @Test
    public void testSdpToJingle() throws Exception {
        prepare();
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        verifyJingleIq(jingle, true);
    }

    @Test
    public void testJingleToSdp() throws Exception {
        prepare();
        // SDP => Jingle
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        Assert.assertNotNull(jingle);

        // Jingle => SDP
        sdp = SdpToJingle.sdpFromJingle0(jingle);
        Assert.assertNotNull(sdp);

        jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        verifyJingleIq(jingle, false);
    }

    @Test
    public void testRtcpMuxPresentInSdp() {
        prepare();
        MediaDescription audio = sdp.getMediaDescriptions()[0];
        Assert.assertTrue(audio.getAttributes("rtcp-mux").length == 1);
    }

    @Test
    public void testRtcpMuxAbsentInSdp() {
        prepare(false);
        MediaDescription audio = sdp.getMediaDescriptions()[0];
        Assert.assertTrue(audio.getAttributes("rtcp-mux").length == 0);
    }

    @Test
    public void testRtcpMuxPresentInJingle() throws Exception {
        prepare();
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();

        List<ContentPacketExtension> contents = jingle.getContentList();
        for (ContentPacketExtension content : contents) {

            List<RtpDescriptionPacketExtension> descriptionExts =
                    content.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);
            RtpDescriptionPacketExtension descriptionExt = descriptionExts.get(0);
            List<RtcpMuxExtension> rtcpMuxExts = descriptionExt.getChildExtensionsOfType(RtcpMuxExtension.class);
            Assert.assertTrue(rtcpMuxExts.size() == 1);
        }
    }

    @Test
    public void testRtcpMuxAbsentInJingle() throws Exception {
        prepare(false);
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();

        List<ContentPacketExtension> contents = jingle.getContentList();
        for (ContentPacketExtension content : contents) {

            List<RtpDescriptionPacketExtension> descriptionExts =
                    content.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);
            RtpDescriptionPacketExtension descriptionExt = descriptionExts.get(0);
            List<RtcpMuxExtension> rtcpMuxExts = descriptionExt.getChildExtensionsOfType(RtcpMuxExtension.class);
            Assert.assertTrue(rtcpMuxExts.size() == 0);
        }
    }

    @Test
    public void testCryptoPresentInJingle() throws Exception {
        prepare();
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        List<ContentPacketExtension> contents = jingle.getContentList();

        Assert.assertNotNull(contents);
        Assert.assertFalse(contents.isEmpty());

        for (ContentPacketExtension content : contents) {
            List<RtpDescriptionPacketExtension> descriptionExts =
                    content.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);
            RtpDescriptionPacketExtension descriptionExt = descriptionExts.get(0);
            List<EncryptionPacketExtension> encryptionExts =
                    descriptionExt.getChildExtensionsOfType(EncryptionPacketExtension.class);
            Assert.assertTrue(encryptionExts.size() == 1);
            EncryptionPacketExtension encryptionExt = encryptionExts.get(0);
//            Assert.assertTrue(encryptionExt.isRequired());
            List<CryptoPacketExtension> cryptoExts =
                    encryptionExt.getChildExtensionsOfType(CryptoPacketExtension.class);
            Assert.assertTrue(cryptoExts.size() == 1);
            CryptoPacketExtension cryptoExt = cryptoExts.get(0);
            Assert.assertTrue(cryptoExt.getCryptoSuite().equals("AES_CM_128_HMAC_SHA1_32") || cryptoExt
                    .getCryptoSuite()
                    .equals("AES_CM_128_HMAC_SHA1_80"));
            Assert.assertEquals(cryptoExt.getTag(), "0");
            Assert.assertTrue(cryptoExt.getKeyParams().equals("inline:keNcG3HezSNID7LmfDa9J4lfdUL8W1F7TNJKcbuy")
                              || cryptoExt.getKeyParams().equals("inline:5ydJsA+FZVpAyqJMT/nW/UW+tcOmDvXJh/pPhNRe"));
        }
    }

    @Test
    public void testCryptoPresentInSdp() throws Exception {
        prepare();
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        Assert.assertNotNull(jingle);

        sdp = SdpToJingle.sdpFromJingle0(jingle);
        Assert.assertNotNull(sdp);

        String text = sdp.toString();
        Assert.assertTrue(
                text.contains("a=crypto:0 AES_CM_128_HMAC_SHA1_32 inline:keNcG3HezSNID7LmfDa9J4lfdUL8W1F7TNJKcbuy"));
        Assert.assertTrue(
                text.contains("a=crypto:0 AES_CM_128_HMAC_SHA1_80 inline:5ydJsA+FZVpAyqJMT/nW/UW+tcOmDvXJh/pPhNRe"));
    }

    @Test
    public void testStreamsPresentInJingle() throws Exception {
        prepare();
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        List<ContentPacketExtension> contents = jingle.getContentList();
        ContentPacketExtension audio = contents.get(0);
        List<RtpDescriptionPacketExtension> descriptionExts =
                audio.getChildExtensionsOfType(RtpDescriptionPacketExtension.class);

        RtpDescriptionPacketExtension descriptionExt = descriptionExts.get(0);

        List<StreamsPacketExtension> streamsExts = descriptionExt.getChildExtensionsOfType(StreamsPacketExtension
                .class);
        Assert.assertTrue(streamsExts.size() == 1);
        StreamsPacketExtension streamsExt = streamsExts.get(0);
        List<StreamPacketExtension> streamExts = streamsExt.getChildExtensionsOfType(StreamPacketExtension.class);
        Assert.assertEquals(1, streamExts.size());
        StreamPacketExtension streamExt = streamExts.get(0);
        Assert.assertTrue(streamExt.getAttributeNames().size() == 3);
        Assert.assertEquals(streamExt.getAttribute("cname"), "hsWuSQJxx7przmb8");
        Assert.assertEquals(streamExt.getAttribute("mslabel"), "stream_label");
        Assert.assertEquals(streamExt.getAttribute("label"), "audio_label");
        SsrcPacketExtension ssrcExt = streamExt.getSsrc();
        Assert.assertEquals(ssrcExt.getText(), "2570980487");
    }

    @Test
    public void testSsrcPresentInSdp() throws Exception {
        prepare();
        JingleIQ jingle = SdpToJingle.jingleFromSdp0(sdp).getDesc().getJingleIQ();
        Assert.assertNotNull(jingle);

        sdp = SdpToJingle.sdpFromJingle0(jingle);
        Assert.assertNotNull(sdp);

        String text = sdp.toString();
        Assert.assertTrue(text.contains("a=ssrc:2570980487 cname:hsWuSQJxx7przmb8"));
        Assert.assertTrue(text.contains("a=ssrc:2570980487 mslabel:stream_label"));
        Assert.assertTrue(text.contains("a=ssrc:2570980487 label:audio_label"));

        Assert.assertTrue(text.contains("a=ssrc:43633328 cname:hsWuSQJxx7przmb8"));
        Assert.assertTrue(text.contains("a=ssrc:43633328 mslabel:stream_label"));
        Assert.assertTrue(text.contains("a=ssrc:43633328 label:video_label"));
    }

    @Test
    public void testJingeIceCandidatesFromSdpStub() {
        List<String> iceCandidates = Arrays.asList(SAMPLE_ICE_CANDIDATES_SDP_STUB.split("\r\n"));
        JingleIQ iq = SdpToJingle.transportInfoFromSdpStub0(iceCandidates, SAMPLE_SID, MEDIA_NAME);

        Assert.assertNotNull(iq);
        Assert.assertEquals(SAMPLE_SID, iq.getSID());
        Assert.assertEquals(IQ.Type.SET, iq.getType());

        List<ContentPacketExtension> contentList = iq.getContentList();
        Assert.assertEquals(1, contentList.size());

        ContentPacketExtension content = contentList.get(0);
        Assert.assertEquals(MEDIA_NAME, content.getName());

        ContentPacketExtension ice = iq.getContentForType(IceUdpTransportPacketExtension.class);
        List<IceUdpTransportPacketExtension> iceUdpExts = ice.getChildExtensionsOfType(IceUdpTransportPacketExtension
                .class);
        iceUdpExts = Utils.filterByClass(iceUdpExts, IceUdpTransportPacketExtension.class);

        Assert.assertEquals(1, iceUdpExts.size());

        for (int i = 0; i < iceUdpExts.size(); ++i) {
            Assert.assertEquals(8, iceUdpExts.get(i).getCandidateList().size());
        }

        verifyCandidateExtension(iceUdpExts.get(0).getCandidateList().get(1), 1, "1", "udp", 1, 0, CandidateType.host,
                "172.22.76.221", 48235);
        verifyCandidateExtension(iceUdpExts.get(0).getCandidateList().get(2), 2, "1", "udp", 2, 0, CandidateType.srflx,
                "172.22.76.221", 36798);
    }

    @Test
    public void getGTalkTransportPacketExtensionForAudio() throws SDPParseException {
        // setup
        final String[] rawAttrs =
                ("a=candidate:1 2 udp 1 172.22.76.221 47216 typ host generation 0\r\n"
                 + "a=candidate:1 1 udp 1 172.22.76.221 48235 typ host generation 0\r\n"
                 + "a=candidate:1 2 udp 2 172.22.76.221 36798 typ srflx raddr 10.0.34.44 rport 48296 generation 0\r\n"
                 + "a=candidate:1 1 udp 2 172.22.76.221 50102 typ relay raddr 213.99.45.11 rport 4313 generation 0\r\n")
                        .split("\r\n");

        final Attribute[] attributes = new Attribute[rawAttrs.length];
        for (int i = 0; i < rawAttrs.length; i++) {
            attributes[i] = Attribute.parse(rawAttrs[i]);
        }

        final Attribute userFragment = Attribute.parse("a=ice-ufrag:YuWMyUbmK/CX6awo");
        final Attribute password = Attribute.parse("a=ice-pwd:DpueNNn6/r6TTRFMqNWw0v/c");

        final Media audio = Media.parse("m=audio 36798 RTP/AVPF 103 104 110 107 9 102 108 0 8 106 105 13 127 126");
        // execute the test
        final GTalkTransportPacketExtension transport =
                SdpToJingle.getGTalkTransportPacketExtension(audio, attributes, userFragment, password);

        // verify the result
        assertThat(transport, notNullValue());


        assertThat(XmlConverters.the(transport.toXML()), XmlMatchers.equivalentTo(
                XmlConverters.the("<transport xmlns=\"http://www.google.com/transport/p2p\">\n"
                                  + "<candidate \n"
                                  + "name=\"rtcp\" \n"
                                  + "protocol=\"udp\"\n"
                                  + "preference=\"1.0\"\n"
                                  + "address=\"172.22.76.221\" port=\"47216\"\n"
                                  + "type=\"local\"\n"
                                  + "generation=\"0\"\n"
                                  + "network=\"0\"\n"
                                  + "username=\"YuWMyUbmK/CX6awo\" password=\"DpueNNn6/r6TTRFMqNWw0v/c\" />\n"
                                  + "\n"
                                  + "<candidate \n"
                                  + "name=\"rtp\" \n"
                                  + "protocol=\"udp\"\n"
                                  + "preference=\"1.0\"\n"
                                  + "address=\"172.22.76.221\" port=\"48235\"\n"
                                  + "type=\"local\"\n"
                                  + "generation=\"0\"\n"
                                  + "network=\"0\"\n"
                                  + "username=\"YuWMyUbmK/CX6awo\" password=\"DpueNNn6/r6TTRFMqNWw0v/c\" />\n"
                                  + "\n"
                                  + "<candidate \n"
                                  + "name=\"rtcp\" \n"
                                  + "protocol=\"udp\"\n"
                                  + "preference=\"0.5\"\n"
                                  + "address=\"10.0.34.44\" port=\"48296\"\n"
                                  + "type=\"stun\"\n"
                                  + "generation=\"0\"\n"
                                  + "network=\"0\"\n"
                                  + "username=\"YuWMyUbmK/CX6awo\" password=\"DpueNNn6/r6TTRFMqNWw0v/c\" />\n"
                                  + "\n"
                                  + "<candidate \n"
                                  + "name=\"rtp\" \n"
                                  + "protocol=\"udp\"\n"
                                  + "preference=\"0.5\"\n"
                                  + "address=\"213.99.45.11\" port=\"4313\"\n"
                                  + "type=\"relay\"\n"
                                  + "generation=\"0\"\n"
                                  + "network=\"0\"\n"
                                  + "username=\"YuWMyUbmK/CX6awo\" password=\"DpueNNn6/r6TTRFMqNWw0v/c\" />\n"
                                  + "</transport>")));
    }

    @Test
    public void testGTalkJingleTransportInfoToSdp() throws Exception {
        //setup
        final JingleIQ jingleIQ =
                new XmlElementParser().
                        parseJingleIQ("<iq to=\"ivan@debian/call\" type=\"set\" id=\"11\">\n"
                                      + "\t<jingle xmlns=\"urn:xmpp:jingle:1\" action=\"transport-info\" "
                                      + "sid=\"4017019983535482348\">\n"
                                      + "\t\t<content name=\"audio\" creator=\"initiator\">\n"
                                      + "\t\t\t<transport xmlns=\"http://www.google.com/transport/p2p\">\n"
                                      + "\t\t\t\t<candidate name=\"rtp\" address=\"192.168.1.119\" port=\"44633\" "
                                      + "preference=\"0.99\" username=\"Z3XlEJEw5uQAuwrq\" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"jQPYHkgERV9g1VejkqzfW1gw\" type=\"local\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"rtp\" address=\"92.168.1.119\" port=\"44635\" "
                                      + "preference=\"0.99\" username=\"Z3XlEJEw5uQAuwrq\" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"jQPYHkgERV9g1VejkqzfW1gw\" type=\"stun\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"rtp\" address=\"2.168.1.119\" port=\"44637\" "
                                      + "preference=\"0.89\" username=\"Z3XlEJEw5uQAuwrq\" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"jQPYHkgERV9g1VejkqzfW1gw\" type=\"relay\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"rtcp\" address=\"195.138.129.182\" port=\"42556\" "
                                      + "preference=\"0.86\" username=\"Z3XlEJEw5uQAuwrq\" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"jQPYHkgERV9g1VejkqzfW1gw\" type=\"local\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"rtcp\" address=\"95.138.129.182\" port=\"42558\" "
                                      + "preference=\"0.86\" username=\"Z3XlEJEw5uQAuwrq\" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"jQPYHkgERV9g1VejkqzfW1gw\" type=\"stun\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"rtcp\" address=\"5.138.129.182\" port=\"44640\" "
                                      + "preference=\"0.76\" username=\"Z3XlEJEw5uQAuwrq\" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"jQPYHkgERV9g1VejkqzfW1gw\" type=\"relay\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t</transport>\n"
                                      + "\t\t</content>\n"
                                      + "\t\t<content name=\"video\" creator=\"initiator\">\n"
                                      + "\t\t\t<transport xmlns=\"http://www.google.com/transport/p2p\">\n"
                                      + "\t\t\t\t<candidate name=\"video_rtp\" address=\"192.168.1.119\" "
                                      + "port=\"44633\" preference=\"0.99\" username=\"tIr7zMAM/5UqVTiY\" "
                                      + "protocol=\"udp\" generation=\"0\" password=\"uRt7Pxqor1e5RwnGjBKufoH+\" "
                                      + "type=\"local\" network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"video_rtp\" address=\"92.168.1.119\" "
                                      + "port=\"44635\" preference=\"0.99\" username=\"tIr7zMAM/5UqVTiY \" "
                                      + "protocol=\"udp\" generation=\"0\" password=\"uRt7Pxqor1e5RwnGjBKufoH+\" "
                                      + "type=\"stun\" network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"video_rtp\" address=\"2.168.1.119\" port=\"44637\""
                                      + " preference=\"0.89\" username=\"tIr7zMAM/5UqVTiY \" protocol=\"udp\" "
                                      + "generation=\"0\" password=\"uRt7Pxqor1e5RwnGjBKufoH+\" type=\"relay\" "
                                      + "network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"video_rtcp\" address=\"195.138.129.182\" "
                                      + "port=\"42556\" preference=\"0.86\" username=\"tIr7zMAM/5UqVTiY \" "
                                      + "protocol=\"udp\" generation=\"0\" password=\"uRt7Pxqor1e5RwnGjBKufoH+\" "
                                      + "type=\"local\" network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"video_rtcp\" address=\"95.138.129.182\" "
                                      + "port=\"42558\" preference=\"0.86\" username=\"tIr7zMAM/5UqVTiY \" "
                                      + "protocol=\"udp\" generation=\"0\" password=\"uRt7Pxqor1e5RwnGjBKufoH+\" "
                                      + "type=\"stun\" network=\"eth0\"/>\n"
                                      + "\t\t\t\t<candidate name=\"video_rtcp\" address=\"5.138.129.182\" "
                                      + "port=\"44640\" preference=\"0.76\" username=\"tIr7zMAM/5UqVTiY \" "
                                      + "protocol=\"udp\" generation=\"0\" password=\"uRt7Pxqor1e5RwnGjBKufoH+\" "
                                      + "type=\"relay\" network=\"eth0\"/>\n"
                                      + "\t\t\t</transport>\n"
                                      + "\t\t</content>\n"
                                      + "\t</jingle>\n"
                                      + "</iq>");

        SessionDescription expectedSdp = SDPFactory.
                parseSessionDescription(
                        "v=0\r\n"
                        + "o=- 4017019983535482348 2 IN IP4 127.0.0.1\r\n"
                        + "s=- \r\n"
                        + "t=0 0\r\n"
                        + "m=audio 1 RTP/SAVPF 1\r\n"
                        + "c=IN IP4 127.0.0.1\r\n"
                        + "a=ice-ufrag:Z3XlEJEw5uQAuwrq\r\n"
                        + "a=ice-pwd:jQPYHkgERV9g1VejkqzfW1gw\r\n"
                        + "a=candidate:1 1 udp 990000009 192.168.1.119 44633 typ host generation 0\r\n"
                        + "a=candidate:1 1 udp 990000009 92.168.1.119 44635 typ srflx raddr 92.168.1.119 rport 44635 "
                        + "generation 0\r\n"
                        + "a=candidate:1 1 udp 889999985 2.168.1.119 44637 typ relay raddr 2.168.1.119 rport 44637 "
                        + "generation 0\r\n"
                        + "a=candidate:1 2 udp 860000014 195.138.129.182 42556 typ host generation 0\r\n"
                        + "a=candidate:1 2 udp 860000014 95.138.129.182 42558 typ srflx raddr 95.138.129.182 rport "
                        + "42558 generation 0\r\n"
                        + "a=candidate:1 2 udp 759999990 5.138.129.182 44640 typ relay raddr 5.138.129.182 rport "
                        + "44640 generation 0\r\n"
                        + "m=video 1 RTP/SAVPF 1\r\n"
                        + "c=IN IP4 127.0.0.1\r\n"
                        + "a=ice-ufrag:tIr7zMAM/5UqVTiY\r\n"
                        + "a=ice-pwd:uRt7Pxqor1e5RwnGjBKufoH+\r\n"
                        + "a=candidate:1 1 udp 990000009 192.168.1.119 44633 typ host generation 0\r\n"
                        + "a=candidate:1 1 udp 990000009 92.168.1.119 44635 typ srflx raddr 92.168.1.119 rport 44635 "
                        + "generation 0\r\n"
                        + "a=candidate:1 1 udp 889999985 2.168.1.119 44637 typ relay raddr 2.168.1.119 rport 44637 "
                        + "generation 0\r\n"
                        + "a=candidate:1 2 udp 860000014 195.138.129.182 42556 typ host generation 0\r\n"
                        + "a=candidate:1 2 udp 860000014 95.138.129.182 42558 typ srflx raddr 95.138.129.182 rport "
                        + "42558 generation 0\r\n"
                        + "a=candidate:1 2 udp 759999990 5.138.129.182 44640 typ relay raddr 5.138.129.182 rport "
                        + "44640 generation 0\r\n");

        final SdpToJingle underTest = new SdpToJingle();

        //execute tests
        final SessionDescription sdp = underTest.sdpFromJingle(jingleIQ);

        //verify results
        assertThat(sdp, notNullValue());
        assertThat(sdp.getMediaDescriptions()[0].toString(), equalTo(expectedSdp.getMediaDescriptions()[0].toString()));
        assertThat(sdp.getMediaDescriptions()[1].toString(), equalTo(expectedSdp.getMediaDescriptions()[1].toString()));

    }

    @Test
    public void parseLibjingleJingleIQ() throws Exception {
        final SdpToJingle s = new SdpToJingle();
        final XmlElementParser elementParser = new XmlElementParser();
        final JingleIQ jingleIQ = elementParser.parseJingleIQ(
                "<iq to=\"kjpm6v2f300029h@debian/SNH-P6410BN\" type=\"set\" id=\"14\">\n"
                + "<jingle xmlns=\"urn:xmpp:jingle:1\" action=\"session-initiate\" sid=\"653507340122337134\" "
                + "initiator=\"ivan@debian/call\">\n"
                + "<content name=\"audio\" creator=\"initiator\">\n"
                + "<description xmlns=\"urn:xmpp:jingle:apps:rtp:1\" media=\"audio\" ssrc=\"1139350810\">\n"
                + "<payload-type id=\"111\" name=\"opus\" clockrate=\"48000\" channels=\"2\">\n"
                + "<parameter name=\"bitrate\" value=\"64000\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"103\" name=\"ISAC\" clockrate=\"16000\"/>\n"
                + "<payload-type id=\"104\" name=\"ISAC\" clockrate=\"32000\"/>\n"
                + "<payload-type id=\"9\" name=\"G722\" clockrate=\"16000\">\n"
                + "<parameter name=\"bitrate\" value=\"64000\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"102\" name=\"ILBC\" clockrate=\"8000\">\n"
                + "<parameter name=\"bitrate\" value=\"13300\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"0\" name=\"PCMU\" clockrate=\"8000\">\n"
                + "<parameter name=\"bitrate\" value=\"64000\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"8\" name=\"PCMA\" clockrate=\"8000\">\n"
                + "<parameter name=\"bitrate\" value=\"64000\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"107\" name=\"CN\" clockrate=\"48000\"/>\n"
                + "<payload-type id=\"106\" name=\"CN\" clockrate=\"32000\"/>\n"
                + "<payload-type id=\"105\" name=\"CN\" clockrate=\"16000\"/>\n"
                + "<payload-type id=\"13\" name=\"CN\" clockrate=\"8000\"/>\n"
                + "<payload-type id=\"127\" name=\"red\" clockrate=\"8000\"/>\n"
                + "<payload-type id=\"126\" name=\"telephone-event\" clockrate=\"8000\"/>\n"
                + "<encryption>\n"
                + "<crypto tag=\"0\" crypto-suite=\"AES_CM_128_HMAC_SHA1_32\" "
                + "key-params=\"inline:mP4p+RZTGESNZ403Ig7R0MaLeFdGklnIvQiLWTTX\"/>\n"
                + "<crypto tag=\"1\" crypto-suite=\"AES_CM_128_HMAC_SHA1_80\" "
                + "key-params=\"inline:Bz5hLIYtbAL5S7O+cTiOaTUGDSJq3fKin8wowc9t\"/>\n"
                + "</encryption>\n"
                + "<rtcp-mux/>\n"
                + "<rtp-hdrext uri=\"urn:ietf:params:rtp-hdrext:ssrc-audio-level\" id=\"1\"/>\n"
                + "</description>\n"
                + "<transport xmlns=\"http://www.google.com/transport/p2p\"/>\n"
                + "</content>\n"
                + "<content name=\"video\" creator=\"initiator\">\n"
                + "<description xmlns=\"urn:xmpp:jingle:apps:rtp:1\" media=\"video\" ssrc=\"1531679997\">\n"
                + "<payload-type id=\"99\" name=\"H264\" clockrate=\"90000\">\n"
                + "<parameter name=\"profile-level-id\" value=\"42E01f\"/>\n"
                + "<parameter name=\"imageattr\" value=\"send * recv [x=[0-1920],y=[0-1200]]\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"100\" name=\"VP8\">\n"
                + "<parameter name=\"width\" value=\"640\"/>\n"
                + "<parameter name=\"height\" value=\"400\"/>\n"
                + "<parameter name=\"framerate\" value=\"30\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"116\" name=\"red\">\n"
                + "<parameter name=\"width\" value=\"640\"/>\n"
                + "<parameter name=\"height\" value=\"400\"/>\n"
                + "<parameter name=\"framerate\" value=\"30\"/>\n"
                + "</payload-type>\n"
                + "<payload-type id=\"117\" name=\"ulpfec\">\n"
                + "<parameter name=\"width\" value=\"640\"/>\n"
                + "<parameter name=\"height\" value=\"400\"/>\n"
                + "<parameter name=\"framerate\" value=\"30\"/>\n"
                + "</payload-type>\n"
                + "<encryption>\n"
                + "<crypto tag=\"0\" crypto-suite=\"AES_CM_128_HMAC_SHA1_80\" "
                + "key-params=\"inline:V0cbXfc/9sDZX8TWY4p9uEG5CrxTRTTsLTebypzc\"/>\n"
                + "</encryption>\n"
                + "<rtcp-mux/>\n"
                + "<rtp-hdrext uri=\"urn:ietf:params:rtp-hdrext:toffset\" id=\"2\"/>\n"
                + "<rtp-hdrext uri=\"http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\" id=\"3\"/>\n"
                + "</description>\n"
                + "<transport xmlns=\"http://www.google.com/transport/p2p\"/>\n"
                + "</content>\n"
                + "<jin:group xmlns:jin='google:jingle' type='BUNDLE'>\n"
                + "<content name='audio'/>\n"
                + "<content name='video'/>\n"
                + "</jin:group>"
                + "</jingle>\n"
                + "<additional-info xmlns='techwin:jingle' nat-type='full_cone' profile-id='6'>\n"
                + "<rtsp xmlns='' uri=''/>\n"
                + "</additional-info>"
                + "</iq>");

        final SessionDescription sdp = s.sdpFromJingle(jingleIQ);

        final SessionDescription expectedSdp =
                SDPFactory.parseSessionDescription("v=0\r\n"
                                                   + "o=ProfessorFarnsworth 653507340122337134 3635226886 "
                                                   + "IN IP4 127.0.0.1\r\n"
                                                   + "s=-\r\n"
                                                   + "t=0 0\r\n"
                                                   + "m=audio 123456789 RTP/SAVPF 111 103 104 9 102 0 8 "
                                                   + "107 106 105 13 127 "
                                                   + "126\r\n"
                                                   + "c=IN IP4 127.0.0.1\r\n"
                                                   + "a=rtcp:36798 IN IP4 127.0.0.1\r\n"
                                                   + "a=sendrecv\r\n"
                                                   + "a=rtpmap:111 OPUS/48000\r\n"
                                                   + "a=rtpmap:103 ISAC/16000\r\n"
                                                   + "a=rtpmap:104 ISAC/32000\r\n"
                                                   + "a=rtpmap:9 G722/16000\r\n"
                                                   + "a=rtpmap:102 ILBC/8000\r\n"
                                                   + "a=rtpmap:0 PCMU/8000\r\n"
                                                   + "a=rtpmap:8 PCMA/8000\r\n"
                                                   + "a=rtpmap:107 CN/48000\r\n"
                                                   + "a=rtpmap:106 CN/32000\r\n"
                                                   + "a=rtpmap:105 CN/16000\r\n"
                                                   + "a=rtpmap:13 CN/8000\r\n"
                                                   + "a=rtpmap:127 RED/8000\r\n"
                                                   + "a=rtpmap:126 TELEPHONE-EVENT/8000\r\n"
                                                   + "a=rtcp-mux\r\n"
                                                   + "a=mid:audio\r\n"
                                                   + "a=crypto:0 AES_CM_128_HMAC_SHA1_32 "
                                                   + "inline:mP4p+RZTGESNZ403Ig7R0MaLeFdGklnIvQiLWTTX\r\n"
                                                   + "a=crypto:1 AES_CM_128_HMAC_SHA1_80 "
                                                   + "inline:Bz5hLIYtbAL5S7O+cTiOaTUGDSJq3fKin8wowc9t\r\n"
                                                   + "m=video 123456789 RTP/SAVPF 99 100 116 117\r\n"
                                                   + "c=IN IP4 127.0.0.1\r\n"
                                                   + "a=rtcp:36798 IN IP4 127.0.0.1\r\n"
                                                   + "a=sendrecv\r\n"
                                                   + "a=rtpmap:99 H264/90000\r\n"
                                                   + "a=rtpmap:100 VP8/90000\r\n"
                                                   + "a=rtpmap:116 RED/90000\r\n"
                                                   + "a=rtpmap:117 ULPFEC/90000\r\n"
                                                   + "a=rtcp-mux\r\n"
                                                   + "a=mid:video\r\n"
                                                   + "a=crypto:0 AES_CM_128_HMAC_SHA1_80 "
                                                   + "inline:V0cbXfc/9sDZX8TWY4p9uEG5CrxTRTTsLTebypzc");

        assertThat(sdp, notNullValue());
        assertThat(sdp.getMediaDescriptions()[0].toString(), equalTo(expectedSdp.getMediaDescriptions()[0].toString()));
        assertThat(sdp.getMediaDescriptions()[1].toString(), equalTo(expectedSdp.getMediaDescriptions()[1].toString()));
    }
}
