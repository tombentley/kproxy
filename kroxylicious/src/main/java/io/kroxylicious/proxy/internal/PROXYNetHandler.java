/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

/**
 * A Netty handler which handles HAProxy PROXY "header" sent on initial connection by HAProxy, and
 * sends a {@link PROXYDecodeEvent} user event to later handlers wanting to use this information.
 *
 * @see <a href="http://www.haproxy.org/download/2.7/doc/proxy-protocol.txt">PROXY protocol spec</a>
 */
public class PROXYNetHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PROXYNetHandler.class);

    private static final byte[] V2_SIGNATURE = {
            0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
    };
    public static final int PP2_TYPE_ALPN = 0x01;
    public static final int PP2_TYPE_AUTHORITY = 0x02;
    public static final int PP2_TYPE_CRC32C = 0x03;
    public static final int PP2_TYPE_NOOP = 0x04;
    public static final int PP2_TYPE_UNIQUE_ID = 0x05;
    public static final int PP2_TYPE_SSL = 0x20;
    public static final int PP2_SUBTYPE_SSL_VERSION = 0x21;
    public static final int PP2_SUBTYPE_SSL_CN = 0x22;
    public static final int PP2_SUBTYPE_SSL_CIPHER = 0x23;
    public static final int PP2_SUBTYPE_SSL_SIGALG = 0x24;
    public static final int PP2_SUBTYPE_SSL_KEYALG = 0x25;
    public static final int PP2_TYPE_NETNS = 0x30;

    static Family familyFromBits(int familyAndTransport) {
        switch ((familyAndTransport & 0xf0) >> 4) {
            case 0x0:
                return Family.AF_UNSPEC;
            case 0x1:
                return Family.AF_INET;
            case 0x2:
                return Family.AF_INET6;
            case 0x3:
                return Family.AF_UNIX;
            default:
                throw new IllegalStateException("Unsupported family");
        }
    }

    static Transport transportFromBits(int familyAndTransport) {
        int bits = familyAndTransport & 0x0f;
        switch (bits) {
            case 0x0:
                return Transport.UNSPEC;
            case 0x1:
                return Transport.STREAM;
            case 0x2:
                return Transport.DGRAM;
            default:
                throw new IllegalStateException("Unsupported transport");
        }
    }

    static Command commandFromBits(int versionAndCommand) {
        int bits = versionAndCommand & 0x0f;
        switch (bits) {
            case 0x0:
                return Command.LOCAL;
            case 0x1:
                return Command.PROXY;
            default:
                throw new IllegalStateException("Unsupported command");
        }
    }

    enum Command {
        LOCAL,
        PROXY;
    }

    enum Family {
        AF_UNSPEC(-1),
        AF_INET(12),
        AF_INET6(36),
        AF_UNIX(216);

        private final int length;

        private Family(int length) {
            this.length = length;
        }
    }

    enum Transport {
        UNSPEC,
        STREAM,
        DGRAM;
    }

    public static void proxyAutoDetect(ChannelHandlerContext ctx, ByteBuf buffer) {
        buffer.markReaderIndex();
        if (buffer.readableBytes() >= 16) {
            for (int i = 0; i < V2_SIGNATURE.length; i++) {
                byte b = buffer.readByte();
                if (b != V2_SIGNATURE[i]) {
                    throw new IllegalStateException();
                }
            }
            byte versionAndCommand = buffer.readByte();
            int version = (versionAndCommand & 0xf0) >> 4;
            if (version == 0x2) {
                proxyV2Rest(ctx, buffer, versionAndCommand);
                return;
            }
        }
        buffer.resetReaderIndex();
        if (buffer.readableBytes() >= 8
                && equals("PROXY", buffer.readCharSequence(5, StandardCharsets.US_ASCII))) {
            buffer.resetReaderIndex();
            proxyV1(ctx, buffer);
            return;
        }
        throw new IllegalStateException("Neither PROXY protocol v1 nor v2");
    }

    public static void proxyV2(ChannelHandlerContext ctx, ByteBuf buffer) {
        if (buffer.readableBytes() >= 16) {
            for (int i = 0; i < V2_SIGNATURE.length; i++) {
                byte b = buffer.readByte();
                if (b != V2_SIGNATURE[i]) {
                    throw new IllegalStateException();
                }
            }
            byte versionAndCommand = buffer.readByte();
            int version = (versionAndCommand & 0xf0) >> 4;
            if (version == 0x2) {
                proxyV2Rest(ctx, buffer, versionAndCommand);
            }
        }
    }

    private static void proxyV2Rest(ChannelHandlerContext ctx, ByteBuf buffer, byte versionAndCommand) {
        Command command = commandFromBits(versionAndCommand);
        switch (command) {
            case LOCAL:
                throw new UnsupportedOperationException("LOCAL command not supported");
            case PROXY:
                break;
        }
        byte familyAndTransport = buffer.readByte();
        Family family = familyFromBits(familyAndTransport);
        Transport transport = transportFromBits(familyAndTransport);
        // TODO do something with transport

        int addrLength = buffer.readUnsignedShort();
        if (family.length != -1 && addrLength < family.length) {
            throw new IllegalStateException("Family=" + family + " requires length >= " + family.length + ": given length is " + addrLength);
        }
        final int index0 = buffer.readerIndex();

        switch (family) {
            case AF_INET:
                proxyV2Inet4(ctx, buffer);
                break;
            case AF_INET6:
                proxyV2Inet6(ctx, buffer);
                break;
            case AF_UNIX:
                proxyV2Unix(buffer);
            case AF_UNSPEC:
                throw new IllegalStateException("AF_UNSPEC not supported");
            default:
                throw new IllegalStateException();
        }

        while (buffer.readerIndex() - index0 < addrLength) {
            byte pp2Type = buffer.readByte();
            int pp2Length = buffer.readUnsignedShort();
            switch (pp2Type) {
                case PP2_TYPE_ALPN:
                    LOGGER.debug("Skipping PP2_TYPE_ALPN bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_TYPE_AUTHORITY:
                    byte[] utf8Bytes = new byte[pp2Length];
                    buffer.readBytes(utf8Bytes);
                    var authority = new String(utf8Bytes, StandardCharsets.UTF_8);
                    LOGGER.debug("PP2_TYPE_AUTHORITY={}", authority);
                    // TODO support this
                    break;
                case PP2_TYPE_CRC32C:
                    LOGGER.debug("Skipping PP2_TYPE_CRC32C bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    // TODO support this
                    break;
                case PP2_TYPE_NOOP:
                    LOGGER.debug("Skipping PP2_TYPE_NOOP bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_TYPE_UNIQUE_ID:
                    byte[] uniqueId = new byte[pp2Length];
                    buffer.readBytes(uniqueId);
                    LOGGER.debug("PP2_TYPE_UNIQUE_ID={}", Arrays.toString(uniqueId));

                case PP2_TYPE_SSL:
                    LOGGER.debug("Skipping PP2_TYPE_SSL bytes");
                    // int x = buffer.readerIndex();
                    // var client = buffer.readByte();
                    // var verify = buffer.readUnsignedInt();
                    // while (pp2Length - (buffer.readerIndex() - x)) {
                    // byte pp2SslSubtype = buffer.readByte();
                    // int pp2SslSublength = buffer.readUnsignedShort();
                    // switch (pp2SslSubtype) {
                    //
                    // }
                    // }
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_SUBTYPE_SSL_VERSION:
                    LOGGER.debug("Skipping PP2_SUBTYPE_SSL_VERSION bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_SUBTYPE_SSL_CN:
                    LOGGER.debug("Skipping PP2_SUBTYPE_SSL_CN bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_SUBTYPE_SSL_CIPHER:
                    LOGGER.debug("Skipping PP2_SUBTYPE_SSL_CIPHER bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_SUBTYPE_SSL_SIGALG:
                    LOGGER.debug("Skipping PP2_SUBTYPE_SSL_SIGALG bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
                case PP2_SUBTYPE_SSL_KEYALG:
                    LOGGER.debug("Skipping PP2_SUBTYPE_SSL_KEYALG bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;

                case PP2_TYPE_NETNS:
                    LOGGER.debug("Skipping PP2_TYPE_NETNS bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;

                default:
                    LOGGER.debug("Skipping unknown PP2 bytes");
                    buffer.readerIndex(buffer.readerIndex() + pp2Length);
                    break;
            }
        }

    }

    private static void proxyV2Unix(ByteBuf buffer) {
        // per https://man7.org/linux/man-pages/man7/unix.7.html we're reading a null-terminated `char sun_path[108]`
        // and we further assume UTF-8 encoding
        var sunPath = new byte[108];
        buffer.readBytes(sunPath);
        var srcPath = new String(sunPath, 0, indexOf(sunPath, (byte) 0x00), StandardCharsets.UTF_8);
        buffer.readBytes(sunPath);
        var destPath = new String(sunPath, 0, indexOf(sunPath, (byte) 0x00), StandardCharsets.UTF_8);
        throw new IllegalStateException("AF_UNIX not supported");
    }

    private static void proxyV2Inet6(ChannelHandlerContext ctx, ByteBuf buffer) {
        final int srcPort;
        final InetAddress destAddr;
        final int destPort;
        final InetAddress srcAddr;
        var ipv6addr = new byte[16];
        buffer.readBytes(ipv6addr);
        try {
            srcAddr = Inet6Address.getByAddress(ipv6addr);
        }
        catch (UnknownHostException e) {
            // Thrown when the array was illegal length, which is impossible here
            throw new AssertionError();
        }
        buffer.readBytes(ipv6addr);
        try {
            destAddr = Inet6Address.getByAddress(ipv6addr);
        }
        catch (UnknownHostException e) {
            // Thrown when the array was illegal length, which is impossible here
            throw new AssertionError();
        }
        srcPort = buffer.readUnsignedShort();
        destPort = buffer.readUnsignedShort();
        fireProxyDecodeEvent(ctx, srcPort, destAddr, destPort, srcAddr);
    }

    private static ChannelHandlerContext fireProxyDecodeEvent(ChannelHandlerContext ctx, int srcPort, InetAddress destAddr, int destPort, InetAddress srcAddr) {
        return ctx.fireUserEventTriggered(new PROXYDecodeEvent(srcAddr, srcPort, destAddr, destPort));
    }

    private static void proxyV2Inet4(ChannelHandlerContext ctx, ByteBuf buffer) {
        final InetAddress destAddr;
        final int srcPort;
        final int destPort;
        final InetAddress srcAddr;
        var ipv4addr = new byte[4];
        buffer.readBytes(ipv4addr);
        try {
            srcAddr = Inet4Address.getByAddress(ipv4addr);
        }
        catch (UnknownHostException e) {
            // Thrown when the array was illegal length, which is impossible here
            throw new AssertionError();
        }
        buffer.readBytes(ipv4addr);
        try {
            destAddr = Inet4Address.getByAddress(ipv4addr);
        }
        catch (UnknownHostException e) {
            // Thrown when the array was illegal length, which is impossible here
            throw new AssertionError();
        }
        srcPort = buffer.readUnsignedShort();
        destPort = buffer.readUnsignedShort();
        fireProxyDecodeEvent(ctx, srcPort, destAddr, destPort, srcAddr);
    }

    private static int indexOf(byte[] array, byte soughtFor) {
        for (var i = 0; i < array.length; i++) {
            if (array[i] == soughtFor) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Try to read the PROXY protocol v1 header from the given {@code buffer},
     * firing a {@link PROXYDecodeEvent} with the source and destination addresses.
     *
     * @param ctx    The context
     * @param buffer The buffer
     * @see "§2.1 of the <a href="https://www.haproxy.org/download/2.7/doc/proxy-protocol.txt">spec for HA Proxy's PROXY protocol</a>."
     */
    static void proxyV1(ChannelHandlerContext ctx, ByteBuf buffer) {
        // PROXY v1: Either "PROXY TCP4" or "PROXY TCP6"
        CharSequence first10 = buffer.readCharSequence(11, StandardCharsets.US_ASCII);
        int maxLength;
        if (equals("PROXY TCP4 ", first10)) {
            maxLength = 56;
            // source address
            // ddd.ddd.ddd.ddd
            byte[] addr = new byte[4];
            var srcAddr = readInet4Addr(buffer, addr, ' ');
            // dest address
            var destAddr = readInet4Addr(buffer, addr, ' ');
            // source port: dddd
            var srcPort = readAsciiDecimal(buffer, ' ', 5);
            // dest port: dddd
            var destPort = readAsciiDecimal(buffer, '\r', 5);
            // CRLF
            discard(buffer, '\n');

            fireProxyDecodeEvent(ctx, srcPort, destAddr, destPort, srcAddr);
        }
        else if (equals("PROXY TCP6 ", first10)) {
            maxLength = 104;
            byte[] addr = new byte[16];
            // source address: hhhh:hhhh:hhhh:hhhh
            var srcAddr = readInet6Addr(buffer, addr);
            // dest address: hhhh:hhhh:hhhh:hhhh
            var destAddr = readInet6Addr(buffer, addr);
            // source port: dddd
            var srcPort = readAsciiDecimal(buffer, ' ', 5);
            // dest port: dddd
            var destPort = readAsciiDecimal(buffer, '\r', 5);
            // CRLF
            discard(buffer, '\n');
            fireProxyDecodeEvent(ctx, srcPort, destAddr, destPort, srcAddr);
        }
        else {
            if (equals("PROXY UNKNO", first10)
                    && equals("WN ", buffer.readCharSequence(3, StandardCharsets.US_ASCII))) {
                throw new UnsupportedOperationException("PROXY UNKNOWN not implemented");
            }
            else {
                throw new IllegalStateException("Bad inet protocol: " + first10);
            }
        }
    }

    private static boolean equals(CharSequence c1, CharSequence c2) {
        if (c1.length() == c2.length()) {
            for (int i = 0; i < c1.length(); i++) {
                if (c1.charAt(i) != c2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static void discard(ByteBuf buffer, char expect) {
        if (buffer.readByte() != expect) {
            throw new IllegalStateException("Expected " + Character.getName(expect));
        }
    }

    static boolean is(ByteBuf buffer, char expect) {
        int index = buffer.readerIndex();
        if (buffer.getByte(index) == expect) {
            buffer.readerIndex(index + 1);
            return true;
        }
        else {
            return false;
        }
    }

    static InetAddress readInet4Addr(ByteBuf buffer, byte[] addr, char finalTerminator) {
        addr[0] = toByte(readAsciiDecimal(buffer, '.', 4));
        addr[1] = toByte(readAsciiDecimal(buffer, '.', 4));
        addr[2] = toByte(readAsciiDecimal(buffer, '.', 4));
        addr[3] = toByte(readAsciiDecimal(buffer, finalTerminator, 4));
        try {
            return Inet4Address.getByAddress(addr);
        }
        catch (UnknownHostException e) {
            // Thrown when the array was illegal length, which is impossible here
            throw new AssertionError();
        }
    }

    static InetAddress readInet6Addr(ByteBuf buffer, byte[] addr) {
        int index = 0;
        int pair;
        int dc = -1;
        if (is(buffer, ':')) {
            if (is(buffer, ':')) {
                dc = 0;
                if (is(buffer, ' ')) {
                    index = 16;
                }
            }
            else {
                throw new IllegalStateException("Initial single : in NET6 address");
            }
        }
        while (index < 16) {
            pair = readAsciiHex(buffer);
            addr[index++] = (byte) ((pair & 0xff00) >> 8);
            addr[index++] = (byte) (pair & 0xff);

            if (is(buffer, ':')) {
                if (is(buffer, ':')) {
                    if (dc != -1) {
                        throw new IllegalStateException("Two double colon");
                    }
                    dc = index;
                }
            }
            if (is(buffer, ' ')) {
                break;
            }
        }
        if (dc >= 0) {
            // account for the ::
            for (int j = 14; j > dc; j -= 2) {
                int k = dc + 14 - j;
                addr[j] = addr[k];
                addr[j + 1] = addr[k + 1];
                addr[k] = 0;
                addr[k + 1] = 0;
            }
        }
        else if (index != 16) {
            throw new IllegalStateException("Too few groups of hex digits");
        }
        try {
            return Inet6Address.getByAddress(addr);
        }
        catch (UnknownHostException e) {
            // Thrown when the array was illegal length, which is impossible here
            throw new AssertionError();
        }
    }

    static byte toByte(int num) {
        if (num < 0 || num > 255) {
            throw new IllegalArgumentException();
        }
        return (byte) num;
    }

    /**
     * Read a decimal represented in ASCII characters 0 to 9 from
     * {@code buffer} until {@code terminator} is found.
     * On successful return the buffer's read index will point to after the terminator
     * and more than {@code maxBytes} (which includes the terminator) will have been read.
     * @param buffer
     * @param terminator
     * @param maxBytes
     * @return The number read.
     */
    static int readAsciiDecimal(ByteBuf buffer, char terminator, int maxBytes) {
        int result = 0;
        int i = 0;
        do {
            char ch = (char) buffer.readByte();
            if (ch == terminator) {
                if (i == 0) {
                    throw new IllegalStateException("Expected 1 or more ASCII digits");
                }
                else {
                    return result;
                }
            }
            else if (ch >= '0' && ch <= '9') {
                if (ch == '0' && i == 0) {
                    throw new IllegalStateException("Leading 0 disallowed in TCP4 decimal");
                }
                result *= 10;
                result += (ch - '0');
            }
            else {
                throw new IllegalStateException("Unexpected character: " + Character.getName(ch));
            }
            i++;
        } while (i < maxBytes);
        throw new IllegalStateException("Expected terminator (" + Character.getName(terminator) + ") not found within " + maxBytes + " bytes");
    }

    static int readAsciiHex(ByteBuf buffer) {
        int result = 0;
        int i = 0;
        do {
            char ch = (char) buffer.readByte();
            if (ch >= '0' && ch <= '9') {
                result = result << 4;
                result += (ch - '0');
            }
            else if (ch >= 'a' && ch <= 'f') {
                result = result << 4;
                result += 10 + (ch - 'a');
            }
            else if (ch >= 'A' && ch <= 'F') {
                result = result << 4;
                result += 10 + (ch - 'A');
            }
            else {
                throw new IllegalStateException("Unexpected character: " + Character.getName(ch));
            }
            i++;
        } while (i < 4);
        return result;
    }

    public enum Mode {
        PROXY_V1,
        PROXY_V2,
        AUTO

    }

    // TODO HOW do we expect and guarantee that net filters compose?

    private final Mode mode;

    PROXYNetHandler(Mode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;
            try {
                switch (mode) {
                    case PROXY_V1:
                        proxyV1(ctx, buffer);
                        break;
                    case PROXY_V2:
                        proxyV2(ctx, buffer);
                        break;
                    case AUTO:
                        proxyAutoDetect(ctx, buffer);
                        break;
                }
            }
            catch (Exception e) {
                LOGGER.warn("Invalid PROXY protocol header", e);
                ctx.disconnect();
                return;
            }
            // remove this filter from the pipeline
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.remove(this);
            LOGGER.debug("{}: Revised pipeline: {}", ctx.channel(), pipeline);
            if (buffer.readableBytes() != 0) {
                // propagate remaining bytes
                ctx.fireChannelRead(buffer);
            }
        }
        else {
            throw new IllegalStateException("Expected a ByteBuf");
        }
    }

}
