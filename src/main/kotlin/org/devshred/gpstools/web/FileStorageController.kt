package org.devshred.gpstools.web

import jakarta.servlet.http.HttpServletRequest
import org.devshred.gpstools.domain.FileStore
import org.devshred.gpstools.domain.GpxService
import org.devshred.gpstools.domain.IOService
import org.devshred.gpstools.domain.StoredFile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.io.IOException
import java.util.UUID

private const val ERROR_MSG_NO_GPS_FILE = "Not a file format to hold GPS data."

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class FileStorageController(
    private val store: FileStore,
    private val ioService: IOService,
    private val gpxService: GpxService,
) {
    @GetMapping("/files/{id}")
    @Throws(IOException::class)
    fun download(
        @PathVariable id: String,
        @RequestParam("m", required = false) mode: String?,
        @RequestParam("name", required = false) name: String?,
        @RequestHeader headers: Map<String, String>,
    ): ResponseEntity<Resource> {
        val storedFile = store.get(UUID.fromString(id))

        val inputStream = gpxService.protoFileToGpxInputStream(storedFile.storageLocation, name)
        val inputStreamResource = InputStreamResource(inputStream)

        val responseHeaders = HttpHeaders()
        if (StringUtils.hasText(mode) && mode == "dl") {
            val file = File(storedFile.filename)
            val filename = file.nameWithoutExtension.onlyAlphanumericChars()
                .ifBlank { "unnamed" }
            val extension = file.extension
            responseHeaders.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$filename.$extension\""
            )
        }

        return ResponseEntity.ok() //
            .contentType(MediaType.valueOf(storedFile.mimeType)) //
            .headers(responseHeaders)
            .body(inputStreamResource)
    }

    @ResponseBody
    @PostMapping(path = ["/file"])
    @Throws(IOException::class)
    fun uploadFile(
        request: HttpServletRequest,
        @RequestParam filename: String,
    ): ResponseEntity<StoredFile> {
        if (!isGpsFile(filename)) {
            throw IllegalArgumentException(ERROR_MSG_NO_GPS_FILE)
        }

        val uploadedFile: StoredFile = ioService.createTempFile(request.inputStream, filename)

        if (uploadedFile.mimeType != org.apache.tika.mime.MediaType.APPLICATION_XML.toString()) {
            ioService.delete(storageLocation = uploadedFile.storageLocation)
            throw IllegalArgumentException(ERROR_MSG_NO_GPS_FILE)
        }

        try {
            val inputStream = gpxService.protoInputStreamFromFileLocation(uploadedFile.storageLocation)
            val protoFile = ioService.createTempFile(inputStream, filename)
            store.put(protoFile.id, protoFile)

            ioService.delete(uploadedFile.storageLocation)
            return ResponseEntity.ok(protoFile)
        } catch (ex: IOException) {
            ioService.delete(storageLocation = uploadedFile.storageLocation)
            throw IllegalArgumentException(ERROR_MSG_NO_GPS_FILE)
        }
    }

    @ResponseBody
    @PostMapping(
        path = ["/files"],
    )
    @Throws(IOException::class)
    fun uploadFiles(
        @RequestParam file: List<MultipartFile>,
    ): ResponseEntity<List<StoredFile>> {
        val results = ArrayList<StoredFile>()
        file
            .filter { isGpsFile(it.originalFilename!!) }
            .forEach {
                val uploadedFile: StoredFile = ioService.createTempFile(it.inputStream, it.originalFilename!!)

                try {
                    if (uploadedFile.mimeType != org.apache.tika.mime.MediaType.APPLICATION_XML.toString()) {
                        ioService.delete(storageLocation = uploadedFile.storageLocation)
                        throw IllegalArgumentException(ERROR_MSG_NO_GPS_FILE)
                    }

                    val inputStream = gpxService.protoInputStreamFromFileLocation(uploadedFile.storageLocation)
                    val protoFile = ioService.createTempFile(inputStream, it.originalFilename!!)
                    store.put(protoFile.id, protoFile)
                    ioService.delete(uploadedFile.storageLocation)
                    results.add(protoFile)
                } catch (ex: IOException) {
                    ioService.delete(storageLocation = uploadedFile.storageLocation)
                    throw IllegalArgumentException(ERROR_MSG_NO_GPS_FILE)
                }
            }
        if (results.isEmpty()) {
            throw IllegalArgumentException("Failed to upload files.")
        }
        return ResponseEntity.ok(results)
    }

    @DeleteMapping("/files/{id}")
    @Throws(IOException::class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: String,
    ) {
        store.delete(UUID.fromString(id))?.let {
            ioService.delete(it.storageLocation)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}

private fun isGpsFile(filename: String) = filename.endsWith(".gpx")

fun String.onlyAlphanumericChars() =
    this.asSequence().filter { it.isLetterOrDigit() }.joinToString("")
