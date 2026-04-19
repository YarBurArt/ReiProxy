package io.yarburart.reiproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.github.monkeywie.proxyee.crt.CertUtil

import io.yarburart.reiproxy.ui.screens.CertInfo

import java.util.Date
import java.util.concurrent.TimeUnit

fun generateDefaultCert(): CertInfo {
    val keyPair = CertUtil.genKeyPair()
    val now = Date()
    val expiry = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650))
    val caCert = CertUtil.genCACert(
        "C=CN, ST=GD, L=SZ, O=ReiProxy, OU=Dev, CN=ReiProxy CA",
        now, expiry, keyPair
    )
    return CertInfo(
        subject = CertUtil.getSubject(caCert),
        issuer = caCert.issuerX500Principal.name,
        notBefore = caCert.notBefore,
        notAfter = caCert.notAfter,
        serialNumber = caCert.serialNumber.toString(16).uppercase(),
        certPem = toPem(caCert.encoded, "CERTIFICATE"),
        privateKeyDer = keyPair.private.encoded,
    )
}

private fun toPem(der: ByteArray, label: String): String {
    val base64 = android.util.Base64.encodeToString(der, android.util.Base64.NO_WRAP)
    val body = base64.windowed(64, 64, true).joinToString("\n")
    return "-----BEGIN $label-----\n$body\n-----END $label-----\n"
}

fun exportCert(context: Context) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/x-x509-ca-cert"
        putExtra(Intent.EXTRA_TITLE, "reiproxy_ca.crt")
    }
    try {
        (context as? Activity)?.startActivityForResult(intent, 1001)
        Toast.makeText(
            context, "Select a location to save the certificate",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context, "Could not open file picker: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}