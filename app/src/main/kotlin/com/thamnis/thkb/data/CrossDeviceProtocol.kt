package com.thamnis.thkb.data

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class PeerProtocol(val label: String) {
    LOCALSEND("LocalSend v2"),
    KDE_CONNECT("KDE Connect"),
    THKB_REMOTE("th-kb Remote"),
}

data class CrossDevicePeer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val protocol: PeerProtocol,
    val transportScheme: String = "http",
    val fingerprint: String = "",
    val deviceType: String = "desktop",
)

sealed class TransferResult {
    data class Success(val message: String) : TransferResult()
    data class Failure(val error: String) : TransferResult()
}

object CrossDeviceProtocol {
    const val LOCALSEND_MULTICAST_GROUP = "224.0.0.167"
    const val LOCALSEND_PORT = 53317
    const val KDE_CONNECT_PORT = 1716
    const val THKB_REMOTE_PORT = 55677
    const val THKB_DISCOVERY_PORT = 55678

    private val localFingerprint: String by lazy {
        UUID.randomUUID().toString().replace("-", "")
    }

    /** Discovers LocalSend v2, KDE Connect, and th-kb Remote Companion peers on the local Wi-Fi LAN. */
    suspend fun discoverAllPeers(
        context: Context,
        durationMs: Long = 1600L,
    ): List<CrossDevicePeer> = withContext(Dispatchers.IO) {
        val discovered = linkedMapOf<String, CrossDevicePeer>()
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = runCatching {
            wifi?.createMulticastLock("thkb_lan_discovery")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()

        try {
            // 1. LocalSend v2 UDP multicast discovery
            runCatching {
                MulticastSocket(null).use { sock ->
                    sock.reuseAddress = true
                    sock.soTimeout = 350
                    sock.bind(InetSocketAddress(0))
                    val group = InetAddress.getByName(LOCALSEND_MULTICAST_GROUP)
                    val announceJson = JSONObject().apply {
                        put("alias", Build.MODEL ?: "th-kb Android")
                        put("version", "2.1")
                        put("deviceModel", Build.MODEL ?: "Android")
                        put("deviceType", "mobile")
                        put("fingerprint", localFingerprint)
                        put("port", LOCALSEND_PORT)
                        put("protocol", "http")
                        put("download", false)
                        put("announce", true)
                    }.toString().toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(announceJson, announceJson.size, group, LOCALSEND_PORT)
                    sock.send(packet)

                    val deadline = System.currentTimeMillis() + (durationMs / 2)
                    val buf = ByteArray(8192)
                    while (System.currentTimeMillis() < deadline) {
                        val recv = DatagramPacket(buf, buf.size)
                        try {
                            sock.receive(recv)
                            val body = String(recv.data, 0, recv.length, Charsets.UTF_8)
                            val obj = JSONObject(body)
                            val fp = obj.optString("fingerprint", "")
                            if (fp.isNotEmpty() && fp != localFingerprint) {
                                val ip = recv.address.hostAddress ?: continue
                                val port = obj.optInt("port", LOCALSEND_PORT)
                                val alias = obj.optString("alias", obj.optString("deviceModel", ip))
                                val proto = obj.optString("protocol", "https")
                                val devType = obj.optString("deviceType", "desktop")
                                val peer = CrossDevicePeer(
                                    id = "ls:$fp",
                                    name = alias,
                                    ip = ip,
                                    port = port,
                                    protocol = PeerProtocol.LOCALSEND,
                                    transportScheme = proto,
                                    fingerprint = fp,
                                    deviceType = devType,
                                )
                                discovered[peer.id] = peer
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            }

            // 2. KDE Connect UDP broadcast identity on port 1716
            runCatching {
                DatagramSocket(0).use { sock ->
                    sock.broadcast = true
                    sock.soTimeout = 350
                    val identity = JSONObject().apply {
                        put("id", System.currentTimeMillis())
                        put("type", "kdeconnect.identity")
                        put(
                            "body",
                            JSONObject().apply {
                                put("deviceId", "thkb_${localFingerprint.take(16)}")
                                put("deviceName", Build.MODEL ?: "th-kb Android")
                                put("deviceType", "phone")
                                put("protocolVersion", 7)
                                put("tcpPort", KDE_CONNECT_PORT)
                                put("incomingCapabilities", JSONArray().put("kdeconnect.share.request"))
                                put("outgoingCapabilities", JSONArray().put("kdeconnect.share.request"))
                            },
                        )
                    }.toString().let { "$it\n".toByteArray(Charsets.UTF_8) }

                    val broadcastAddr = InetAddress.getByName("255.255.255.255")
                    sock.send(DatagramPacket(identity, identity.size, broadcastAddr, KDE_CONNECT_PORT))

                    val deadline = System.currentTimeMillis() + (durationMs / 3)
                    val buf = ByteArray(8192)
                    while (System.currentTimeMillis() < deadline) {
                        val recv = DatagramPacket(buf, buf.size)
                        try {
                            sock.receive(recv)
                            val raw = String(recv.data, 0, recv.length, Charsets.UTF_8).trim()
                            val obj = JSONObject(raw)
                            if (obj.optString("type") == "kdeconnect.identity") {
                                val b = obj.optJSONObject("body") ?: continue
                                val devId = b.optString("deviceId", "")
                                if (devId.isEmpty() || devId.startsWith("thkb_")) continue
                                val ip = recv.address.hostAddress ?: continue
                                val tcpPort = b.optInt("tcpPort", KDE_CONNECT_PORT)
                                val name = b.optString("deviceName", ip)
                                val devType = b.optString("deviceType", "desktop")
                                val peer = CrossDevicePeer(
                                    id = "kde:$devId",
                                    name = name,
                                    ip = ip,
                                    port = tcpPort,
                                    protocol = PeerProtocol.KDE_CONNECT,
                                    deviceType = devType,
                                )
                                discovered[peer.id] = peer
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            }

            // 3. th-kb Remote Desktop Companion UDP discovery on port 55678 (broadcast + subnet broadcast + multicast + localhost)
            runCatching {
                DatagramSocket(0).use { sock ->
                    sock.broadcast = true
                    sock.soTimeout = 450
                    val probe = "THKB_DISCOVER".toByteArray(Charsets.UTF_8)
                    val targets = linkedSetOf<InetAddress>(
                        InetAddress.getByName("255.255.255.255"),
                        InetAddress.getByName(LOCALSEND_MULTICAST_GROUP),
                        InetAddress.getByName("127.0.0.1"),
                    )
                    runCatching {
                        val ifaces = NetworkInterface.getNetworkInterfaces()
                        while (ifaces.hasMoreElements()) {
                            val iface = ifaces.nextElement()
                            if (iface.isUp && !iface.isLoopback) {
                                for (ia in iface.interfaceAddresses) {
                                    ia.broadcast?.let { targets.add(it) }
                                }
                            }
                        }
                    }
                    for (target in targets) {
                        runCatching {
                            sock.send(DatagramPacket(probe, probe.size, target, THKB_DISCOVERY_PORT))
                        }
                    }
                    val deadline = System.currentTimeMillis() + (durationMs / 2)
                    val buf = ByteArray(2048)
                    while (System.currentTimeMillis() < deadline) {
                        val recv = DatagramPacket(buf, buf.size)
                        try {
                            sock.receive(recv)
                            val msg = String(recv.data, 0, recv.length, Charsets.UTF_8).trim()
                            if (msg.startsWith("THKB_COMPANION:")) {
                                val parts = msg.split(":")
                                val hostName = parts.getOrNull(1).orEmpty().ifBlank {
                                    recv.address.hostAddress ?: "Desktop"
                                }
                                val port = parts.getOrNull(2)?.toIntOrNull() ?: THKB_REMOTE_PORT
                                val ip = recv.address.hostAddress ?: continue
                                val peer = CrossDevicePeer(
                                    id = "thkb:$ip:$port",
                                    name = hostName,
                                    ip = ip,
                                    port = port,
                                    protocol = PeerProtocol.THKB_REMOTE,
                                )
                                discovered[peer.id] = peer
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            }
        } finally {
            runCatching {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            }
        }

        discovered.values.toList()
    }

    /** Checks reachability of a th-kb companion host:port via UDP Ping and returns its hostname if alive. */
    suspend fun pingRemoteCompanion(host: String, port: Int): String? = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) return@withContext null
        runCatching {
            DatagramSocket(0).use { sock ->
                sock.soTimeout = 450
                val payload = """{"t":"ping"}""".toByteArray(Charsets.UTF_8)
                val addr = InetAddress.getByName(cleanHost)
                sock.send(DatagramPacket(payload, payload.size, addr, port))
                val buf = ByteArray(512)
                val recv = DatagramPacket(buf, buf.size)
                sock.receive(recv)
                val msg = String(recv.data, 0, recv.length, Charsets.UTF_8).trim()
                if (msg.startsWith("THKB_PONG:")) msg.removePrefix("THKB_PONG:").ifBlank { cleanHost } else null
            }
        }.getOrNull()
    }

    /** Returns the device's primary local LAN IPv4 address for display in pairing guides. */
    fun getLocalIpv4Address(): String? = runCatching {
        val ifaces = NetworkInterface.getNetworkInterfaces()
        while (ifaces.hasMoreElements()) {
            val iface = ifaces.nextElement()
            if (!iface.isUp || iface.isLoopback) continue
            val addrs = iface.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    return@runCatching addr.hostAddress
                }
            }
        }
        null
    }.getOrNull()

    /** Sends a file over LocalSend v2 or KDE Connect LAN protocol. */
    suspend fun sendFile(
        context: Context,
        peer: CrossDevicePeer,
        fileUri: Uri,
    ): TransferResult = withContext(Dispatchers.IO) {
        when (peer.protocol) {
            PeerProtocol.LOCALSEND -> sendViaLocalSendV2(context, peer, fileUri)
            PeerProtocol.KDE_CONNECT -> sendViaKdeConnect(context, peer, fileUri)
            PeerProtocol.THKB_REMOTE -> TransferResult.Failure("Selected peer is Remote Input only")
        }
    }

    private fun resolveFileMeta(context: Context, uri: Uri): Triple<String, Long, String> {
        val resolver = context.contentResolver
        var fileName = "shared_file"
        var fileSize = -1L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
            }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        if (fileSize <= 0L) {
            fileSize = resolver.openInputStream(uri)?.use { it.readBytes().size.toLong() } ?: 0L
        }
        return Triple(fileName, fileSize, mime)
    }

    private fun openPeerConnection(url: URL, peer: CrossDevicePeer): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        if (conn is HttpsURLConnection && peer.fingerprint.isNotEmpty()) {
            // Pin TLS certificate SHA-256 against the announced LocalSend v2 peer fingerprint
            val expectedHex = peer.fingerprint.lowercase().replace(":", "")
            val tm = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    val cert = chain?.firstOrNull() ?: throw CertificateException("Missing peer certificate")
                    val sha = MessageDigest.getInstance("SHA-256")
                        .digest(cert.encoded)
                        .joinToString("") { "%02x".format(it) }
                    if (expectedHex.length == 64 && sha != expectedHex) {
                        throw CertificateException("LocalSend fingerprint mismatch")
                    }
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val ssl = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(tm), SecureRandom())
            }
            conn.sslSocketFactory = ssl.socketFactory
            conn.setHostnameVerifier { _, _ -> true }
        }
        conn.connectTimeout = 8000
        conn.readTimeout = 20000
        return conn
    }

    private fun sendViaLocalSendV2(
        context: Context,
        peer: CrossDevicePeer,
        uri: Uri,
    ): TransferResult = runCatching {
        val (fileName, fileSize, mimeType) = resolveFileMeta(context, uri)
        val fileId = "f_${UUID.randomUUID().toString().take(8)}"
        val scheme = if (peer.transportScheme == "https") "https" else "http"
        val baseUrl = "$scheme://${peer.ip}:${peer.port}/api/localsend/v2"

        val prepareBody = JSONObject().apply {
            put(
                "info",
                JSONObject().apply {
                    put("alias", Build.MODEL ?: "th-kb Android")
                    put("version", "2.1")
                    put("deviceModel", Build.MODEL ?: "Android")
                    put("deviceType", "mobile")
                    put("fingerprint", localFingerprint)
                    put("port", LOCALSEND_PORT)
                    put("protocol", scheme)
                    put("download", false)
                },
            )
            put(
                "files",
                JSONObject().apply {
                    put(
                        fileId,
                        JSONObject().apply {
                            put("id", fileId)
                            put("fileName", fileName)
                            put("size", fileSize)
                            put("fileType", mimeType)
                        },
                    )
                },
            )
        }.toString()

        val prepareConn = openPeerConnection(URL("$baseUrl/prepare-upload"), peer).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        prepareConn.outputStream.use { it.write(prepareBody.toByteArray(Charsets.UTF_8)) }

        val prepareCode = prepareConn.responseCode
        if (prepareCode !in 200..299) {
            return TransferResult.Failure("Peer rejected prepare-upload (HTTP $prepareCode)")
        }
        val prepareResp = JSONObject(prepareConn.inputStream.bufferedReader().use { it.readText() })
        val sessionId = prepareResp.getString("sessionId")
        val tokensObj = prepareResp.getJSONObject("files")
        val token = tokensObj.getString(fileId)

        val uploadUrl = "$baseUrl/upload?sessionId=${URLEncoder.encode(sessionId, "UTF-8")}" +
            "&fileId=${URLEncoder.encode(fileId, "UTF-8")}" +
            "&token=${URLEncoder.encode(token, "UTF-8")}"
        val uploadConn = openPeerConnection(URL(uploadUrl), peer).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/octet-stream")
            if (fileSize > 0) setFixedLengthStreamingMode(fileSize)
        }
        uploadConn.outputStream.use { out ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(out, bufferSize = 16384)
            } ?: return TransferResult.Failure("Cannot read selected file")
            out.flush()
        }

        val uploadCode = uploadConn.responseCode
        if (uploadCode in 200..299) {
            TransferResult.Success("Sent $fileName (${fileSize / 1024} KB) to ${peer.name} via LocalSend")
        } else {
            TransferResult.Failure("Upload failed (HTTP $uploadCode)")
        }
    }.getOrElse { err ->
        TransferResult.Failure("LocalSend error: ${err.message ?: err.javaClass.simpleName}")
    }

    private fun sendViaKdeConnect(
        context: Context,
        peer: CrossDevicePeer,
        uri: Uri,
    ): TransferResult = runCatching {
        val (fileName, fileSize, _) = resolveFileMeta(context, uri)
        val server = ServerSocket(0)
        val payloadPort = server.localPort
        server.soTimeout = 15000

        // Background accept thread for KDE Connect payload stream
        var serverError: Throwable? = null
        val acceptThread = Thread {
            runCatching {
                server.use { srv ->
                    srv.accept().use { dataSocket ->
                        dataSocket.getOutputStream().use { out ->
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                input.copyTo(out, bufferSize = 16384)
                            }
                            out.flush()
                        }
                    }
                }
            }.onFailure { serverError = it }
        }.apply { start() }

        Socket().use { tcp ->
            tcp.connect(InetSocketAddress(peer.ip, peer.port), 6000)
            tcp.soTimeout = 8000
            val out: OutputStream = tcp.getOutputStream()
            val identity = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("type", "kdeconnect.identity")
                put(
                    "body",
                    JSONObject().apply {
                        put("deviceId", "thkb_${localFingerprint.take(16)}")
                        put("deviceName", Build.MODEL ?: "th-kb Android")
                        put("deviceType", "phone")
                        put("protocolVersion", 7)
                    },
                )
            }.toString() + "\n"
            out.write(identity.toByteArray(Charsets.UTF_8))

            val sharePacket = JSONObject().apply {
                put("id", System.currentTimeMillis() + 1)
                put("type", "kdeconnect.share.request")
                put(
                    "body",
                    JSONObject().apply {
                        put("filename", fileName)
                        put("open", false)
                    },
                )
                put("payloadSize", fileSize)
                put("payloadTransferInfo", JSONObject().apply { put("port", payloadPort) })
            }.toString() + "\n"
            out.write(sharePacket.toByteArray(Charsets.UTF_8))
            out.flush()

            // Wait briefly for peer to connect back & pull file payload
            acceptThread.join(14000)
        }

        val err = serverError
        if (err != null) {
            TransferResult.Failure("KDE Connect transfer error: ${err.message}")
        } else {
            TransferResult.Success("Dispatched $fileName to ${peer.name} via KDE Connect")
        }
    }.getOrElse { err ->
        TransferResult.Failure("KDE Connect error: ${err.message ?: err.javaClass.simpleName}")
    }
}

object RemoteInputClient {
    fun sendCommandAsync(host: String, port: Int, payload: JSONObject) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) return
        Thread {
            runCatching {
                DatagramSocket().use { sock ->
                    val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                    val addr = InetAddress.getByName(cleanHost)
                    sock.send(DatagramPacket(bytes, bytes.size, addr, port))
                }
            }
        }.start()
    }

    fun sendMove(host: String, port: Int, dx: Float, dy: Float) {
        sendCommandAsync(
            host,
            port,
            JSONObject().apply {
                put("t", "move")
                put("dx", dx.toDouble())
                put("dy", dy.toDouble())
            },
        )
    }

    fun sendClick(host: String, port: Int, button: String = "left") {
        sendCommandAsync(
            host,
            port,
            JSONObject().apply {
                put("t", "click")
                put("btn", button)
            },
        )
    }

    fun sendScroll(host: String, port: Int, dy: Float) {
        sendCommandAsync(
            host,
            port,
            JSONObject().apply {
                put("t", "scroll")
                put("dy", dy.toDouble())
            },
        )
    }

    fun sendText(host: String, port: Int, text: String) {
        if (text.isEmpty()) return
        sendCommandAsync(
            host,
            port,
            JSONObject().apply {
                put("t", "text")
                put("text", text)
            },
        )
    }

    fun sendSpecialKey(host: String, port: Int, key: String) {
        sendCommandAsync(
            host,
            port,
            JSONObject().apply {
                put("t", "key")
                put("key", key)
            },
        )
    }
}
