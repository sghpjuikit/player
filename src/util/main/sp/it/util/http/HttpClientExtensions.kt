package sp.it.util.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.discardRemaining
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import java.io.File
import java.net.URI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import sp.it.util.async.coroutine.VT
import sp.it.util.dev.fail
import sp.it.util.file.tryDeleteIfExists
import sp.it.util.functional.net

typealias DownloadState = Double?

suspend fun HttpClient.downloadFile(srcUrl: String, file: File): Flow<DownloadState> = flow {
   val dir = file.parentFile ?: fail { "File $file has no parent directory" }
   val url = URI(srcUrl).toURL()

   withContext(VT) {
      if (file.exists()) file.tryDeleteIfExists { "Deleting file $file failed" }.orThrow
      if (!dir.exists()) dir.mkdirs()
      if (!dir.exists()) fail { "Unable to create directory $dir" }
   }

   prepareGet(url) {
      timeout { requestTimeoutMillis = null }
   }.execute { response ->
      if (response.status.isSuccess()) {
         val lengthTotal = response.contentLength()?.toDouble()
         var lengthRead = 0
         val byteArray = ByteArray(1024)
         val dataIn = response.bodyAsChannel()

         file.writeChannel().use {
            do {
               val p = lengthTotal?.net { lengthRead.toDouble()/it }
               if (p!=1.0) emit(p)
               val read = dataIn.readAvailable(byteArray, 0, byteArray.size)  // or even more effectively https://ktor.io/docs/response.html#streaming
               writeFully(byteArray, 0, read)
               lengthRead += read
            } while (read>0)
         }

         emit(1.0)
      } else {
         response.discardRemaining()
         fail { "File $srcUrl not downloaded, http status=${response.status}" }
      }
   }
}