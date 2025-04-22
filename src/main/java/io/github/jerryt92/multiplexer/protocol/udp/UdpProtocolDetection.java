package io.github.jerryt92.multiplexer.protocol.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

public final class UdpProtocolDetection {
    private static final int MINIMUM_LENGTH = 4;

    public static UdpProtocolType detectProtocol(DatagramPacket in) {
        ByteBuf buffer = in.content();
        if (buffer.readableBytes() < MINIMUM_LENGTH) {
            return UdpProtocolType.UNKNOWN;
        }
        byte b1 = buffer.getByte(0);
        byte b2 = buffer.getByte(1);
        byte b3 = buffer.getByte(2);
        byte b4 = buffer.getByte(3);
        // Check for SNMP
        if ((b1 == 0x30 && b2 == 0x29 && b3 == 0x02 && b4 == 0x01) || (b1 == 0x30 && b2 == 0x3e && b3 == 0x02 && b4 == 0x01)) {
            return UdpProtocolType.SNMP;
        }
        // Check for DNS
//        if (buffer.getUnsignedShort(0) >= 0x0001 && buffer.getUnsignedShort(0) <= 0xFFFF) {
//            // DNS queries and responses have a transaction ID in the first 2 bytes
//            return ProtocolType.DNS;
//        }

        // Add more protocol checks here

        // Unknown protocol
        return UdpProtocolType.SNMP;
    }
}
