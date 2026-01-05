import java.util.zip.ZipEntry
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ZipManager {

    fun unzip(fileZip: File): File {

        val destDir =
            File(fileZip.parent + File.separator + TEMPORARY_UNZIPPED_TEMPLATE_PREFIX + fileZip.nameWithoutExtension)
        Log.d("unzip, destDir : $destDir")
        if (destDir.exists()) {
            Log.d("Temporary unzipped template already exists : ${destDir.absolutePath}, will try to delete.")
            if (destDir.deleteRecursively()) {
                Log.d("Old temporary unzipped template successfully deleted.")
            } else {
                Log.w("Could not delete old temporary unzipped template.")
            }
        }
        val buffer = ByteArray(1024)
        val zis = ZipInputStream(FileInputStream(fileZip))
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            Log.d("zipEntry = $zipEntry")

            val newFile: File = newFile(destDir, zipEntry)
            Log.d("newFile = ${newFile.absolutePath}")

            if (zipEntry.isDirectory) {
                if (!newFile.isDirectory && !newFile.mkdirs()) {
                    throw IOException("Failed to create directory $newFile")
                }
            } else {
                // fix for Windows-created archives
                val parent = newFile.parentFile
                if (!parent.isDirectory && !parent.mkdirs()) {
                    throw IOException("Failed to create directory $parent")
                }

                // write file content
                val fos = FileOutputStream(newFile)
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
        zis.close()
        return destDir
    }

    @Throws(IOException::class)
    fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)
        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.name)
        }
        return destFile
    }
}