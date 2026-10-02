package fr.poubone.att2.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Discord IPC client (handshake + FRAME send/receive). Local only: named pipe on
 * Windows, Unix domain socket elsewhere. No network, no extra dependency.
 */
final class DiscordIpcClient implements AutoCloseable {
    static final int OP_HANDSHAKE = 0;
    static final int OP_FRAME = 1;
    static final int OP_CLOSE = 2;
    static final int OP_PING = 3;
    static final int OP_PONG = 4;

    private static final int HEADER = 8;
    private static final int MAX_PAYLOAD = 64 * 1024;

    private final Transport transport;

    private DiscordIpcClient(Transport transport) {
        this.transport = transport;
    }

    static DiscordIpcClient connect(String clientId) throws IOException {
        IOException last = null;
        for (String endpoint : endpoints()) {
            try {
                Transport transport = open(endpoint);
                DiscordIpcClient client = new DiscordIpcClient(transport);
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", clientId);
                client.write(OP_HANDSHAKE, handshake.toString());
                JsonObject reply = client.readBlocking(2000);
                if (reply == null) {
                    transport.close();
                    throw new IOException("Discord IPC handshake timed out on " + endpoint);
                }
                return client;
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("Discord IPC endpoint not found");
    }

    void sendFrame(JsonObject payload) throws IOException {
        write(OP_FRAME, payload.toString());
    }

    /** Non-blocking: null when no complete frame is waiting. Answers PING with PONG. */
    JsonObject poll() throws IOException {
        JsonObject frame = readAvailable();
        if (frame == null) return null;
        if ("PING".equals(frame.get("cmd") == null ? "" : frame.get("cmd").getAsString())) {
            write(OP_PONG, frame.toString());
        }
        return frame;
    }

    @Override
    public void close() {
        try {
            write(OP_CLOSE, "{}");
        } catch (IOException ignored) {
        }
        transport.close();
    }

    private void write(int opcode, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(HEADER + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(payload.length);
        buf.put(payload);
        transport.writeFully(buf.array());
    }

    private JsonObject readBlocking(long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            JsonObject frame = readAvailable();
            if (frame != null) return frame;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private JsonObject readAvailable() throws IOException {
        byte[] header = transport.readExactIfAvailable(HEADER);
        if (header == null) return null;
        ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buf.getInt();
        int length = buf.getInt();
        if (length < 0 || length > MAX_PAYLOAD) {
            throw new IOException("Discord IPC payload too large: " + length);
        }
        byte[] payload = transport.readExact(length);
        if (opcode == OP_PING) {
            write(OP_PONG, new String(payload, StandardCharsets.UTF_8));
        }
        if (payload.length == 0) return new JsonObject();
        return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Transport open(String endpoint) throws IOException {
        if (isWindows()) {
            return new PipeTransport(endpoint);
        }
        return new UnixTransport(Path.of(endpoint));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static List<String> endpoints() {
        List<String> list = new ArrayList<>();
        if (isWindows()) {
            for (int i = 0; i < 10; i++) {
                list.add("\\\\.\\pipe\\discord-ipc-" + i);
            }
            return list;
        }
        List<Path> roots = new ArrayList<>();
        String xdg = System.getenv("XDG_RUNTIME_DIR");
        if (xdg != null && !xdg.isBlank()) {
            Path runtime = Path.of(xdg);
            roots.add(runtime);
            roots.add(runtime.resolve("app").resolve("com.discordapp.Discord"));
            roots.add(runtime.resolve("snap.discord"));
        }
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp != null) roots.add(Path.of(tmp));
        roots.add(Path.of("/tmp"));
        for (Path root : roots) {
            for (int i = 0; i < 10; i++) {
                Path path = root.resolve("discord-ipc-" + i);
                if (Files.exists(path)) {
                    list.add(path.toString());
                }
            }
        }
        if (list.isEmpty()) {
            Path fallback = Path.of(xdg != null && !xdg.isBlank() ? xdg : "/tmp").resolve("discord-ipc-0");
            list.add(fallback.toString());
        }
        return list;
    }

    private interface Transport {
        void writeFully(byte[] bytes) throws IOException;

        byte[] readExactIfAvailable(int count) throws IOException;

        byte[] readExact(int count) throws IOException;

        void close();
    }

    private static final class UnixTransport implements Transport {
        private final SocketChannel channel;

        UnixTransport(Path path) throws IOException {
            SocketChannel open = SocketChannel.open(StandardProtocolFamily.UNIX);
            open.configureBlocking(false);
            open.connect(UnixDomainSocketAddress.of(path));
            if (!open.finishConnect()) {
                open.close();
                throw new IOException("Unix Discord IPC connect failed: " + path);
            }
            this.channel = open;
        }

        @Override
        public void writeFully(byte[] bytes) throws IOException {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            while (buf.hasRemaining()) {
                int n = channel.write(buf);
                if (n < 0) throw new IOException("Discord IPC socket closed");
                if (n == 0) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while writing Discord IPC", e);
                    }
                }
            }
        }

        @Override
        public byte[] readExactIfAvailable(int count) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(count);
            int n = channel.read(buf);
            if (n < 0) throw new IOException("Discord IPC socket closed");
            if (n == 0) return null;
            while (buf.hasRemaining()) {
                int more = channel.read(buf);
                if (more < 0) throw new IOException("Discord IPC socket closed");
                if (more == 0) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while reading Discord IPC", e);
                    }
                }
            }
            return buf.array();
        }

        @Override
        public byte[] readExact(int count) throws IOException {
            byte[] data = readExactIfAvailable(count);
            if (data != null) return data;
            long deadline = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < deadline) {
                data = readExactIfAvailable(count);
                if (data != null) return data;
            }
            throw new IOException("Timed out reading Discord IPC payload");
        }

        @Override
        public void close() {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static final class PipeTransport implements Transport {
        private final RandomAccessFile pipe;

        PipeTransport(String name) throws IOException {
            this.pipe = new RandomAccessFile(name, "rw");
        }

        @Override
        public void writeFully(byte[] bytes) throws IOException {
            pipe.write(bytes);
        }

        @Override
        public byte[] readExactIfAvailable(int count) throws IOException {
            if (pipe.length() < count) return null;
            byte[] data = new byte[count];
            pipe.readFully(data);
            return data;
        }

        @Override
        public byte[] readExact(int count) throws IOException {
            long deadline = System.currentTimeMillis() + 2000;
            while (pipe.length() < count) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IOException("Timed out reading Discord IPC pipe");
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading Discord IPC", e);
                }
            }
            byte[] data = new byte[count];
            pipe.readFully(data);
            return data;
        }

        @Override
        public void close() {
            try {
                pipe.close();
            } catch (IOException ignored) {
            }
        }
    }
}
