package chat.stoat.providers

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.api.StoatHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import java.io.File

class AttachmentProvider : FileProvider(R.xml.file_paths)

suspend fun getAttachmentContentUri(
    context: Context,
    resourceUrl: String,
    id: String,
    filename: String
): Uri {
    val attachmentsDir = File(context.cacheDir, "attachments")
    if (!attachmentsDir.exists()) {
        attachmentsDir.mkdir()
    }

    val response = StoatHttp.get(resourceUrl)
    val file = File(attachmentsDir, "$id-$filename")
    file.writeBytes(response.readBytes())

    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file
    )
}
