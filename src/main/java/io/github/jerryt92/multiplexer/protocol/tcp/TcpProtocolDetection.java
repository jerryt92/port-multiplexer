package io.github.jerryt92.multiplexer.protocol.tcp;

import io.netty.buffer.ByteBuf;

public final class TcpProtocolDetection {
    private static final int MINIMUM_LENGTH = 4;

    public static TcpProtocolType detectProtocol(ByteBuf in) {
        if (in.readableBytes() < MINIMUM_LENGTH) {
            return TcpProtocolType.UNKNOWN;
        }
        in.markReaderIndex();
        byte[] initialBytes = new byte[MINIMUM_LENGTH];
        in.readBytes(initialBytes);
        in.resetReaderIndex();

        // Check for SSL/TLS first to avoid overlap with MQTT
        if (initialBytes[0] == 0x16 && initialBytes[1] == 0x03 && (initialBytes[2] >= 0x00 && initialBytes[2] <= 0x03)) {
            return TcpProtocolType.SSL_TLS;
        }

        // Check for HTTP
        if ((initialBytes[0] == 'G' && initialBytes[1] == 'E' && initialBytes[2] == 'T') || // GET
                (initialBytes[0] == 'P' && initialBytes[1] == 'O' && initialBytes[2] == 'S' && initialBytes[3] == 'T') || // POST
                (initialBytes[0] == 'H' && initialBytes[1] == 'T' && initialBytes[2] == 'T' && initialBytes[3] == 'P') || // HTTP
                (initialBytes[0] == 'P' && initialBytes[1] == 'U' && initialBytes[2] == 'T') || // PUT
                (initialBytes[0] == 'D' && initialBytes[1] == 'E' && initialBytes[2] == 'L') || // DELETE
                (initialBytes[0] == 'H' && initialBytes[1] == 'E' && initialBytes[2] == 'A' && initialBytes[3] == 'D') || // HEAD
                (initialBytes[0] == 'O' && initialBytes[1] == 'P' && initialBytes[2] == 'T' && initialBytes[3] == 'I') || // OPTIONS
                (initialBytes[0] == 'T' && initialBytes[1] == 'R' && initialBytes[2] == 'A' && initialBytes[3] == 'C') || // TRACE
                (initialBytes[0] == 'C' && initialBytes[1] == 'O' && initialBytes[2] == 'N' && initialBytes[3] == 'N')) { // CONNECT
            return TcpProtocolType.HTTP;
        }

        // Check for MQTT
        if ((initialBytes[0] & 0xF0) == 0x10) {
            return TcpProtocolType.MQTT;
        }

        // Check for SSH
        if (initialBytes[0] == 'S' && initialBytes[1] == 'S' && initialBytes[2] == 'H' && initialBytes[3] == '-') {
            return TcpProtocolType.SSH;
        }

        // Check for WebSocket
        if ((initialBytes[0] & 0x80) == 0x80 && (initialBytes[1] & 0x80) == 0x80) {
            return TcpProtocolType.WEBSOCKET;
        }

        // Check for RDP (TPKT Header)
        // Byte 0: Version 3 (0x03)
        // Byte 1: Reserved 0 (0x00)
        // Byte 2-3: Length (Must be >= 4, usually header is 4 bytes)
        if (initialBytes[0] == 0x03 && initialBytes[1] == 0x00) {
            // 进一步计算长度是否合法，防止误判
            int length = (initialBytes[2] & 0xFF) << 8 | (initialBytes[3] & 0xFF);
            if (length >= 4) {
                return TcpProtocolType.RDP;
            }
        }

        // Unknown protocol
        return TcpProtocolType.UNKNOWN;
    }
}