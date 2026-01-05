import java.io.File

class FeatureChecker(projectEditor: ProjectEditor) {

    private val hasDataSet = projectEditor.findJsonBoolean(HAS_DATASET_KEY) ?: true
    private val debugMode = projectEditor.findJsonBoolean("debugMode") ?: false

    private val filesPathToSkipIfDataSet = listOf<String>(
        "buildscripts" + File.separator + "prepopulation.gradle",
        "buildSrc" + File.separator + "build.gradle",
        "__PKG_JOINED__.android.build",
    )

    private val filesPathToSkipIfNotDebug = listOf<String>(
        "git-hash.gradle", "jacoco.gradle", "maven-check.gradle"
    )

    fun checkFeaturesAndProcess(currentFile: File, processCallback: () -> Unit) {
        if (dataSetCheck(currentFile) && debugCheck(currentFile))
            processCallback()
    }

    private fun dataSetCheck(currentFile: File) = !hasDataSet || !shouldSkipIfDataSet(currentFile)

    private fun debugCheck(currentFile: File) = debugMode || !shouldSkipIfNotDebug(currentFile)

    private fun shouldSkipIfDataSet(currentFile: File): Boolean {
        filesPathToSkipIfDataSet.forEach {
            if (currentFile.path.contains(it)) {
                Log.d("Skipping because DataSet : $currentFile")
                return true
            }
        }
        return false
    }

    private fun shouldSkipIfNotDebug(currentFile: File): Boolean {
        filesPathToSkipIfNotDebug.forEach {
            if (currentFile.path.contains(it)) {
                Log.d("Skipping because not debug : $currentFile")
                return true
            }
        }
        return false
    }
}
