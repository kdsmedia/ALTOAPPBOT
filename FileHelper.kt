import org.json.JSONObject
import java.io.File

class FileHelper(val pathHelper: PathHelper) {

    /**
     * CREATE COM.COMPANY.EXAMPLE PATH DIRECTORIES
     */
    fun createPathDirectories() {
        for (dir in kotlinProjectDirs) {
            val path = pathHelper.getTargetPath(dir)
            val directories = File(pathHelper.replaceDirectoriesPath(path))
            directories.mkdirs()
        }
    }

    /**
     *  COPY NON MODIFIED FILES
     */
    fun copyFiles() {
        val sourceFolder = File(pathHelper.filesToCopy)
        val targetFolder = File(pathHelper.targetDirPath)
        if (targetFolder.exists()) {
            Log.d("SDK files already exist in target path ${pathHelper.targetDirPath}, will try to delete previous build files.")
            if (targetFolder.deleteRecursively()) {
                Log.d("Old project files successfully deleted.")
            } else {
                Log.w("Could not delete old project files.")
            }
        }
        targetFolder.mkdir()
        if (!sourceFolder.copyRecursively(target = targetFolder, overwrite = true)) {
            throw Exception("An error occurred while copying files with target folder : ${targetFolder.absolutePath}")
        }

        renameTxtXmlFiles(targetFolder)
    }

    private fun renameTxtXmlFiles(targetFolder: File) {
        targetFolder.walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }
            .forEach { currentFolder ->

                currentFolder.walkTopDown()
                    .filter { file -> !file.isHidden && file.isFile && file.absolutePath.endsWith(XML_TXT_EXT) }
                    .forEach { currentTxtXmlFile ->
                        val newFile = File(currentTxtXmlFile.absolutePath.replaceXmlTxtSuffix())
                        currentTxtXmlFile.renameTo(newFile)
                    }
            }
    }
}

fun getDumpedInfoFile(assetsPath: String): File {
    val projectDataSetAndroidPath = getProjectDataSetAndroidPath(assetsPath)
    val dumpInfoFile = File(projectDataSetAndroidPath + File.separator + "dump_info.json")

    if (dumpInfoFile.exists()) {
        dumpInfoFile.delete()
    }
    dumpInfoFile.parentFile.mkdirs()
    if (!dumpInfoFile.createNewFile()) {
        throw Exception("An error occurred while creating new file : $dumpInfoFile")
    }
    return dumpInfoFile
}

fun getProjectDataSetAndroidPath(assetsPath: String): String {
    val projectDataSetDirPath = File(assetsPath).parentFile.parent
    return projectDataSetDirPath + File.separator + "android"
}

fun getDataPath(assetsPath: String, tableName: String, index: Int? = null): String {
    val path = assetsPath + File.separator + DATA_PATH_KEY + File.separator +
            "$tableName.$DATA_DATASET_SUFFIX" + File.separator +
            "$tableName."
    return if (index == null) {
        path + DATA_JSON_SUFFIX
    } else {
        "$path$index.$DATA_JSON_SUFFIX"
    }
}

fun File.isWithTemplateName() = this.name.contains(TEMPLATE_PLACEHOLDER)

fun File.isActionFromNavBarTemplate() = this.name.contains("$ACTION_FROM_NAV_BAR_KEY$TEMPLATE_PLACEHOLDER")

fun File.readFile(): String {
    return this.bufferedReader().use {
        it.readText()
    }
}

// Used for both custom templates and custom formatters
fun getManifest(path: String): File = File(path + File.separator + "manifest.json")

// Used for both custom templates and custom formatters
fun getManifestJSONContent(path: String): JSONObject? {
    Log.d("getManifestJSONContent: $path")
    val manifest = getManifest(path)
    return getManifestJSONContent(manifest)
}

fun getManifestJSONContent(manifestFile: File): JSONObject? {
    return if (manifestFile.exists()) {
        val jsonString = manifestFile.readFile()
        retrieveJSONObject(jsonString)
    } else
        null
}

fun imageExistsInFormatter(path: String, imageName: String): Boolean =
    File(path + File.separator + "Images" + File.separator + imageName).exists()

fun imageExistsInFormatterInDarkMode(path: String, imageName: String): Boolean {
    val file = File(path + File.separator + "Images" + File.separator + imageName)
    val name = file.nameWithoutExtension + "_dark." + file.extension
    val darkModeFile = File(path + File.separator + "Images" + File.separator + name)
    return darkModeFile.exists()
}
