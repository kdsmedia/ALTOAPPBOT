import java.io.File
import java.lang.IllegalArgumentException

class PathHelper(
    val targetDirPath: String,
    val templateFilesPath: String,
    val templateFormsPath: String,
    val hostDb: String,
    val filesToCopy: String,
    val companyWithCaps: String,
    val appNameWithCaps: String,
    val pkg: String
) {

    private val tmpUnzippedTemplateListToBeDeleted: MutableList<File> = mutableListOf()

    fun getPath(currentPath: String): String {
        val path = targetDirPath + replacePath(currentPath)
        return path.replaceIfWindowsPath()
    }

    fun getLayoutTemplatePath(currentPath: String, formPath: String): String {
        val path = targetDirPath + replaceLayoutTemplatePath(currentPath, formPath)
        return path.replaceIfWindowsPath()
    }

    fun replaceDirectoriesPath(path: String): String {
        return path.replace(PACKAGE_PH, pkg.replace(".", File.separator))
            .replace(PACKAGE_JOINED_PH, pkg) // for buildSrc
    }

    private fun replacePath(currentPath: String): String {
        val paths = currentPath.replaceIfWindowsPath().split(templateFilesPath)
        if (paths.size < 2) {
            throw Exception("Couldn't find target directory with path : $currentPath")
        }
        return replaceDirectoriesPath(paths[1])
    }

    private fun replaceLayoutTemplatePath(currentPath: String, formPath: String): String {
        val paths = currentPath.replaceIfWindowsPath().split(formPath.replaceIfWindowsPath())
        if (paths.size < 2) {
            throw Exception("Couldn't find target directory with path : $currentPath")
        }
        val subPath = paths[1].removePrefix("/").removeSuffix("\\").removePrefix(ANDROID_PATH_KEY)
        Log.d("replaceLayoutTemplatePath, subPath = $subPath")
        return replaceDirectoriesPath(subPath)
    }

    private val listFormTemplatesPath = templateFormsPath + File.separator + LIST_FORMS_KEY

    private val detailFormTemplatesPath = templateFormsPath + File.separator + DETAIL_FORMS_KEY

    private val hostFormTemplatesPath = hostDb + File.separator + HOST_FORMS

    private val hostListFormTemplatesPath = hostFormTemplatesPath + File.separator + LIST_FORMS_KEY

    private val hostDetailFormTemplatesPath = hostFormTemplatesPath + File.separator + DETAIL_FORMS_KEY

    private val hostLoginFormTemplatesPath = hostFormTemplatesPath + File.separator + LOGIN_FORMS_KEY

    private val hostFormattersPath = hostDb + File.separator + HOST_FORMATTERS_KEY

    private val hostInputControlsPath = hostDb + File.separator + HOST_INPUT_CONTROLS_KEY

    private val hostLoginFormPath = hostDb + File.separator + HOST_LOGIN_FORM_KEY

    private val srcPath = targetDirPath + File.separator +
            APP_PATH_KEY + File.separator +
            SRC_PATH_KEY

    fun resPath(): String {
        val resPath = srcPath + File.separator +
                MAIN_PATH_KEY + File.separator +
                RES_PATH_KEY
        return resPath.replaceIfWindowsPath()
    }

    fun navigationPath(): String {
        val navPath = resPath() + File.separator + NAVIGATION_PATH_KEY
        return navPath.replaceIfWindowsPath()
    }

    fun formPath(formType: String): String {
        val listPath = getTargetPath("main") + File.separator + formType
        return listPath.replaceIfWindowsPath()
    }

    private val layoutPath = resPath() + File.separator + LAYOUT_PATH_KEY

    fun assetsPath(): String {
        val assetsPath = srcPath + File.separator +
                MAIN_PATH_KEY + File.separator +
                ASSETS_PATH_KEY
        return assetsPath.replaceIfWindowsPath()
    }

    fun getRecyclerViewItemPath(tableName: String) =
        layoutPath + File.separator + RECYCLER_VIEW_ITEM_PREFIX + tableName.toLowerCase().addXmlSuffix()

    fun getDetailFormPath(tableName: String) =
        layoutPath + File.separator + DETAIL_FORM_PREFIX + tableName.toLowerCase().addXmlSuffix()

    fun getTargetPath(dir: String): String {
        val path = srcPath + File.separator +
                dir + File.separator +
                JAVA_PATH_KEY + File.separator +
                PACKAGE_PH
        return replaceDirectoriesPath(path)
    }

    private fun isWindowsOS(): Boolean = System.getProperty("os.name").contains("Windows")

    private fun String.replaceIfWindowsPath(): String {
        return if (isWindowsOS())
            this.replace("\\", "/")
        else
            this
    }


    fun getFormPath(formName: String?, formType: FormType): String {
        return if (formName.isNullOrEmpty()) {
            if (formType == FormType.LIST) getDefaultTemplateListFormPath() else getDefaultTemplateDetailFormPath()
        } else {
            if (formType == FormType.LIST) getTemplateListFormPath(formName) else getTemplateDetailFormPath(formName)
        }
    }

    fun verifyFormPath(formPath: String, formType: FormType): String {
        if (File(formPath).exists()) {
            if (!appFolderExistsInTemplate(formPath)) {
                return if (formType == FormType.LIST) {
                    Log.w("WARNING : INCOMPATIBLE TEMPLATE WAS GIVEN FOR THE LIST FORM $formPath")
                    getDefaultTemplateListFormPath()
                } else {
                    Log.w("WARNING : INCOMPATIBLE TEMPLATE WAS GIVEN FOR THE DETAIL FORM $formPath")
                    getDefaultTemplateDetailFormPath()
                }
            }
        } else {
            return if (formType == FormType.LIST) {
                Log.w("WARNING : MISSING LIST FORM TEMPLATE $formPath")
                getDefaultTemplateListFormPath()
            } else {
                Log.w("WARNING : MISSING DETAIL FORM TEMPLATE $formPath")
                getDefaultTemplateDetailFormPath()
            }
        }
        return formPath
    }

    private fun appFolderExistsInTemplate(formPath: String): Boolean = File(getAppFolderInTemplate(formPath)).exists()

    fun getAppFolderInTemplate(formPath: String): String {
        val androidFormPath = formPath + File.separator + ANDROID_PATH_KEY
        if (File(androidFormPath).exists()) {
            return androidFormPath + File.separator + APP_PATH_KEY
        }
        return formPath + File.separator + APP_PATH_KEY
    }

    private fun getDefaultTemplateListFormPath() = listFormTemplatesPath + File.separator + DEFAULT_LIST_FORM
    private fun getDefaultTemplateDetailFormPath() = detailFormTemplatesPath + File.separator + DEFAULT_DETAIL_FORM

    fun isDefaultTemplateListFormPath(formPath: String) = formPath == getDefaultTemplateListFormPath()

    private fun getTemplateListFormPath(formName: String): String {
        var templatePath = ""
        var newFormName = formName
        if (formName.startsWith("/")) {
            templatePath = hostListFormTemplatesPath

            if (formName.endsWith(".zip")) {
                val zipFile = File(templatePath + File.separator + formName.removePrefix("/"))
                if (zipFile.exists()) {
                    val tmpDir = ZipManager.unzip(zipFile)
                    tmpUnzippedTemplateListToBeDeleted.add(tmpDir)
                    newFormName = TEMPORARY_UNZIPPED_TEMPLATE_PREFIX + formName.removePrefix("/").removeSuffix(".zip")
                } else {
                    return getDefaultTemplateListFormPath()
                }
            }

        } else {
            templatePath = listFormTemplatesPath
        }
        return templatePath + File.separator + newFormName.removePrefix(File.separator)
    }

    private fun getTemplateDetailFormPath(formName: String): String {
        var templatePath = ""
        var newFormName = formName
        if (formName.startsWith("/")) {
            templatePath = hostDetailFormTemplatesPath

            if (formName.endsWith(".zip")) {
                val zipFile = File(templatePath + File.separator + formName.removePrefix("/"))
                if (zipFile.exists()) {
                    val tmpDir = ZipManager.unzip(zipFile)
                    tmpUnzippedTemplateListToBeDeleted.add(tmpDir)
                    newFormName = TEMPORARY_UNZIPPED_TEMPLATE_PREFIX + formName.removePrefix("/").removeSuffix(".zip")
                } else {
                    return getDefaultTemplateDetailFormPath()
                }
            }

        } else {
            templatePath = detailFormTemplatesPath
        }
        return templatePath + File.separator + newFormName.removePrefix(File.separator)
    }

    fun getCustomFormatterPath(name: String): String {
        Log.d("getCustomFormatterPath:name: $name")
        if (name.startsWith("/")) {
            var formatterPath = ""
            formatterPath = hostFormattersPath
            var newFormatterName = name
            if (name.endsWith(".zip")) {
                val zipFile = File(formatterPath + File.separator + name.removePrefix("/"))
                if (zipFile.exists()) {
                    val tmpDir = ZipManager.unzip(zipFile)
                    tmpUnzippedTemplateListToBeDeleted.add(tmpDir)
                    newFormatterName = TEMPORARY_UNZIPPED_TEMPLATE_PREFIX + name.removePrefix("/").removeSuffix(".zip")
                } else {
                    throw IllegalArgumentException("Zip file '$name' could not be found")
                }
            }
            Log.d("getCustomFormatterPath:return: ${hostFormattersPath + File.separator + newFormatterName.removePrefix(File.separator)}")
            return hostFormattersPath + File.separator + newFormatterName.removePrefix(File.separator)
        }
        throw IllegalArgumentException("Getting path of formatter $name that is not a host one ie. starting with '/'")
    }

    // Need to browse all input controls and check their manifest name value
    fun getInputControlPath(name: String): String {
        Log.d("getInputControlPath: name = $name")
        if (name.startsWith("/")) {
            findAppropriateFolder(hostInputControlsPath, name)?.let { inputControlFolder ->
                return hostInputControlsPath + File.separator + inputControlFolder.name
            }
            // check for zip
            findAppropriateZip(hostInputControlsPath, name)?.let { unzippedArchive ->
                return unzippedArchive.absolutePath.replaceIfWindowsPath()
            }
        }
        throw IllegalArgumentException("Getting path of input control $name that is not a host one ie. starting with '/'")
    }

    fun getTemplateLoginFormPath(formName: String): String? {
        var templatePath = ""
        var newFormName = formName
        if (formName.startsWith("/")) {
            Log.d("formName $formName")
            templatePath = hostLoginFormTemplatesPath
            Log.d("templatePath $templatePath")

            if (formName.endsWith(".zip")) {
                val zipFile = File(templatePath + File.separator + formName.removePrefix("/"))
                if (zipFile.exists()) {
                    val tmpDir = ZipManager.unzip(zipFile)
                    tmpUnzippedTemplateListToBeDeleted.add(tmpDir)
                    newFormName = TEMPORARY_UNZIPPED_TEMPLATE_PREFIX + formName.removePrefix("/").removeSuffix(".zip")
                    Log.d("newFormName $newFormName")
                    return templatePath + File.separator + newFormName.removePrefix(File.separator)
                }
            } else {
                Log.d("newFormName $newFormName")
                return templatePath + File.separator + newFormName.removePrefix(File.separator)
            }
        }
        Log.d("getTemplateLoginFormPath returns null")
        return null
    }

    private fun findAppropriateFolder(basePath: String, nameInManifest: String): File? {
        Log.d("findAppropriateFolder, basePath : $basePath, nameInManifest: $nameInManifest")
        File(basePath).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }
            .forEach { currentFolder ->
                getManifestJSONContent(currentFolder.absolutePath.replaceIfWindowsPath())?.let { jsonContent ->
                    if (jsonContent.getSafeString("name") == nameInManifest.removePrefix("/")) {
                        return currentFolder
                    }
                }
            }
        return null
    }

    private fun findAppropriateZip(basePath: String, nameInManifest: String): File? {
        Log.d("findAppropriateZip, basePath : $basePath, nameInManifest: $nameInManifest")
        File(basePath).walkTopDown().filter { file -> !file.isHidden && file.isFile && file.extension == "zip" }
            .forEach { zipFile ->
                Log.d("zipFile: $zipFile")
                val tmpDir = ZipManager.unzip(zipFile)
                tmpUnzippedTemplateListToBeDeleted.add(tmpDir)
                tmpDir.walkTopDown().firstOrNull { zipContent -> zipContent.name == "manifest.json" }?.let { manifestFile ->
                    getManifestJSONContent(manifestFile)?.let { jsonContent ->
                        if (jsonContent.getSafeString("name") == nameInManifest.removePrefix("/")) {
                            return tmpDir
                        }
                    }
                }
            }
        return null
    }

    fun findMatchingKotlinClass(basePath: String, annotation: String): String? {
        Log.d("findMatchingInputControlClass, basePath : $basePath")
        File(basePath).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }
            .forEach { currentFolder ->
                currentFolder.walkTopDown()
                    .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) && file.name != DS_STORE }
                    .forEach { currentFile ->
                        val fileContent: String = currentFile.readFile()
                        if (fileContent.contains(annotation)) {
                            return currentFile.name.substringBefore(".")
                        }
                    }
            }
        return null
    }

    fun deleteTemporaryUnzippedDirectories() {
        tmpUnzippedTemplateListToBeDeleted.forEach { fileToBeDeleted ->
            Log.d("Dir to be deleted : ${fileToBeDeleted.absolutePath}")
            if (fileToBeDeleted.deleteRecursively()) {
                Log.d("Temporary unzipped template directory successfully deleted.")
            } else {
                Log.w("Could not delete temporary unzipped template directory.")
            }
        }
    }

    fun isValidFormatter(format: String): Boolean {
        if (!format.startsWith("/")) return false
        val formatPath = getCustomFormatterPath(format)
        getManifestJSONContent(formatPath)?.let {
            val fieldMapping = getFieldMappingFormatter(it, format)
            return fieldMapping.isValidFormatter()
        }
        return false
    }

    fun isValidKotlinCustomFormatter(format: String): Boolean {
        if (!format.startsWith("/")) return false
        val formatPath = getCustomFormatterPath(format)
        if (!formattersFolderExistsInFormatter(formatPath)) return false
        getManifestJSONContent(formatPath)?.let {
            val fieldMapping = getFieldMappingFormatter(it, format)
            return fieldMapping.isValidKotlinCustomDataFormatter()
        }
        return false
    }

    fun getKotlinCustomFormatterBinding(format: String): String {
        val formatPath = getCustomFormatterPath(format)
        getManifestJSONContent(formatPath)?.let {
            val fieldMapping = getFieldMappingFormatter(it, format)
            return fieldMapping.binding ?: ""
        }
        return ""
    }

    private fun formattersFolderExistsInFormatter(path: String): Boolean =
        File(path + File.separator + ANDROID_PATH_KEY + File.separator + FORMATTERS_FORMATTER_KEY).exists()
}