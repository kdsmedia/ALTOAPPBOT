//DEPS com.samskivert:jmustache:1.15
//DEPS com.squareup.retrofit2:converter-gson:2.9.0

@file:DependsOnMaven("com.github.ajalt.clikt:clikt-jvm:3.2.0")
@file:DependsOnMaven("org.codehaus.groovy:groovy-sql:3.0.9")
@file:DependsOnMaven("org.xerial:sqlite-jdbc:3.36.0.3")
@file:DependsOnMaven("org.json:json:20210307")
@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")

//@file:DependsOnMaven("com.google.android:android:4.1.1.4")
//@file:DependsOnMaven("commons-io:commons-io:2.8.0")
//@file:DependsOnMaven("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31")
//@file:DependsOnMaven("org.graalvm.compiler:compiler:21.3.0")

@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
//@file:CompilerOpts("-jvm-target 1.8")

@file:Import("Includes.kt")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import java.io.File

class GenerateCommand : CliktCommand(name = "generate") {

    private lateinit var FILES_TO_COPY: String
    private lateinit var TEMPLATE_FILES: String
    private lateinit var TEMPLATE_FORMS: String
    private lateinit var HOST_DB: String
    private lateinit var projectEditorJson: File
    private lateinit var catalogJson: File

    val projectEditor: String by option(help = projectEditorText).prompt(projectEditorText).validate {
        projectEditorJson = File(it)
        require(projectEditorJson.exists()) { "Can't find project editor json file ${projectEditorJson.name}" }
    }
    val filesToCopy: String by option(help = filesToCopyText).prompt(filesToCopyText).validate {
        val filesToCopyDir = File(it)
        require(filesToCopyDir.exists() && filesToCopyDir.isDirectory) { "Can't find files to copy directory $it" }
        FILES_TO_COPY = filesToCopy.removeSuffix("/")
    }
    val templateFiles: String by option(help = templateFilesText).prompt(templateFilesText).validate {
        val templateFilesDir = File(it)
        require(templateFilesDir.exists() && templateFilesDir.isDirectory) { "Can't find template files directory $it" }
        TEMPLATE_FILES = templateFiles.removeSuffix("/")
    }
    val templateForms: String by option(help = templateFormsText).prompt(templateFormsText).validate {
        val templateFormsDir = File(it)
        require(templateFormsDir.exists() && templateFormsDir.isDirectory) { "Can't find template forms directory $it" }
        TEMPLATE_FORMS = templateForms.removeSuffix("/")
    }
    val hostDb: String by option(help = hostDbText).prompt(hostDbText).validate {
        HOST_DB = hostDb.removeSuffix("/")
    }
    val catalog: String by option(help = catalogText).prompt(catalogText).validate {
        catalogJson = File(it)
        require(catalogJson.exists()) { "Can't find catalog json file ${catalogJson.name}" }
    }

    override fun run() {
        Log.i("Parameters checked.")
        Log.i("Version: ${Version.VALUE}")
        Log.i("Starting procedure...")
        start()
        Log.i("Procedure complete.")
    }

    private fun start() {

        Log.i("file: ${projectEditorJson.path}...")

        val catalogDef = CatalogDef(catalogJson)

        Log.i("Reading catalog json file done.")
        Log.i("--------------------------------------")

        val projectEditor = ProjectEditor(projectEditorFile = projectEditorJson, catalogDef = catalogDef)

        Log.i("Reading project editor json file done.")
        Log.i("--------------------------------------")

        var targetDirPath = ""
        projectEditor.findJsonString("targetDirPath")?.let {
            targetDirPath = it
        } ?: run {
            val targetDirPathFromEnv = System.getenv("TARGET_PATH")
            if (!targetDirPathFromEnv.isNullOrEmpty())
                targetDirPath = targetDirPathFromEnv
        }
        if (targetDirPath.isEmpty()) {
            throw Exception("No target directory. Define env var `TARGET_PATH` or pass it in project JSON editor ")
        }

        val pathHelper = PathHelper(
                targetDirPath = targetDirPath.removeSuffix("/"),
                templateFilesPath = TEMPLATE_FILES,
                templateFormsPath = TEMPLATE_FORMS,
                hostDb = HOST_DB,
                filesToCopy = FILES_TO_COPY,
                companyWithCaps = projectEditor.findJsonString("companyWithCaps") ?: DEFAULT_COMPANY,
                appNameWithCaps = projectEditor.findJsonString("appNameWithCaps") ?: DEFAULT_APPLICATION,
                pkg = projectEditor.findJsonString("package") ?: DEFAULT_PACKAGE
        )

        val fileHelper = FileHelper(pathHelper)

        Log.i("Start gathering Mustache templating data...")

        val mustacheHelper = MustacheHelper(fileHelper, projectEditor)

        Log.i("Gathering Mustache templating data done.")
        Log.i("----------------------------------------")

        fileHelper.copyFiles()

        Log.i("Files successfully copied.")

        fileHelper.createPathDirectories()

        Log.i("Start applying Mustache templating...")

        mustacheHelper.applyListFormTemplate()

        Log.i("Applied List Form Templates")

        mustacheHelper.applyDetailFormTemplate()

        Log.i("Applied Detail Form Templates")

        pathHelper.deleteTemporaryUnzippedDirectories()

        Log.i("Deleted Temporary Unzipped Directories")

        mustacheHelper.processTemplates()

        Log.i("Mustache templating done.")

        mustacheHelper.copyFilesAfterGlobalTemplating()

        Log.i("Copied remaining files after templating")

        Log.i("-------------------------")

        mustacheHelper.makeTableInfo()

        Log.i("\"$TABLE_INFO_FILENAME\" file successfully generated.")

        mustacheHelper.makeAppInfo()

        Log.i("\"$APP_INFO_FILENAME\" file successfully generated.")

        mustacheHelper.makeCustomFormatters()

        Log.i("\"$CUSTOM_FORMATTERS_FILENAME\" file successfully generated.")

        mustacheHelper.makeInputControls()

        Log.i("\"$INPUT_CONTROLS_FILENAME\" file successfully generated.")

        mustacheHelper.makeActions()

        Log.i("\"$ACTIONS_FILENAME\" file successfully generated.")

        Log.i("Output: ${projectEditor.findJsonString("targetDirPath")}")

        Log.i("data:")
        Log.logData(mustacheHelper.data)
    }
}

class CreateDatabaseCommand : CliktCommand(name = "createDatabase") {

    private lateinit var ASSETS: String
    private lateinit var DBFILEPATH: String
    private lateinit var projectEditorJson: File
    private lateinit var catalogJson: File

    val projectEditor: String by option(help = projectEditorText).prompt(projectEditorText).validate {
        projectEditorJson = File(it)
        require(projectEditorJson.exists()) { "Can't find project editor json file ${projectEditorJson.name}" }
    }
    val assets: String by option(help = assetsText).prompt(assetsText).validate {
        val assetsDir = File(it)
        require(assetsDir.exists() && assetsDir.isDirectory) { "Can't find assets directory $it" }
        ASSETS = assets.removeSuffix("/")
    }
    val dbFile: String by option(help = dbText).prompt(dbText).validate {
        val dbParentDir = File(it).parentFile
        dbParentDir.mkdirs()
        require(dbParentDir.exists() && dbParentDir.isDirectory) { "Can't find db's parent directory directory $dbParentDir" }
        DBFILEPATH = dbFile.removeSuffix("/")
    }
    val catalog: String by option(help = catalogText).prompt(catalogText).validate {
        catalogJson = File(it)
        require(catalogJson.exists()) { "Can't find catalog json file ${catalogJson.name}" }
    }

    override fun run() {
        Log.i("Parameters checked.")
        Log.i("Version: ${Version.VALUE}")
        Log.i("Starting procedure...")
        start()
        Log.i("Procedure complete.")
    }

    private fun start() {

        Log.i("file: ${projectEditorJson.path}...")

        val catalogDef = CatalogDef(catalogJson)

        Log.i("Reading catalog json file done.")
        Log.i("--------------------------------------")

        val projectEditor = ProjectEditor(projectEditorFile = projectEditorJson, catalogDef = catalogDef, isCreateDatabaseCommand = true)

        Log.i("Reading project editor json file done.")
        Log.i("--------------------------------------")

        CreateDatabaseTask(projectEditor.dataModelList, ASSETS, DBFILEPATH)

        Log.i("Output: $DBFILEPATH}")
    }
}

class Main : CliktCommand() {
    override fun run() {}
}

fun main(args: Array<String>) = Main().subcommands(VersionCommand(), GenerateCommand(), CreateDatabaseCommand()).main(args)
