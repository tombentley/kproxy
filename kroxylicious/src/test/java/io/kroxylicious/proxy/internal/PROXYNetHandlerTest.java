/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import static io.kroxylicious.proxy.internal.PROXYNetHandler.Mode.PROXY_V1;
import static io.kroxylicious.proxy.internal.PROXYNetHandler.Mode.PROXY_V2;
import static org.junit.jupiter.api.Assertions.*;

public class PROXYNetHandlerTest {
    private static ByteBuf bufferOf(String x) {
        return Unpooled.copiedBuffer(x.getBytes(StandardCharsets.US_ASCII));
    }

    private static ByteBuf bufferOf(int... ints) {
        byte[] bytes = new byte[ints.length];
        for (int j = 0; j < ints.length; j++) {
            bytes[j] = (byte) ints[j];
        }
        return Unpooled.copiedBuffer(bytes);
    }

    @Test
    public void testReadAsciiDecimal() {
        ByteBuf buffer = bufferOf("42 ");
        assertEquals(42, PROXYNetHandler.readAsciiDecimal(buffer, ' ', 3));
        assertEquals(3, buffer.readerIndex());

        buffer.readerIndex(0);
        var e = assertThrows(IllegalStateException.class, () -> PROXYNetHandler.readAsciiDecimal(buffer, ' ', 2));
        assertEquals("Expected terminator (SPACE) not found within 2 bytes", e.getMessage());
        assertEquals(2, buffer.readerIndex());

        var buffer2 = bufferOf("4a ");
        e = assertThrows(IllegalStateException.class, () -> PROXYNetHandler.readAsciiDecimal(buffer2, ' ', 3));
        assertEquals("Unexpected character: LATIN SMALL LETTER A", e.getMessage());
        assertEquals(2, buffer2.readerIndex());
    }

    @Test
    public void testReadAsciiHex() {
        ByteBuf buffer = bufferOf("ffff");
        assertEquals(0xffff, PROXYNetHandler.readAsciiHex(buffer));
        assertEquals(4, buffer.readerIndex());

        buffer = bufferOf("abcd");
        assertEquals(0xabcd, PROXYNetHandler.readAsciiHex(buffer));
        assertEquals(4, buffer.readerIndex());
    }

    @Test
    public void testProxyV1Tcp4() {
        var decodeEvent = handleGood(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.7 8 9\r\n");
        assertTrue(decodeEvent.srcAddr() instanceof Inet4Address);
        assertArrayEquals(new byte[]{ 9, 1, 2, 3 }, ((Inet4Address) decodeEvent.srcAddr()).getAddress());
        assertEquals(8, decodeEvent.srcPort());
        assertTrue(decodeEvent.destAddr() instanceof Inet4Address);
        assertArrayEquals(new byte[]{ 4, 5, 6, 7 }, ((Inet4Address) decodeEvent.destAddr()).getAddress());
        assertEquals(9, decodeEvent.destPort());

        decodeEvent = handleGood(PROXY_V1, "PROXY TCP4 9.1.2.3 255.5.6.7 8 9\r\n");
        assertTrue(decodeEvent.srcAddr() instanceof Inet4Address);
        assertArrayEquals(new byte[]{ 9, 1, 2, 3 }, ((Inet4Address) decodeEvent.srcAddr()).getAddress());
        assertEquals(8, decodeEvent.srcPort());
        assertTrue(decodeEvent.destAddr() instanceof Inet4Address);
        assertArrayEquals(new byte[]{ (byte) 0xff, 5, 6, 7 }, ((Inet4Address) decodeEvent.destAddr()).getAddress());
        assertEquals(9, decodeEvent.destPort());

        decodeEvent = handleGood(PROXY_V1, "PROXY TCP4 9.1.2.3 255.5.6.7 8 9\r\nwith some extra",
                "with some extra");
        assertTrue(decodeEvent.srcAddr() instanceof Inet4Address);
        assertArrayEquals(new byte[]{ 9, 1, 2, 3 }, ((Inet4Address) decodeEvent.srcAddr()).getAddress());
        assertEquals(8, decodeEvent.srcPort());
        assertTrue(decodeEvent.destAddr() instanceof Inet4Address);
        assertArrayEquals(new byte[]{ (byte) 0xff, 5, 6, 7 }, ((Inet4Address) decodeEvent.destAddr()).getAddress());
        assertEquals(9, decodeEvent.destPort());

        // Leading 0 disallowed in TCP4 decimal (addr)
        handleBad(PROXY_V1, "PROXY TCP4 0.1.2.3 4.5.6.7 8 9\r\n");

        // Leading 0 disallowed in TCP4 decimal (port)
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.7 0 9\r\n");

        // Too big dest port
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.7 8 999999999\r\n");

        // Too big src port
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.7 8888888888 9\r\n");

        // Unexpected separator
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.7 8_9\r\n");

        // Too many in dotted quad
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.7.0 8 9\r\n");

        // Byte overflow
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.256 8 9\r\n");

        // Too many digits
        handleBad(PROXY_V1, "PROXY TCP4 9.1.2.3 4.5.6.2567 8 9\r\n");
    }

    private PROXYDecodeEvent handleGood(PROXYNetHandler.Mode mode, String v1Header) {
        return handleGood(mode, bufferOf(v1Header), null);
    }

    private PROXYDecodeEvent handleGood(PROXYNetHandler.Mode mode, ByteBuf v1Header) {
        return handleGood(mode, v1Header, null);
    }

    private PROXYDecodeEvent handleGood(PROXYNetHandler.Mode mode, String v1Header, String extra) {
        return handleGood(mode, bufferOf(v1Header), bufferOf(extra));
    }

    private PROXYDecodeEvent handleGood(PROXYNetHandler.Mode mode, ByteBuf v1Header, ByteBuf extra) {
        var channel = new EmbeddedChannel();
        var userEventCollector = new UserEventCollector();
        channel.pipeline().addLast(new PROXYNetHandler(mode), userEventCollector);
        channel.writeInbound(v1Header);
        channel.checkException();
        var decodeEvent = assertInstanceOf(PROXYDecodeEvent.class, userEventCollector.readUserEvent());
        assertNull(userEventCollector.readUserEvent(), "Expected only a single user event");
        assertEquals(List.of("UserEventCollector#0", "DefaultChannelPipeline$TailContext#0"), channel.pipeline().names(),
                "Expected handler to have removed itself");
        if (extra == null) {
            assertNull(channel.readInbound(), "Expected no more read events");
        }
        else {
            var actualExtra = assertInstanceOf(ByteBuf.class, channel.readInbound());
            assertTrue(ByteBufUtil.equals(extra, actualExtra), "Expected another read event");
        }
        return decodeEvent;
    }

    private void handleBad(PROXYNetHandler.Mode mode, String v1Header) {
        handleBad(mode, bufferOf(v1Header));
    }

    private void handleBad(PROXYNetHandler.Mode mode, ByteBuf buf) {
        var channel = new EmbeddedChannel();
        var userEventCollector = new UserEventCollector();
        channel.pipeline().addLast(new PROXYNetHandler(mode), userEventCollector);
        channel.writeInbound(buf);
        channel.checkException();
        assertNull(channel.readInbound());
        assertNull(channel.readInbound());
        assertNull(userEventCollector.readUserEvent());
        assertFalse(channel.isOpen(), "Expected channel to be closed");
    }

    @Test
    public void testProxyV1BadInetProtocol() {
        handleBad(PROXY_V1, "PROXY TCP5 0.1.2.3 4.5.6.7 8 9\r\n");
    }

    @Test
    public void testProxyV1Unknown() {
        handleBad(PROXY_V1, "PROXY UNKNOWN 0.1.2.3 4.5.6.7 8 9\r\n");
    }

    @Test
    public void testProxyV1Tcp6() {
        var event = handleGood(PROXY_V1, "PROXY TCP6 " +
                "1111:2222:3333:4444:5555:6666:7777:8888 " +
                "fefe:eded:dcdc:cbcb:baba:a9a9:12fc:ba56 " +
                "9 7\r\n");
        assertTrue(event.srcAddr() instanceof Inet6Address);
        assertEquals("/1111:2222:3333:4444:5555:6666:7777:8888", event.srcAddr().toString());
        assertEquals(9, event.srcPort());
        assertTrue(event.destAddr() instanceof Inet6Address);
        assertEquals("/fefe:eded:dcdc:cbcb:baba:a9a9:12fc:ba56", event.destAddr().toString());
        assertEquals(7, event.destPort());

        event = handleGood(PROXY_V1, "PROXY TCP6 " +
                "1234::5678 " +
                "fefe:eded:dcdc:cbcb:1234::ba56 " +
                "9 7\r\n");
        assertTrue(event.srcAddr() instanceof Inet6Address);
        assertEquals("/1234:0:0:0:0:0:0:5678", event.srcAddr().toString());
        assertEquals(9, event.srcPort());
        assertTrue(event.destAddr() instanceof Inet6Address);
        assertEquals("/fefe:eded:dcdc:cbcb:1234:0:0:ba56", event.destAddr().toString());
        assertEquals(7, event.destPort());

        event = handleGood(PROXY_V1, "PROXY TCP6 " +
                "::5678 " +
                "ba56:: " +
                "9 7\r\n");
        assertTrue(event.srcAddr() instanceof Inet6Address);
        assertEquals("/0:0:0:0:0:0:0:5678", event.srcAddr().toString());
        assertEquals(9, event.srcPort());
        assertTrue(event.destAddr() instanceof Inet6Address);
        assertEquals("/ba56:0:0:0:0:0:0:0", event.destAddr().toString());
        assertEquals(7, event.destPort());

        event = handleGood(PROXY_V1, "PROXY TCP6 " +
                ":: " +
                ":: " +
                "9 7\r\n");
        assertTrue(event.srcAddr() instanceof Inet6Address);
        assertEquals("/0:0:0:0:0:0:0:0", event.srcAddr().toString());
        assertEquals(9, event.srcPort());
        assertTrue(event.destAddr() instanceof Inet6Address);
        assertEquals("/0:0:0:0:0:0:0:0", event.destAddr().toString());
        assertEquals(7, event.destPort());

        // When hex is present it must be in groups of 4 digits
        handleBad(PROXY_V1, "PROXY TCP6 " +
                "0:0:0:0:0:0:0:0 " +
                ":: " +
                "9 7\r\n");

        // Too many groups of hex digits
        handleBad(PROXY_V1, "PROXY TCP6 " +
                "1111:2222:3333:4444:5555:6666:7777:8888:9999 " +
                ":: " +
                "9 7\r\n");

        // Too few groups of hex digits
        handleBad(PROXY_V1, "PROXY TCP6 " +
                "1111:2222:3333:4444:5555:6666:7777 " +
                ":: " +
                "9 7\r\n");
    }

    @Test
    public void testProxyV2Tcp4() {
        var event = handleGood(PROXY_V2, bufferOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A, // sig
                0x21, // version (2) and command (PROXY)
                0x11, // family (inet) and transport (stream)
                0x00, 0x0c, // length=12,
                0x11, 0x12, 0x13, 0x14, // src addr
                0x21, 0x22, 0x23, 0x24, // dest addr
                0x00, 0x15, // src port
                0xff, 0x25 // dest port
        ));
        assertEquals("/17.18.19.20", event.srcAddr().toString());
        assertEquals(21, event.srcPort());
        assertEquals("/33.34.35.36", event.destAddr().toString());
        assertEquals(65317, event.destPort());

        event = handleGood(PROXY_V2, bufferOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A, // sig
                0x21, // version (2) and command (PROXY)
                0x11, // family (inet) and transport (stream)
                0x00, 0x0c, // length=12,
                0x11, 0x12, 0x13, 0x14, // src addr
                0x21, 0x22, 0x23, 0x24, // dest addr
                0x00, 0x15, // src port
                0xff, 0x25, // dest port
                's', 'o', 'm', 'e', ' ', 'm', 'o', 'r', 'e'), bufferOf("some more"));
        assertEquals("/17.18.19.20", event.srcAddr().toString());
        assertEquals(21, event.srcPort());
        assertEquals("/33.34.35.36", event.destAddr().toString());
        assertEquals(65317, event.destPort());

    }

    @Test
    public void testProxyV2Tcp4LengthTooSmall() {
        handleBad(PROXY_V2, bufferOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A, // sig
                0x21, // version (2) and command (PROXY)
                0x11, // family (inet) and transport (stream)
                0x00, 0x0b, // length=11 which is too small for this inet family!
                0x11, 0x12, 0x13, 0x14, // src addr
                0x21, 0x22, 0x23, 0x24, // dest addr
                0x00, 0x15, // src port
                0x00, 0x25 // dest port
        ));
    }

    @Test
    public void testProxyV2Tcp4WithAlpn() {
        var event = handleGood(PROXY_V2, bufferOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A, // sig
                0x21, // version (2) and command (PROXY)
                0x11, // family (inet) and transport (stream)
                0x00, 0x0c + 5, // length=17,
                0x11, 0x12, 0x13, 0x14, // src addr
                0x21, 0x22, 0x23, 0x24, // dest addr
                0x00, 0x15, // src port
                0x00, 0x25, // dest port
                0x01, // type
                0x00, 0x02, // length
                0x00, 0x00));

        assertEquals("/17.18.19.20", event.srcAddr().toString());
        assertEquals(21, event.srcPort());
        assertEquals("/33.34.35.36", event.destAddr().toString());
        assertEquals(37, event.destPort());

    }

    @Test
    public void testProxyV2Tcp6() {
        // Without any extra data
        var event = handleGood(PROXY_V2, bufferOf(0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A, // sig
                0x21, // version (2) and command (PROXY)
                0x21, // family (inet6) and transport (stream)
                0x00, 0x24, // length=36,
                0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x21, 0x22, 0x23, 0x44, 0x25, 0x26, 0x27, 0x28, // src addr
                0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, // dest addr
                0x00, 0x29, // src port
                0x00, 0x49// dest port
        ));

        assertEquals("/1112:1314:1516:1718:2122:2344:2526:2728", event.srcAddr().toString());
        assertEquals(41, event.srcPort());
        assertEquals("/3132:3334:3536:3738:4142:4344:4546:4748", event.destAddr().toString());
        assertEquals(73, event.destPort());

    }
    //
    // // TODO for v2
    // // * With non-STREAM transport
    // // * With UNIX family
    // // * with the path doesn't include \0
    // // * With LOCAL command
    // // * With multiple extras
    // // * With out-of-bounds PP type
    // // * Tests for the SSL extras
    //
    // // TODO: Tests for PROXY.proxyAutoDetect()
}
