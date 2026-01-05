import org.json.JSONObject
import java.io.File

fun integrateGlobalStamp(dumpInfoFile: File, initialGlobalStamp: Int) {
    Log.i("Updating initialGlobalStamp value in dump_info.json file with value : $initialGlobalStamp")
    val jsonObj: JSONObject = retrieveJSONObject(dumpInfoFile.readFile()) ?: JSONObject()
    jsonObj.put("dumped_stamp", initialGlobalStamp)
    dumpInfoFile.writeText(jsonObj.toString(2))
}

fun integrateDumpedTables(dumpInfoFile: File, tableNames: List<String>) {
    Log.i("Updating dumpedTables value in dump_info.json file with value : $tableNames")
    val jsonObj: JSONObject = retrieveJSONObject(dumpInfoFile.readFile()) ?: JSONObject()
    jsonObj.put("dumped_tables", tableNames)
    dumpInfoFile.writeText(jsonObj.toString(2))
}